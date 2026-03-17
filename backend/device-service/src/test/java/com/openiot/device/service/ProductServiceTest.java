package com.openiot.device.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.redis.util.RedisUtil;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.vo.ProductCreateVO;
import com.openiot.device.vo.ProductUpdateVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 产品服务单元测试
 *
 * <p>测试 ProductService 的核心 CRUD 操作和缓存逻辑。
 * 使用 Mockito mock 外部依赖（Mapper、Redis、ThingModelService），
 * 通过 MockedStatic 模拟 TenantContext 静态方法。
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("产品服务测试")
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private ThingModelService thingModelService;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private ProductService productService;

    /** 模拟 TenantContext 静态方法 */
    private MockedStatic<TenantContext> tenantContextMock;

    /** 测试用产品实体 */
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 手动注入 baseMapper（Mockito 的 @InjectMocks 无法注入 ServiceImpl 的 baseMapper 字段）
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);

        // 初始化 MyBatis Plus 表信息缓存，避免 lambda/逻辑删除相关操作出错
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Product.class);

        // 初始化 TenantContext 静态 mock
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("1");
        tenantContextMock.when(TenantContext::isPlatformAdmin).thenReturn(false);

        // 初始化测试产品
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTenantId(1L);
        testProduct.setProductKey("PROD_ABC123");
        testProduct.setProductName("温湿度传感器");
        testProduct.setProductType("DEVICE");
        testProduct.setProtocolType("MQTT");
        testProduct.setNodeType("DIRECT");
        testProduct.setDataFormat("JSON");
        testProduct.setStatus("1");
    }

    @AfterEach
    void tearDown() {
        // 释放静态 mock，避免影响其他测试
        tenantContextMock.close();
    }

    // ==================== 创建产品 ====================

    @Test
    @DisplayName("创建产品成功")
    void createProduct_Success() {
        // Given: 构造创建请求 VO
        ProductCreateVO vo = new ProductCreateVO();
        vo.setProductName("温湿度传感器");
        vo.setProductType("DEVICE");
        vo.setProtocolType("MQTT");

        // Mock: ProductMapper.insert 模拟保存成功，count 模拟产品密钥不重复
        doReturn(1).when(productMapper).insert(any(Product.class));
        doReturn(0L).when(productMapper).selectCount(any(LambdaQueryWrapper.class));

        // When: 调用创建产品方法
        Product result = productService.createProduct(vo);

        // Then: 验证返回值和字段设置
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("温湿度传感器");
        assertThat(result.getProductType()).isEqualTo("DEVICE");
        assertThat(result.getProtocolType()).isEqualTo("MQTT");
        assertThat(result.getTenantId()).isEqualTo(1L);
        assertThat(result.getProductKey()).isNotNull().startsWith("PROD_");
        assertThat(result.getStatus()).isEqualTo("1");
        assertThat(result.getNodeType()).isEqualTo("DIRECT");
        assertThat(result.getDataFormat()).isEqualTo("JSON");

        // 验证缓存写入
        verify(redisUtil).set(anyString(), any(Product.class), anyLong());
    }

    // ==================== 查询产品列表 ====================

    @Test
    @DisplayName("查询产品列表")
    void getProductList_Success() {
        // Given: 准备分页结果
        Page<Product> expectedPage = new Page<>(1, 10);
        expectedPage.setTotal(1);
        expectedPage.setRecords(java.util.List.of(testProduct));

        // Mock: 分页查询返回预期结果
        doReturn(expectedPage).when(productMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        // When: 调用分页查询
        Page<Product> result = productService.getProductList(1, 10, null, null, null);

        // Then: 验证分页结果
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getProductName()).isEqualTo("温湿度传感器");

        verify(productMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== 更新产品信息 ====================

    @Test
    @DisplayName("更新产品信息")
    void updateProduct_Success() {
        // Given: 构造更新请求
        ProductUpdateVO vo = new ProductUpdateVO();
        vo.setProductName("高精度温湿度传感器");
        vo.setProtocolType("HTTP");

        // Mock: 根据 ID 查询返回已有产品，更新返回成功
        doReturn(testProduct).when(productMapper).selectById(1L);
        doReturn(1).when(productMapper).updateById(any(Product.class));

        // When: 调用更新方法
        Product result = productService.updateProduct(1L, vo);

        // Then: 验证字段被正确更新
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("高精度温湿度传感器");
        assertThat(result.getProtocolType()).isEqualTo("HTTP");

        // 验证缓存被刷新
        verify(redisUtil).set(anyString(), any(Product.class), anyLong());
    }

    @Test
    @DisplayName("更新产品 - 产品不存在抛出异常")
    void updateProduct_NotFound() {
        // Given
        ProductUpdateVO vo = new ProductUpdateVO();
        vo.setProductName("不存在的产品");

        // Mock: 查询返回 null
        doReturn(null).when(productMapper).selectById(999L);

        // When & Then: 应抛出 BusinessException
        assertThatThrownBy(() -> productService.updateProduct(999L, vo))
                .isInstanceOf(BusinessException.class);
    }

    // ==================== 删除产品（逻辑删除）====================

    @Test
    @DisplayName("删除产品（逻辑删除）")
    void deleteProduct_Success() {
        // Given: 产品存在且无关联设备
        doReturn(testProduct).when(productMapper).selectById(1L);
        doReturn(0L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(productMapper).deleteById(1L);

        // When: 调用删除方法
        productService.deleteProduct(1L);

        // Then: 验证执行了删除操作
        verify(productMapper).deleteById(1L);
        // 验证缓存被清除
        verify(redisUtil).delete(eq("product:info:1"));
    }

    @Test
    @DisplayName("删除产品 - 存在关联设备时拒绝删除")
    void deleteProduct_HasDevices() {
        // Given: 产品存在且有关联设备
        doReturn(testProduct).when(productMapper).selectById(1L);
        doReturn(3L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));

        // When & Then: 应抛出 BusinessException
        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关联设备");

        // 验证没有执行删除
        verify(productMapper, never()).deleteById(anyLong());
    }

    // ==================== 根据ID查询产品 ====================

    @Test
    @DisplayName("根据ID查询产品 - 缓存命中")
    void getProductById_CacheHit() {
        // Given: Redis 缓存中存在产品
        when(redisUtil.get(eq("product:info:1"), eq(Product.class))).thenReturn(testProduct);

        // When: 调用查询方法
        Product result = productService.getProductById(1L);

        // Then: 直接返回缓存数据，不查数据库
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("温湿度传感器");
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("根据ID查询产品 - 缓存未命中查询数据库")
    void getProductById_CacheMiss() {
        // Given: Redis 缓存未命中，数据库有数据
        when(redisUtil.get(eq("product:info:1"), eq(Product.class))).thenReturn(null);
        doReturn(testProduct).when(productMapper).selectById(1L);

        // When: 调用查询方法
        Product result = productService.getProductById(1L);

        // Then: 从数据库查询并写入缓存
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("温湿度传感器");
        verify(productMapper).selectById(1L);
        verify(redisUtil).set(anyString(), any(Product.class), anyLong());
    }

    @Test
    @DisplayName("根据ID查询产品 - 产品不存在抛出异常")
    void getProductById_NotFound() {
        // Given: 缓存和数据库都没有
        when(redisUtil.get(anyString(), eq(Product.class))).thenReturn(null);
        doReturn(null).when(productMapper).selectById(999L);

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(BusinessException.class);
    }
}
