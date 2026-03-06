package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.vo.ProductCreateVO;
import com.openiot.device.vo.ProductUpdateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 产品服务
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    private final DeviceMapper deviceMapper;
    private final SecureRandom random = new SecureRandom();

    /**
     * 创建产品
     *
     * @param vo 产品创建 VO
     * @return 产品实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(ProductCreateVO vo) {
        // 获取租户ID
        Long tenantId = getTenantId();

        // 生成产品密钥（租户内唯一）
        String productKey = generateProductKey();

        // 创建产品实体
        Product product = new Product();
        BeanUtils.copyProperties(vo, product);
        product.setTenantId(tenantId);
        product.setProductKey(productKey);

        // 设置默认值
        if (product.getStatus() == null) {
            product.setStatus("1");
        }
        if (product.getNodeType() == null) {
            product.setNodeType("DIRECT");
        }
        if (product.getDataFormat() == null) {
            product.setDataFormat("JSON");
        }

        // 保存产品
        this.save(product);

        log.info("创建产品成功: {} (tenant={}, key={})",
            product.getProductName(), tenantId, productKey);

        return product;
    }

    /**
     * 更新产品
     *
     * @param id 产品ID
     * @param vo 产品更新 VO
     * @return 产品实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Long id, ProductUpdateVO vo) {
        Product product = this.getById(id);
        if (product == null) {
            throw BusinessException.notFound("产品不存在: " + id);
        }

        // 检查租户权限
        checkTenantAccess(product);

        // 更新字段
        if (vo.getProductName() != null) {
            product.setProductName(vo.getProductName());
        }
        if (vo.getProductType() != null) {
            product.setProductType(vo.getProductType());
        }
        if (vo.getProtocolType() != null) {
            product.setProtocolType(vo.getProtocolType());
        }
        if (vo.getNodeType() != null) {
            product.setNodeType(vo.getNodeType());
        }
        if (vo.getDataFormat() != null) {
            product.setDataFormat(vo.getDataFormat());
        }
        if (vo.getDescription() != null) {
            product.setDescription(vo.getDescription());
        }
        if (vo.getStatus() != null) {
            product.setStatus(vo.getStatus());
        }

        this.updateById(product);

        log.info("更新产品成功: {} (id={})", product.getProductName(), id);

        return product;
    }

    /**
     * 删除产品（检查是否有关联设备）
     *
     * @param id 产品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = this.getById(id);
        if (product == null) {
            throw BusinessException.notFound("产品不存在: " + id);
        }

        // 检查租户权限
        checkTenantAccess(product);

        // 检查是否有关联设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, id);
        long deviceCount = deviceMapper.selectCount(wrapper);

        if (deviceCount > 0) {
            throw BusinessException.badRequest(
                String.format("无法删除产品，存在 %d 个关联设备", deviceCount));
        }

        // 软删除
        this.removeById(id);

        log.info("删除产品成功: {} (id={})", product.getProductName(), id);
    }

    /**
     * 根据ID查询产品
     *
     * @param id 产品ID
     * @return 产品实体
     */
    public Product getProductById(Long id) {
        Product product = this.getById(id);
        if (product == null) {
            throw BusinessException.notFound("产品不存在: " + id);
        }

        // 检查租户权限
        checkTenantAccess(product);

        return product;
    }

    /**
     * 分页查询产品
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param productName 产品名称（模糊查询）
     * @param protocolType 协议类型
     * @param status 状态
     * @return 分页结果
     */
    public Page<Product> getProductList(int pageNum, int pageSize,
                                        String productName, String protocolType, String status) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        // 租户过滤
        Long tenantId = getTenantId();
        wrapper.eq(Product::getTenantId, tenantId);

        // 条件过滤
        if (productName != null && !productName.isEmpty()) {
            wrapper.like(Product::getProductName, productName);
        }
        if (protocolType != null && !protocolType.isEmpty()) {
            wrapper.eq(Product::getProtocolType, protocolType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Product::getStatus, status);
        }

        wrapper.orderByDesc(Product::getCreateTime);

        return this.page(page, wrapper);
    }

    /**
     * 更新产品物模型
     *
     * @param id 产品ID
     * @param thingModel 物模型定义
     * @return 产品实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Product updateThingModel(Long id, JsonNode thingModel) {
        Product product = this.getById(id);
        if (product == null) {
            throw BusinessException.notFound("产品不存在: " + id);
        }

        // 检查租户权限
        checkTenantAccess(product);

        product.setThingModel(thingModel);
        this.updateById(product);

        log.info("更新产品物模型成功: {} (id={})", product.getProductName(), id);

        return product;
    }

    /**
     * 获取产品的关联设备数量
     *
     * @param productId 产品ID
     * @return 设备数量
     */
    public int getDeviceCount(Long productId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, productId);
        return (int) deviceMapper.selectCount(wrapper);
    }

    /**
     * 生成产品密钥（租户内唯一，10-20字符）
     * 格式：PROD_XXXXXX
     */
    private String generateProductKey() {
        Long tenantId = getTenantId();
        String productKey;

        // 最多尝试 10 次生成唯一的产品密钥
        for (int i = 0; i < 10; i++) {
            // 生成随机部分（6位大写字母和数字）
            String randomPart = generateRandomString(6);
            productKey = "PROD_" + randomPart;

            // 检查是否唯一
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getTenantId, tenantId)
                   .eq(Product::getProductKey, productKey);

            if (this.count(wrapper) == 0) {
                return productKey;
            }
        }

        throw BusinessException.internalError("生成产品密钥失败，请稍后重试");
    }

    /**
     * 生成随机字符串（大写字母和数字）
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
    private void checkTenantAccess(Product product) {
        Long currentTenantId = getTenantId();
        if (!currentTenantId.equals(product.getTenantId())) {
            log.warn("跨租户访问被拒绝: current={}, target={}",
                currentTenantId, product.getTenantId());
            throw BusinessException.forbidden("无权访问该产品");
        }
    }
}
