package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.entity.AlertRule;
import com.openiot.device.mapper.AlertRecordMapper;
import com.openiot.device.mapper.AlertRuleMapper;
import com.openiot.device.vo.AlertStatisticsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 告警服务单元测试
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("告警服务测试")
class AlertServiceTest {

    @Mock
    private AlertRecordMapper alertRecordMapper;

    @Mock
    private AlertRuleMapper alertRuleMapper;

    @InjectMocks
    private AlertService alertService;

    private AlertRecord testAlert;
    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testRule = new AlertRule();
        testRule.setId(1L);
        testRule.setTenantId(1L);
        testRule.setRuleName("温度告警");
        testRule.setAlertLevel("warning");
        testRule.setStatus("1");

        testAlert = new AlertRecord();
        testAlert.setId(1L);
        testAlert.setTenantId(1L);
        testAlert.setDeviceId(1L);
        testAlert.setRuleId(1L);
        testAlert.setAlertLevel("warning");
        testAlert.setAlertTitle("温度超限");
        testAlert.setAlertContent("设备温度超过阈值");
        testAlert.setStatus("pending");
        testAlert.setAlertTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("分页查询告警 - 成功")
    void queryAlerts_Success() {
        // Given
        Page<AlertRecord> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testAlert));
        page.setTotal(1);

        when(alertRecordMapper.selectPage(any(), any())).thenReturn(page);

        // When
        Page<AlertRecord> result = alertService.queryAlerts(1, 10, 1L, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getAlertTitle()).isEqualTo("温度超限");

        verify(alertRecordMapper, times(1)).selectPage(any(), any());
    }

    @Test
    @DisplayName("处理告警 - 成功")
    void handleAlert_Success() {
        // Given
        when(alertRecordMapper.selectById(1L)).thenReturn(testAlert);
        when(alertRecordMapper.updateById(any(AlertRecord.class))).thenReturn(1);

        // When
        AlertRecord result = alertService.handleAlert(1L, "resolved", "温度已恢复正常");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("resolved");
        assertThat(result.getHandleRemark()).isEqualTo("温度已恢复正常");

        verify(alertRecordMapper, times(1)).updateById(any(AlertRecord.class));
    }

    @Test
    @DisplayName("处理告警 - 状态流转错误")
    void handleAlert_InvalidStatusTransition() {
        // Given
        testAlert.setStatus("resolved");
        when(alertRecordMapper.selectById(1L)).thenReturn(testAlert);

        // When & Then
        assertThatThrownBy(() -> alertService.handleAlert(1L, "processing", "重新处理"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态流转错误");
    }

    @Test
    @DisplayName("批量处理告警 - 成功")
    void batchHandleAlerts_Success() {
        // Given
        List<Long> alertIds = Arrays.asList(1L, 2L, 3L);

        when(alertRecordMapper.selectById(1L)).thenReturn(testAlert);
        when(alertRecordMapper.updateById(any(AlertRecord.class))).thenReturn(1);

        // When
        int count = alertService.batchHandleAlerts(alertIds, "resolved", "批量处理");

        // Then
        assertThat(count).isEqualTo(3);
        verify(alertRecordMapper, times(3)).updateById(any(AlertRecord.class));
    }

    @Test
    @DisplayName("查询告警统计 - 成功")
    void getStatistics_Success() {
        // Given
        when(alertRecordMapper.selectCount(any())).thenReturn(10L); // pending
        when(alertRecordMapper.selectCount(any())).thenReturn(5L);  // processing
        when(alertRecordMapper.selectCount(any())).thenReturn(20L); // resolved
        when(alertRecordMapper.selectCount(any())).thenReturn(2L);  // ignored

        // When
        AlertStatisticsVO statistics = alertService.getStatistics(1L);

        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getTotalCount()).isGreaterThan(0);

        verify(alertRecordMapper, atLeast(4)).selectCount(any());
    }

    @Test
    @DisplayName("创建告警规则 - 成功")
    void createRule_Success() {
        // Given
        AlertRule rule = new AlertRule();
        rule.setRuleName("湿度告警");
        rule.setAlertLevel("critical");
        rule.setConditions("{\"humidity\": {\"gt\": 90}}");

        when(alertRuleMapper.insert(any(AlertRule.class))).thenReturn(1);

        // When
        alertService.createRule(rule);

        // Then
        verify(alertRuleMapper, times(1)).insert(any(AlertRule.class));
    }

    @Test
    @DisplayName("删除告警规则 - 成功")
    void deleteRule_Success() {
        // Given
        when(alertRuleMapper.selectById(1L)).thenReturn(testRule);
        when(alertRuleMapper.deleteById(1L)).thenReturn(1);

        // When
        alertService.deleteRule(1L);

        // Then
        verify(alertRuleMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除告警规则 - 规则不存在")
    void deleteRule_NotFound() {
        // Given
        when(alertRuleMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> alertService.deleteRule(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("告警规则不存在");

        verify(alertRuleMapper, never()).deleteById(any());
    }
}
