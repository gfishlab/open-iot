package com.openiot.device.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 设备服务单元测试
 *
 * <p>测试 DeviceService 的核心功能：设备注册、认证、查询、编码唯一性校验。
 * 使用 Mockito mock DeviceMapper 和 ProductService，
 * 通过 MockedStatic 模拟 TenantContext 静态方法。
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("设备服务测试")
class DeviceServiceTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private DeviceService deviceService;

    /** 模拟 TenantContext 静态方法 */
    private MockedStatic<TenantContext> tenantContextMock;

    /** 测试用设备实体 */
    private Device testDevice;

    /** 测试用产品实体 */
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 手动注入 baseMapper（Mockito 的 @InjectMocks 无法注入 ServiceImpl 的 baseMapper 字段）
        ReflectionTestUtils.setField(deviceService, "baseMapper", deviceMapper);

        // 初始化 MyBatis Plus 表信息缓存，避免 lambda 相关操作出错
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Device.class);

        // 初始化 TenantContext 静态 mock
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("1");
        tenantContextMock.when(TenantContext::isPlatformAdmin).thenReturn(false);

        // 初始化测试设备
        testDevice = new Device();
        testDevice.setId(1L);
        testDevice.setTenantId(1L);
        testDevice.setProductId(1L);
        testDevice.setDeviceCode("device001");
        testDevice.setDeviceName("测试设备");
        testDevice.setDeviceKey("test-device-key");
        // BCrypt 编码后的 "test-device-secret"
        testDevice.setDeviceSecret("$2a$10$dummyBCryptHashForTestingPurposes123456789012");
        testDevice.setDeviceToken("abc123token");
        testDevice.setProtocolType("MQTT");
        testDevice.setStatus("1");

        // 初始化测试产品
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTenantId(1L);
        testProduct.setProductKey("PROD_ABC123");
        testProduct.setProductName("温湿度传感器");
        testProduct.setProtocolType("MQTT");
    }

    @AfterEach
    void tearDown() {
        // 释放静态 mock
        tenantContextMock.close();
    }

    // ==================== 注册设备 ====================

    @Test
    @DisplayName("注册设备成功")
    void createDevice_Success() {
        // Given: 构造待创建的设备
        Device newDevice = new Device();
        newDevice.setDeviceCode("device002");
        newDevice.setDeviceName("新设备");
        newDevice.setProtocolType("MQTT");

        // Mock: 设备编码不重复，保存成功
        doReturn(0L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(deviceMapper).insert(any(Device.class));

        // When: 调用创建方法
        Device result = deviceService.createDevice(newDevice);

        // Then: 验证设备创建结果
        assertThat(result).isNotNull();
        assertThat(result.getDeviceCode()).isEqualTo("device002");
        assertThat(result.getTenantId()).isEqualTo(1L);
        assertThat(result.getDeviceToken()).isNotNull().isNotEmpty();
        assertThat(result.getStatus()).isEqualTo("1");

        verify(deviceMapper).insert(any(Device.class));
    }

    @Test
    @DisplayName("注册设备 - 设备编码重复抛出异常")
    void createDevice_DuplicateCode() {
        // Given: 设备编码已存在
        Device newDevice = new Device();
        newDevice.setTenantId(1L);
        newDevice.setDeviceCode("device001");

        // Mock: 返回已存在的记录数
        doReturn(1L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));

        // When & Then: 应抛出 BusinessException
        assertThatThrownBy(() -> deviceService.createDevice(newDevice))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备编码已存在");

        // 验证没有执行插入操作
        verify(deviceMapper, never()).insert(any(Device.class));
    }

    // ==================== 设备认证 ====================

    @Test
    @DisplayName("设备认证（deviceKey + deviceSecret）- 成功")
    void authenticateDevice_Success() {
        // Given: 准备带有 BCrypt 哈希密钥的设备
        // 注意：实际的 BCrypt 匹配需要正确的哈希值，这里通过 spy 方式绕过
        Device dbDevice = new Device();
        dbDevice.setId(1L);
        dbDevice.setDeviceCode("device001");
        dbDevice.setDeviceKey("test-key");
        dbDevice.setStatus("1");

        // 使用 BCryptPasswordEncoder 生成正确的哈希
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        dbDevice.setDeviceSecret(encoder.encode("test-secret"));

        // Mock: 根据 deviceKey 查询到设备
        doReturn(dbDevice).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When: 调用认证方法
        Device result = deviceService.authenticateDevice("test-key", "test-secret");

        // Then: 认证成功，返回设备
        assertThat(result).isNotNull();
        assertThat(result.getDeviceCode()).isEqualTo("device001");
    }

    @Test
    @DisplayName("设备认证 - DeviceKey不存在")
    void authenticateDevice_KeyNotFound() {
        // Given: 查询不到设备
        doReturn(null).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When & Then: 抛出认证失败异常
        assertThatThrownBy(() -> deviceService.authenticateDevice("invalid-key", "any-secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备认证失败");
    }

    @Test
    @DisplayName("设备认证 - DeviceSecret错误")
    void authenticateDevice_WrongSecret() {
        // Given: 设备存在但密钥不匹配
        Device dbDevice = new Device();
        dbDevice.setId(1L);
        dbDevice.setDeviceKey("test-key");
        dbDevice.setStatus("1");

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        dbDevice.setDeviceSecret(encoder.encode("correct-secret"));

        doReturn(dbDevice).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When & Then: 密钥错误，抛出认证失败异常
        assertThatThrownBy(() -> deviceService.authenticateDevice("test-key", "wrong-secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备认证失败");
    }

    @Test
    @DisplayName("设备认证 - 设备已禁用")
    void authenticateDevice_Disabled() {
        // Given: 设备已禁用
        Device dbDevice = new Device();
        dbDevice.setId(1L);
        dbDevice.setDeviceCode("device001");
        dbDevice.setDeviceKey("test-key");
        dbDevice.setStatus("0"); // 禁用状态

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        dbDevice.setDeviceSecret(encoder.encode("test-secret"));

        doReturn(dbDevice).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When & Then: 设备已禁用，抛出异常
        assertThatThrownBy(() -> deviceService.authenticateDevice("test-key", "test-secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备已禁用");
    }

    // ==================== 查询设备列表（分页）====================

    @Test
    @DisplayName("查询设备列表（分页）")
    void page_Success() {
        // Given: 准备分页结果
        Page<Device> expectedPage = new Page<>(1, 10);
        expectedPage.setTotal(1);
        expectedPage.setRecords(java.util.List.of(testDevice));

        // Mock: 分页查询返回预期结果
        doReturn(expectedPage).when(deviceMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        // When: 调用分页查询
        Page<Device> result = deviceService.page(1, 10, null, null);

        // Then: 验证分页结果
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDeviceCode()).isEqualTo("device001");

        verify(deviceMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询设备列表 - 带条件过滤")
    void page_WithFilters() {
        // Given: 准备空的分页结果
        Page<Device> expectedPage = new Page<>(1, 10);
        expectedPage.setTotal(0);

        doReturn(expectedPage).when(deviceMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        // When: 带状态和协议类型条件查询
        Page<Device> result = deviceService.page(1, 10, "1", "MQTT");

        // Then: 验证调用了分页查询
        assertThat(result).isNotNull();
        verify(deviceMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== 设备编码唯一性校验 ====================

    @Test
    @DisplayName("设备编码唯一性校验 - 编码不重复允许创建")
    void createDevice_UniqueCode() {
        // Given: 新设备编码在租户内唯一
        Device newDevice = new Device();
        newDevice.setDeviceCode("unique_device_code");
        newDevice.setDeviceName("唯一编码设备");

        // Mock: 查询编码不存在
        doReturn(0L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(deviceMapper).insert(any(Device.class));

        // When: 调用创建方法
        Device result = deviceService.createDevice(newDevice);

        // Then: 创建成功
        assertThat(result).isNotNull();
        assertThat(result.getDeviceCode()).isEqualTo("unique_device_code");
    }

    @Test
    @DisplayName("设备编码唯一性校验 - 编码重复拒绝创建")
    void createDevice_DuplicateCodeValidation() {
        // Given: 设备编码已被同租户使用
        Device newDevice = new Device();
        newDevice.setTenantId(1L);
        newDevice.setDeviceCode("existing_code");

        // Mock: 查询编码已存在
        doReturn(1L).when(deviceMapper).selectCount(any(LambdaQueryWrapper.class));

        // When & Then: 重复编码应抛出异常
        assertThatThrownBy(() -> deviceService.createDevice(newDevice))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备编码已存在");

        // 确保不执行插入
        verify(deviceMapper, never()).insert(any(Device.class));
    }

    // ==================== 根据设备编码查询 ====================

    @Test
    @DisplayName("根据设备编码查询 - 成功")
    void getByCode_Success() {
        // Given
        doReturn(testDevice).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When
        Device result = deviceService.getByCode("device001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDeviceCode()).isEqualTo("device001");
    }

    @Test
    @DisplayName("根据设备编码查询 - 设备不存在返回null")
    void getByCode_NotFound() {
        // Given
        doReturn(null).when(deviceMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        // When
        Device result = deviceService.getByCode("nonexistent");

        // Then
        assertThat(result).isNull();
    }
}
