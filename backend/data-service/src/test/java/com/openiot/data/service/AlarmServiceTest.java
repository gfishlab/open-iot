package com.openiot.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.data.alarm.AlarmService;
import com.openiot.data.entity.AlarmRecord;
import com.openiot.data.entity.AlarmRule;
import com.openiot.data.mapper.AlarmRecordMapper;
import com.openiot.data.mapper.AlarmRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 告警服务单元测试
 * 覆盖告警规则匹配、阈值触发、表达式触发、变化率触发、静默期处理、告警恢复等场景
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("告警服务测试")
class AlarmServiceTest {

    @Mock
    private AlarmRuleMapper ruleMapper;

    @Mock
    private AlarmRecordMapper recordMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 使用真实的 ObjectMapper，因为测试需要真正解析 JSON
     */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AlarmService alarmService;

    // ========== 公共测试数据 ==========
    private static final Long TENANT_ID = 1L;
    private static final Long DEVICE_ID = 100L;
    private static final String DEVICE_CODE = "device001";
    private static final Long PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        // 每个测试前重置状态（AlarmService 内部有 ConcurrentHashMap 缓存）
        // 由于 @InjectMocks 每次会重新创建实例，缓存自然是空的
    }

    // ==========================================================
    // T031-1: 告警规则匹配 - 阈值触发 (threshold)
    // ==========================================================

    @Nested
    @DisplayName("阈值触发测试")
    class ThresholdTriggerTests {

        @Test
        @DisplayName("阈值触发 - 温度超过上限触发告警")
        void processDeviceData_ThresholdExceeded_ShouldCreateAlarm() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            // 没有活动告警
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报温度 55（超过阈值 50）
            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应创建一条告警记录
            ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
            verify(recordMapper).insert(captor.capture());

            AlarmRecord record = captor.getValue();
            assertThat(record.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(record.getDeviceId()).isEqualTo(DEVICE_ID);
            assertThat(record.getDeviceCode()).isEqualTo(DEVICE_CODE);
            assertThat(record.getAlarmStatus()).isEqualTo("active");
            assertThat(record.getAlarmLevel()).isEqualTo("critical");
            assertThat(record.getTriggerValue()).contains("temperature");
        }

        @Test
        @DisplayName("阈值触发 - 温度未超过上限不触发告警")
        void processDeviceData_ThresholdNotExceeded_ShouldNotCreateAlarm() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 设备上报温度 45（未超过阈值 50）
            Map<String, Object> propertyData = Map.of("temperature", 45.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 不应创建告警记录（insert 不应被调用）
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("阈值触发 - 小于等于运算符")
        void processDeviceData_ThresholdLessOrEqual_ShouldCreateAlarm() {
            // Given: 电压 <= 3.0 时触发告警
            AlarmRule rule = buildThresholdRule("<=", 3.0, "voltage");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报电压 2.8（低于阈值 3.0）
            Map<String, Object> propertyData = Map.of("voltage", 2.8);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应创建告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("阈值触发 - 属性值不存在不触发告警")
        void processDeviceData_PropertyMissing_ShouldNotTrigger() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 设备上报数据中不包含 temperature 字段
            Map<String, Object> propertyData = Map.of("humidity", 60.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 不应创建告警
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("阈值触发 - 不等于运算符")
        void processDeviceData_ThresholdNotEqual_ShouldCreateAlarm() {
            // Given: status != 0 时触发告警
            AlarmRule rule = buildThresholdRule("!=", 0.0, "errorCode");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报 errorCode = 1（不等于 0）
            Map<String, Object> propertyData = Map.of("errorCode", 1.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应创建告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }
    }

    // ==========================================================
    // T031-2: 表达式触发 (expression)
    // ==========================================================

    @Nested
    @DisplayName("表达式触发测试")
    class ExpressionTriggerTests {

        @Test
        @DisplayName("表达式触发 - 变量替换后触发告警")
        void processDeviceData_ExpressionTriggered_ShouldCreateAlarm() {
            // Given: 表达式条件
            AlarmRule rule = buildExpressionRule("${temperature} > 50 && ${humidity} < 20");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报数据，变量替换后不包含 null/undefined
            Map<String, Object> propertyData = new HashMap<>();
            propertyData.put("temperature", 55.0);
            propertyData.put("humidity", 15.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应创建告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("表达式触发 - 缺少变量不触发告警")
        void processDeviceData_ExpressionMissingVariable_ShouldNotTrigger() {
            // Given: 表达式中引用了 temperature，但设备数据中没有该字段
            AlarmRule rule = buildExpressionRule("${temperature} > 50");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 设备上报数据中不包含 temperature（替换后表达式包含 null）
            Map<String, Object> propertyData = Map.of("humidity", 60.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 表达式中会残留未替换的 ${temperature}，不触发告警
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }
    }

    // ==========================================================
    // T031-3: 变化率触发 (rate)
    // ==========================================================

    @Nested
    @DisplayName("变化率触发测试")
    class RateTriggerTests {

        @Test
        @DisplayName("变化率触发 - 首次上报不触发告警")
        void processDeviceData_RateFirstReport_ShouldNotTrigger() {
            // Given: 变化率超过 10 时触发告警
            AlarmRule rule = buildRateRule("temperature", 10.0);
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 首次上报温度数据
            Map<String, Object> propertyData = Map.of("temperature", 25.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 首次上报不触发告警
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("变化率触发 - 变化超过阈值触发告警")
        void processDeviceData_RateExceeded_ShouldCreateAlarm() {
            // Given: 变化率超过 10 时触发告警
            AlarmRule rule = buildRateRule("temperature", 10.0);
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 第一次上报：建立基线
            Map<String, Object> firstData = Map.of("temperature", 25.0);
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, firstData);

            // 第二次上报：温度急剧变化（从 25 到 40，变化 15 > 阈值 10）
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            Map<String, Object> secondData = Map.of("temperature", 40.0);
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, secondData);

            // Then: 第二次上报应触发告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("变化率触发 - 变化未超过阈值不触发告警")
        void processDeviceData_RateNotExceeded_ShouldNotTrigger() {
            // Given: 变化率超过 10 时触发告警
            AlarmRule rule = buildRateRule("temperature", 10.0);
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 第一次上报：建立基线
            Map<String, Object> firstData = Map.of("temperature", 25.0);
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, firstData);

            // 第二次上报：温度小幅变化（从 25 到 30，变化 5 < 阈值 10）
            Map<String, Object> secondData = Map.of("temperature", 30.0);
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, secondData);

            // Then: 不应触发告警
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }
    }

    // ==========================================================
    // T031-4: 静默期处理
    // ==========================================================

    @Nested
    @DisplayName("静默期处理测试")
    class SilencePeriodTests {

        @Test
        @DisplayName("静默期内不重复创建告警")
        void processDeviceData_WithinSilencePeriod_ShouldNotCreateAlarm() {
            // Given: 启用静默期（300秒），且刚才已触发过告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            rule.setSilenceEnabled(true);
            rule.setSilenceSeconds(300);

            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 没有活动告警
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null)   // 第一次调用：查询活动告警 → 无
                    .thenReturn(buildRecentAlarmRecord()); // 第二次调用：查询最近告警 → 在静默期内

            // 设备上报温度 55（超过阈值）
            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 由于在静默期内，不应创建新告警
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("静默期外正常创建告警")
        void processDeviceData_OutsideSilencePeriod_ShouldCreateAlarm() {
            // Given: 启用静默期（300秒），但上次告警时间超过静默期
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            rule.setSilenceEnabled(true);
            rule.setSilenceSeconds(300);

            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 构建过期告警记录（10分钟前）
            AlarmRecord expiredAlarm = new AlarmRecord();
            expiredAlarm.setAlarmTime(LocalDateTime.now().minusMinutes(10));

            when(recordMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null)        // 第一次调用：查询活动告警 → 无
                    .thenReturn(expiredAlarm); // 第二次调用：查询最近告警 → 已超过静默期
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报温度 55
            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 静默期已过，应创建新告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("未启用静默期时正常创建告警")
        void processDeviceData_SilenceDisabled_ShouldCreateAlarm() {
            // Given: 未启用静默期
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            rule.setSilenceEnabled(false);

            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 无静默期限制，直接创建告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }
    }

    // ==========================================================
    // T031-5: 告警恢复
    // ==========================================================

    @Nested
    @DisplayName("告警恢复测试")
    class AlarmRecoveryTests {

        @Test
        @DisplayName("告警恢复 - 数据恢复正常时自动解除告警")
        void processDeviceData_RecoveredFromAlarm_ShouldResolve() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 存在活动告警
            AlarmRecord activeAlarm = new AlarmRecord();
            activeAlarm.setId(1L);
            activeAlarm.setRuleId(rule.getId());
            activeAlarm.setDeviceId(DEVICE_ID);
            activeAlarm.setAlarmStatus("active");

            // 第一次 selectOne：查询活动告警（在 evaluateRule 调用 handleAlarmRecovered 时使用 selectList）
            // 由于规则未触发，会走 handleAlarmRecovered 分支，调用 selectList
            when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activeAlarm));
            when(recordMapper.updateById(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报温度 45（低于阈值 50，告警条件不再满足）
            Map<String, Object> propertyData = Map.of("temperature", 45.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 活动告警应被更新为 resolved
            ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
            verify(recordMapper).updateById(captor.capture());

            AlarmRecord updated = captor.getValue();
            assertThat(updated.getAlarmStatus()).isEqualTo("resolved");
            assertThat(updated.getRecoverTime()).isNotNull();
        }

        @Test
        @DisplayName("告警恢复 - 无活动告警时不执行任何操作")
        void processDeviceData_NoActiveAlarm_ShouldDoNothing() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(rule))   // ruleMapper.selectList 返回规则
                    .thenReturn(Collections.emptyList()); // 对应其他可能的 selectList

            // 没有活动告警
            when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // 设备上报温度 45（低于阈值，不触发告警）
            Map<String, Object> propertyData = Map.of("temperature", 45.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 无活动告警，不应执行任何更新
            verify(recordMapper, never()).updateById(any(AlarmRecord.class));
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("告警恢复 - 已存在活动告警时更新触发值而非新建")
        void processDeviceData_ActiveAlarmExists_ShouldUpdateNotInsert() {
            // Given: 温度 > 50 时触发告警
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

            // 已有活动告警
            AlarmRecord existingAlarm = new AlarmRecord();
            existingAlarm.setId(1L);
            existingAlarm.setRuleId(rule.getId());
            existingAlarm.setDeviceId(DEVICE_ID);
            existingAlarm.setAlarmStatus("active");
            existingAlarm.setTriggerValue("temperature=55");

            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingAlarm);
            when(recordMapper.updateById(any(AlarmRecord.class))).thenReturn(1);

            // 设备上报温度 60（仍然超过阈值）
            Map<String, Object> propertyData = Map.of("temperature", 60.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应更新已有告警的触发值，不应新建
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
            verify(recordMapper).updateById(any(AlarmRecord.class));
        }
    }

    // ==========================================================
    // 其他边界场景
    // ==========================================================

    @Nested
    @DisplayName("边界场景测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("无匹配的告警规则时不执行任何操作")
        void processDeviceData_NoMatchingRules_ShouldDoNothing() {
            // Given: 无匹配规则
            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 不应有任何告警操作
            verify(recordMapper, never()).insert(any(AlarmRecord.class));
            verify(recordMapper, never()).updateById(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("规则评估异常不影响其他规则执行")
        void processDeviceData_RuleEvaluationError_ShouldContinue() {
            // Given: 两条规则，第一条条件JSON无效，第二条正常
            AlarmRule badRule = buildThresholdRule(">", 50.0, "temperature");
            badRule.setTriggerCondition("invalid-json"); // 无效JSON

            AlarmRule goodRule = buildThresholdRule(">", 50.0, "temperature");
            goodRule.setId(2L);

            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(badRule, goodRule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When: 不应抛出异常
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 第二条规则仍应正常评估并创建告警
            verify(recordMapper).insert(any(AlarmRecord.class));
        }

        @Test
        @DisplayName("清理过期缓存 - 正常执行无异常")
        void cleanExpiredCache_ShouldNotThrow() {
            // When & Then: 清理空缓存不应抛出异常
            alarmService.cleanExpiredCache();
        }

        @Test
        @DisplayName("通知类型为 none 时不发送通知")
        void processDeviceData_NotifyTypeNone_ShouldNotSendNotification() {
            // Given: 通知类型为 none
            AlarmRule rule = buildThresholdRule(">", 50.0, "temperature");
            rule.setNotifyType("none");

            when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));
            when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(recordMapper.insert(any(AlarmRecord.class))).thenReturn(1);

            Map<String, Object> propertyData = Map.of("temperature", 55.0);

            // When
            alarmService.processDeviceData(TENANT_ID, DEVICE_ID, DEVICE_CODE, PRODUCT_ID, propertyData);

            // Then: 应创建告警，但 notifyStatus 更新不应因通知发送而改变
            ArgumentCaptor<AlarmRecord> captor = ArgumentCaptor.forClass(AlarmRecord.class);
            verify(recordMapper).insert(captor.capture());
            assertThat(captor.getValue().getNotifyStatus()).isEqualTo("none");

            // restTemplate 不应被调用（webhook 通知）
            verify(restTemplate, never()).postForObject(anyString(), any(), any());
        }
    }

    // ==========================================================
    // 辅助方法：构建测试数据
    // ==========================================================

    /**
     * 构建阈值类型的告警规则
     *
     * @param operator  运算符（>、>=、<、<=、==、!=）
     * @param value     阈值
     * @param property  属性名
     * @return 告警规则
     */
    private AlarmRule buildThresholdRule(String operator, double value, String property) {
        AlarmRule rule = new AlarmRule();
        rule.setId(1L);
        rule.setTenantId(TENANT_ID);
        rule.setRuleName("温度过高告警");
        rule.setAlarmLevel("critical");
        rule.setTriggerType("threshold");
        rule.setTriggerCondition(String.format(
                "{\"property\":\"%s\",\"operator\":\"%s\",\"value\":%s}",
                property, operator, value));
        rule.setContentTemplate("设备温度超过阈值");
        rule.setNotifyType("none");
        rule.setStatus("1");
        rule.setDelFlag("0");
        rule.setSilenceEnabled(false);
        return rule;
    }

    /**
     * 构建表达式类型的告警规则
     *
     * @param expression 表达式内容
     * @return 告警规则
     */
    private AlarmRule buildExpressionRule(String expression) {
        AlarmRule rule = new AlarmRule();
        rule.setId(1L);
        rule.setTenantId(TENANT_ID);
        rule.setRuleName("复合条件告警");
        rule.setAlarmLevel("warning");
        rule.setTriggerType("expression");
        rule.setTriggerCondition(String.format("{\"expression\":\"%s\"}", expression));
        rule.setContentTemplate("复合条件触发告警");
        rule.setNotifyType("none");
        rule.setStatus("1");
        rule.setDelFlag("0");
        rule.setSilenceEnabled(false);
        return rule;
    }

    /**
     * 构建变化率类型的告警规则
     *
     * @param property       属性名
     * @param changeThreshold 变化率阈值
     * @return 告警规则
     */
    private AlarmRule buildRateRule(String property, double changeThreshold) {
        AlarmRule rule = new AlarmRule();
        rule.setId(1L);
        rule.setTenantId(TENANT_ID);
        rule.setRuleName("温度变化率告警");
        rule.setAlarmLevel("warning");
        rule.setTriggerType("rate");
        rule.setTriggerCondition(String.format(
                "{\"property\":\"%s\",\"change\":%s}", property, changeThreshold));
        rule.setContentTemplate("属性变化率超标");
        rule.setNotifyType("none");
        rule.setStatus("1");
        rule.setDelFlag("0");
        rule.setSilenceEnabled(false);
        return rule;
    }

    /**
     * 构建静默期内的最近告警记录
     *
     * @return 最近的告警记录（1分钟前触发）
     */
    private AlarmRecord buildRecentAlarmRecord() {
        AlarmRecord record = new AlarmRecord();
        record.setId(1L);
        record.setAlarmTime(LocalDateTime.now().minusMinutes(1)); // 1分钟前触发
        record.setAlarmStatus("resolved");
        return record;
    }
}
