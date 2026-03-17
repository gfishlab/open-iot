package com.openiot.device.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.AlertRecordMapper;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.mapper.RuleMapper;
import com.openiot.device.metrics.AlertMetrics;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 告警服务单元测试
 * 测试告警查询、处理、批量处理、统计、状态流转等功能
 *
 * @author open-iot
 */
@DisplayName("告警服务测试")
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRecordMapper alertRecordMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private RuleMapper ruleMapper;

    @Mock
    private AlertMetrics alertMetrics;

    @InjectMocks
    private AlertService alertService;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        // 手动注入 baseMapper（Mockito 的 @InjectMocks 无法注入 ServiceImpl 的 baseMapper 字段）
        ReflectionTestUtils.setField(alertService, "baseMapper", alertRecordMapper);

        // 初始化 MyBatis Plus 表信息缓存，避免 LambdaUpdateWrapper 使用时出现 lambda cache 错误
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AlertRecord.class);

        tenantContextMock = mockStatic(TenantContext.class);
        // 默认设置为普通租户
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("1");
        tenantContextMock.when(TenantContext::getUserId).thenReturn("100");
        tenantContextMock.when(TenantContext::isPlatformAdmin).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    // ==================== 告警详情查询测试 ====================

    @Test
    @DisplayName("查询告警详情 - 正常返回")
    void getAlertDetail_success() {
        AlertRecord alert = buildAlert(1L, 1L, "critical", "pending");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);

        AlertRecord result = alertService.getAlertDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("critical", result.getAlertLevel());
    }

    @Test
    @DisplayName("查询告警详情 - 告警不存在应抛出异常")
    void getAlertDetail_notFound_shouldThrow() {
        when(alertRecordMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> alertService.getAlertDetail(999L));
    }

    @Test
    @DisplayName("查询告警详情 - 租户隔离，跨租户访问应抛出异常")
    void getAlertDetail_crossTenant_shouldThrow() {
        // 告警属于租户2，当前用户属于租户1
        AlertRecord alert = buildAlert(1L, 2L, "warning", "pending");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);

        assertThrows(BusinessException.class, () -> alertService.getAlertDetail(1L));
    }

    @Test
    @DisplayName("查询告警详情 - 平台管理员可跨租户访问")
    void getAlertDetail_platformAdmin_canAccessAnyTenant() {
        tenantContextMock.when(TenantContext::isPlatformAdmin).thenReturn(true);

        AlertRecord alert = buildAlert(1L, 2L, "critical", "pending");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);

        AlertRecord result = alertService.getAlertDetail(1L);

        assertNotNull(result);
        assertEquals(2L, result.getTenantId());
    }

    // ==================== 状态流转测试 ====================

    @Test
    @DisplayName("处理告警 - pending 转 processing（合法）")
    void handleAlert_pendingToProcessing_success() {
        AlertRecord alert = buildAlert(1L, 1L, "critical", "pending");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);
        when(alertRecordMapper.update(any(), any())).thenReturn(1);

        // handleAlert 内部会重新查一次（返回处理后的告警）
        AlertRecord updatedAlert = buildAlert(1L, 1L, "critical", "processing");
        // selectById 在 handleAlert 中调用两次: getAlertDetail + 最终 getById
        when(alertRecordMapper.selectById(1L)).thenReturn(alert).thenReturn(updatedAlert);

        AlertRecord result = alertService.handleAlert(1L, "processing");

        assertNotNull(result);
        verify(alertRecordMapper).update(any(), any());
        verify(alertMetrics).recordAlarmHandled("1", 1L, "processing");
    }

    @Test
    @DisplayName("处理告警 - pending 转 resolved（合法）")
    void handleAlert_pendingToResolved_success() {
        AlertRecord alert = buildAlert(1L, 1L, "warning", "pending");
        AlertRecord updated = buildAlert(1L, 1L, "warning", "resolved");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert).thenReturn(updated);
        when(alertRecordMapper.update(any(), any())).thenReturn(1);

        AlertRecord result = alertService.handleAlert(1L, "resolved");

        assertNotNull(result);
        verify(alertMetrics).recordAlarmHandled("1", 1L, "resolved");
    }

    @Test
    @DisplayName("处理告警 - resolved 转 processing（非法状态转换）")
    void handleAlert_resolvedToProcessing_shouldThrow() {
        AlertRecord alert = buildAlert(1L, 1L, "critical", "resolved");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);

        assertThrows(BusinessException.class, () -> alertService.handleAlert(1L, "processing"));
    }

    @Test
    @DisplayName("处理告警 - ignored 状态不能再转换")
    void handleAlert_ignoredToAny_shouldThrow() {
        AlertRecord alert = buildAlert(1L, 1L, "info", "ignored");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert);

        assertThrows(BusinessException.class, () -> alertService.handleAlert(1L, "resolved"));
    }

    @Test
    @DisplayName("处理告警 - processing 转 resolved（合法）")
    void handleAlert_processingToResolved_success() {
        AlertRecord alert = buildAlert(1L, 1L, "critical", "processing");
        AlertRecord updated = buildAlert(1L, 1L, "critical", "resolved");
        when(alertRecordMapper.selectById(1L)).thenReturn(alert).thenReturn(updated);
        when(alertRecordMapper.update(any(), any())).thenReturn(1);

        AlertRecord result = alertService.handleAlert(1L, "resolved");

        assertNotNull(result);
    }

    // ==================== 批量处理测试 ====================

    @Test
    @DisplayName("批量处理告警 - 正常处理")
    void batchHandleAlerts_success() {
        List<Long> alertIds = List.of(1L, 2L, 3L);
        when(alertRecordMapper.update(any(), any())).thenReturn(1);

        int count = alertService.batchHandleAlerts(alertIds, "resolved");

        assertEquals(3, count);
        verify(alertMetrics).recordAlarmBatchHandled("1", 3, "resolved");
    }

    // ==================== 统计测试 ====================

    @Test
    @DisplayName("告警统计 - 按级别和状态统计")
    void getStatistics_success() {
        // 准备测试数据
        List<AlertRecord> alerts = List.of(
                buildAlert(1L, 1L, "critical", "pending"),
                buildAlert(2L, 1L, "critical", "resolved"),
                buildAlert(3L, 1L, "warning", "pending"),
                buildAlert(4L, 1L, "info", "ignored")
        );

        when(alertRecordMapper.selectList(any())).thenReturn(alerts);

        AlertService.AlertStatisticsVO stats = alertService.getStatistics(null);

        assertNotNull(stats);
        assertEquals(4L, stats.getTotalCount());
        assertEquals(2L, stats.getLevelCount().get("critical"));
        assertEquals(1L, stats.getLevelCount().get("warning"));
        assertEquals(1L, stats.getLevelCount().get("info"));
        assertEquals(2L, stats.getPendingCount());
        assertEquals(1L, stats.getResolvedCount());
        assertEquals(2L, stats.getCriticalCount());
    }

    // ==================== 设备告警历史测试 ====================

    @Test
    @DisplayName("查询设备告警历史 - 设备不存在应抛出异常")
    void getDeviceAlertHistory_deviceNotFound_shouldThrow() {
        when(deviceMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> alertService.getDeviceAlertHistory(999L, 10));
    }

    @Test
    @DisplayName("查询设备告警历史 - 跨租户设备应抛出异常")
    void getDeviceAlertHistory_crossTenantDevice_shouldThrow() {
        Device device = new Device();
        device.setId(1L);
        device.setTenantId(2L);  // 不同租户
        when(deviceMapper.selectById(1L)).thenReturn(device);

        assertThrows(BusinessException.class, () -> alertService.getDeviceAlertHistory(1L, 10));
    }

    @Test
    @DisplayName("查询设备告警历史 - 正常查询")
    void getDeviceAlertHistory_success() {
        Device device = new Device();
        device.setId(1L);
        device.setTenantId(1L);
        when(deviceMapper.selectById(1L)).thenReturn(device);

        List<AlertRecord> alerts = List.of(buildAlert(1L, 1L, "warning", "pending"));
        when(alertRecordMapper.selectList(any())).thenReturn(alerts);

        List<AlertRecord> result = alertService.getDeviceAlertHistory(1L, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用的告警记录
     */
    private AlertRecord buildAlert(Long id, Long tenantId, String level, String status) {
        AlertRecord alert = new AlertRecord();
        alert.setId(id);
        alert.setTenantId(tenantId);
        alert.setDeviceId(1L);
        alert.setProductId(1L);
        alert.setAlertLevel(level);
        alert.setAlertTitle("测试告警");
        alert.setAlertContent("测试告警内容");
        alert.setStatus(status);
        alert.setCreateTime(LocalDateTime.now());
        return alert;
    }
}
