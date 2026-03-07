package com.openiot.device.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 告警业务指标收集器
 *
 * <p>收集告警触发、处理等关键业务指标
 *
 * <h3>指标列表：</h3>
 * <ul>
 *   <li>alarm.triggered.count - 告警触发数</li>
 *   <li>alarm.handled.count - 告警处理数</li>
 *   <li>alarm.resolved.count - 告警解决数</li>
 *   <li>alarm.ignored.count - 告警忽略数</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class AlertMetrics {

    private static final String PREFIX = "openiot.alert.";

    private final MeterRegistry meterRegistry;

    /**
     * 告警触发计数器
     */
    @Getter
    private final Counter triggeredCounter;

    /**
     * 告警处理计数器
     */
    @Getter
    private final Counter handledCounter;

    /**
     * 告警解决计数器
     */
    @Getter
    private final Counter resolvedCounter;

    /**
     * 告警忽略计数器
     */
    @Getter
    private final Counter ignoredCounter;

    public AlertMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("初始化告警业务指标收集器");

        // 初始化告警指标
        this.triggeredCounter = Counter.builder(PREFIX + "triggered.count")
                .description("告警触发数")
                .register(meterRegistry);

        this.handledCounter = Counter.builder(PREFIX + "handled.count")
                .description("告警处理数")
                .register(meterRegistry);

        this.resolvedCounter = Counter.builder(PREFIX + "resolved.count")
                .description("告警解决数")
                .register(meterRegistry);

        this.ignoredCounter = Counter.builder(PREFIX + "ignored.count")
                .description("告警忽略数")
                .register(meterRegistry);

        log.info("告警业务指标初始化完成");
    }

    /**
     * 记录告警触发
     *
     * @param tenantId  租户ID
     * @param deviceId  设备ID
     * @param alertLevel 告警级别
     * @param ruleId    触发的规则ID
     */
    public void recordAlarmTriggered(String tenantId, Long deviceId, String alertLevel, Long ruleId) {
        triggeredCounter.increment();

        // 按租户、级别、规则分类统计
        Counter.builder(PREFIX + "triggered.count")
                .tag("tenant_id", tenantId)
                .tag("level", alertLevel)
                .tag("rule_id", String.valueOf(ruleId))
                .register(meterRegistry)
                .increment();

        log.debug("记录告警触发: tenant={}, device={}, level={}, rule={}",
                tenantId, deviceId, alertLevel, ruleId);
    }

    /**
     * 记录告警处理
     *
     * @param tenantId 租户ID
     * @param alertId  告警ID
     * @param status   处理状态
     */
    public void recordAlarmHandled(String tenantId, Long alertId, String status) {
        handledCounter.increment();

        // 按租户和处理状态分类统计
        Counter.builder(PREFIX + "handled.count")
                .tag("tenant_id", tenantId)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        // 根据状态更新对应计数器
        switch (status) {
            case "resolved" -> resolvedCounter.increment();
            case "ignored" -> ignoredCounter.increment();
            default -> log.debug("告警状态变更: alertId={}, status={}", alertId, status);
        }

        log.debug("记录告警处理: tenant={}, alertId={}, status={}", tenantId, alertId, status);
    }

    /**
     * 记录告警批量处理
     *
     * @param tenantId 租户ID
     * @param count    处理数量
     * @param status   处理状态
     */
    public void recordAlarmBatchHandled(String tenantId, int count, String status) {
        for (int i = 0; i < count; i++) {
            handledCounter.increment();
            switch (status) {
                case "resolved" -> resolvedCounter.increment();
                case "ignored" -> ignoredCounter.increment();
            }
        }

        log.debug("记录告警批量处理: tenant={}, count={}, status={}", tenantId, count, status);
    }
}
