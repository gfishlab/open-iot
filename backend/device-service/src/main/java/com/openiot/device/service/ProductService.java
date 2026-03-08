package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.redis.util.RedisUtil;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.vo.ProductCreateVO;
import com.openiot.device.vo.ProductUpdateVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 产品服务
 *
 * <p>提供产品的 CRUD 操作，支持 Redis 缓存。
 *
 * <h3>缓存策略：</h3>
 * <ul>
 *   <li>缓存 Key 格式：product:info:{productId}</li>
 *   <li>TTL: 30 分钟</li>
 *   <li>更新/删除时自动清除缓存</li>
 * </ul>
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    private final DeviceMapper deviceMapper;
    private final ThingModelService thingModelService;
    private final RedisUtil redisUtil;
    private final SecureRandom random = new SecureRandom();

    // ==================== 缓存配置 ====================

    /**
     * 产品信息缓存 Key 前缀
     */
    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:info:";

    /**
     * 缓存过期时间（秒）- 30 分钟
     */
    private static final long CACHE_TTL_SECONDS = 30 * 60;

    /**
     * 缓存产品信息
     */
    private void cacheProduct(Product product) {
        try {
            String cacheKey = getCacheKey(product.getId());
            redisUtil.set(cacheKey, product, CACHE_TTL_SECONDS);
            log.debug("缓存产品信息: id={}", product.getId());
        } catch (Exception e) {
            log.warn("缓存产品信息失败: {}", e.getMessage());
        }
    }

    /**
     * 清除产品缓存
     */
    private void evictProductCache(Long productId) {
        try {
            String cacheKey = getCacheKey(productId);
            redisUtil.delete(cacheKey);
            log.debug("清除产品缓存: id={}", productId);
        } catch (Exception e) {
            log.warn("清除产品缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存 Key
     */
    private String getCacheKey(Long productId) {
        return PRODUCT_CACHE_KEY_PREFIX + productId;
    }

    // ==================== CRUD 操作 ====================

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

        // 缓存产品信息
        cacheProduct(product);

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

        // 更新缓存
        cacheProduct(product);

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

        // 清除缓存
        evictProductCache(id);

        log.info("删除产品成功: {} (id={})", product.getProductName(), id);
    }

    /**
     * 根据ID查询产品（带缓存）
     *
     * @param id 产品ID
     * @return 产品实体
     */
    public Product getProductById(Long id) {
        // 尝试从缓存获取
        String cacheKey = getCacheKey(id);
        Product cachedProduct = redisUtil.get(cacheKey, Product.class);

        if (cachedProduct != null) {
            log.debug("从缓存获取产品: id={}", id);
            return cachedProduct;
        }

        // 从数据库查询
        Product product = this.getById(id);
        if (product == null) {
            throw BusinessException.notFound("产品不存在: " + id);
        }

        // 检查租户权限
        checkTenantAccess(product);

        // 缓存产品信息
        cacheProduct(product);

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

        // 验证物模型定义
        thingModelService.validateThingModel(thingModel);

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
        Long count = deviceMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
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

    // ==================== 缓存操作 ====================

    /**
     * 查询产品详情（包含统计信息）
     *
     * @param id 产品ID
     * @return 产品详情 VO
     */
    public ProductDetailVO getProductDetail(Long id) {
        Product product = getProductById(id);

        // 查询关联设备数量
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, id);
        long deviceCount = deviceMapper.selectCount(wrapper);

        ProductDetailVO detail = new ProductDetailVO();
        detail.setProduct(product);
        detail.setDeviceCount(deviceCount);

        return detail;
    }

    /**
     * 查询产品的设备列表
     *
     * @param productId 产品ID
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 设备分页列表
     */
    public Page<Device> getProductDevices(Long productId, int pageNum, int pageSize) {
        Product product = this.getById(productId);
        if (product == null) {
            throw BusinessException.notFound("产品不存在");
        }

        // 检查租户权限
        checkTenantAccess(product);

        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, productId)
               .orderByDesc(Device::getCreateTime);

        return deviceMapper.selectPage(page, wrapper);
    }

    /**
     * 启用/禁用产品
     *
     * @param id     产品ID
     * @param status 状态：1-启用，0-禁用
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        Product product = getProductById(id);
        product.setStatus(status);
        this.updateById(product);

        log.info("更新产品状态: {} (id={}, status={})", product.getProductName(), id, status);
    }

    /**
     * 查询产品统计信息
     *
     * @param productId 产品ID
     * @return 统计信息
     */
    public ProductStatisticsVO getStatistics(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw BusinessException.notFound("产品不存在");
        }

        // 设备统计
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, productId);
        long totalDevices = deviceMapper.selectCount(wrapper);

        wrapper.eq(Device::getStatus, "1");
        long onlineDevices = deviceMapper.selectCount(wrapper);

        ProductStatisticsVO statistics = new ProductStatisticsVO();
        statistics.setProductId(productId);
        statistics.setProductName(product.getProductName());
        statistics.setTotalDevices(totalDevices);
        statistics.setOnlineDevices(onlineDevices);
        statistics.setOfflineDevices(totalDevices - onlineDevices);

        return statistics;
    }
}
