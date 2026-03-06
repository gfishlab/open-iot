package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.vo.DeviceCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 设备服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService extends ServiceImpl<DeviceMapper, Device> {

    private final ProductService productService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 创建设备
     */
    public Device createDevice(Device device) {
        // 设置租户ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            device.setTenantId(Long.valueOf(tenantId));
        }

        // 检查设备编码是否已存在
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getTenantId, device.getTenantId())
               .eq(Device::getDeviceCode, device.getDeviceCode());
        if (this.count(wrapper) > 0) {
            throw BusinessException.badRequest("设备编码已存在: " + device.getDeviceCode());
        }

        // 生成设备 Token
        device.setDeviceToken(generateToken());

        // 设置默认值
        if (device.getStatus() == null) {
            device.setStatus("1");
        }

        this.save(device);
        log.info("创建设备成功: {} (tenant={})", device.getDeviceCode(), device.getTenantId());
        return device;
    }

    /**
     * 创建设备（带产品关联）
     *
     * @param vo 设备创建 VO
     * @return 设备实体
     */
    public Device createDeviceWithProduct(DeviceCreateVO vo) {
        // 获取租户ID
        Long tenantId = getTenantId();

        // 检查设备编码是否已存在
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getTenantId, tenantId)
               .eq(Device::getDeviceCode, vo.getDeviceCode());
        if (this.count(wrapper) > 0) {
            throw BusinessException.badRequest("设备编码已存在: " + vo.getDeviceCode());
        }

        // 创建设备实体
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setDeviceCode(vo.getDeviceCode());
        device.setDeviceName(vo.getDeviceName());
        device.setProtocolType(vo.getProtocolType());

        // 关联产品
        if (vo.getProductId() != null) {
            Product product = productService.getProductById(vo.getProductId());

            // 检查产品租户是否匹配
            if (!product.getTenantId().equals(tenantId)) {
                throw BusinessException.forbidden("无权使用该产品");
            }

            device.setProductId(product.getId());

            // 从产品继承协议类型（如果设备未指定）
            if (device.getProtocolType() == null || device.getProtocolType().isEmpty()) {
                device.setProtocolType(product.getProtocolType());
            }

            log.info("设备关联产品: device={}, product={}",
                device.getDeviceCode(), product.getProductKey());
        }

        // 生成设备密钥对（DeviceKey + DeviceSecret）
        String deviceKey = generateDeviceKey();
        String deviceSecret = generateDeviceSecret();

        device.setDeviceKey(deviceKey);
        device.setDeviceSecret(passwordEncoder.encode(deviceSecret)); // BCrypt 哈希存储

        // 生成设备 Token
        device.setDeviceToken(generateToken());

        // 设置默认值
        if (device.getStatus() == null) {
            device.setStatus("1");
        }

        // 保存设备
        this.save(device);

        log.info("创建设备成功: {} (tenant={}, key={})",
            device.getDeviceCode(), tenantId, deviceKey);

        // 注意：返回的 device 对象中 deviceSecret 是哈希值，原始密钥需要单独返回给用户
        // 在实际使用中，应该创建一个 DeviceVO 来返回，包含明文的 deviceSecret（仅创建时返回一次）

        return device;
    }

    /**
     * 设备认证（验证 DeviceKey + DeviceSecret）
     *
     * @param deviceKey 设备密钥
     * @param deviceSecret 设备密钥（明文）
     * @return 设备实体
     */
    public Device authenticateDevice(String deviceKey, String deviceSecret) {
        // 根据 DeviceKey 查询设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceKey, deviceKey);
        Device device = this.getOne(wrapper);

        if (device == null) {
            log.warn("设备认证失败: DeviceKey 不存在 ({})", deviceKey);
            throw BusinessException.unauthorized("设备认证失败");
        }

        // 验证 DeviceSecret（BCrypt）
        if (!passwordEncoder.matches(deviceSecret, device.getDeviceSecret())) {
            log.warn("设备认证失败: DeviceSecret 错误 (device={})", device.getDeviceCode());
            throw BusinessException.unauthorized("设备认证失败");
        }

        // 检查设备状态
        if ("0".equals(device.getStatus())) {
            log.warn("设备认证失败: 设备已禁用 (device={})", device.getDeviceCode());
            throw BusinessException.forbidden("设备已禁用");
        }

        log.info("设备认证成功: {}", device.getDeviceCode());
        return device;
    }

    /**
     * 根据设备编码查询
     */
    public Device getByCode(String deviceCode) {
        String tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceCode, deviceCode);
        if (tenantId != null) {
            wrapper.eq(Device::getTenantId, Long.valueOf(tenantId));
        }
        return this.getOne(wrapper);
    }

    /**
     * 分页查询设备
     */
    public Page<Device> page(int pageNum, int pageSize, String status, String protocolType) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();

        // 租户过滤
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(Device::getTenantId, Long.valueOf(tenantId));
        }

        // 条件过滤
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Device::getStatus, status);
        }
        if (protocolType != null && !protocolType.isEmpty()) {
            wrapper.eq(Device::getProtocolType, protocolType);
        }

        wrapper.orderByDesc(Device::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 重新生成设备 Token
     */
    public String regenerateToken(Long deviceId) {
        Device device = this.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        // 权限检查
        checkTenantAccess(device);

        String newToken = generateToken();
        device.setDeviceToken(newToken);
        this.updateById(device);
        log.info("重新生成设备Token: {}", device.getDeviceCode());
        return newToken;
    }

    /**
     * 生成设备 Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成设备密钥（DeviceKey，UUID v4）
     */
    private String generateDeviceKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成设备密钥（DeviceSecret，UUID v4 明文）
     * 注意：存储时需要 BCrypt 哈希
     */
    private String generateDeviceSecret() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取当前租户ID
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("未找到租户信息");
        }
        return Long.valueOf(tenantId);
    }

    /**
     * 检查租户访问权限
     */
    private void checkTenantAccess(Device device) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(String.valueOf(device.getTenantId()))) {
            log.warn("跨租户访问被拒绝: current={}, target={}", tenantId, device.getTenantId());
            throw BusinessException.forbidden("无权访问该设备");
        }
    }
}
