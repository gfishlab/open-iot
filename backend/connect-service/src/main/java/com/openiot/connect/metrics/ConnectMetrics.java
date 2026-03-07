package com.openiot.connect.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 连接服务业务指标收集器
 *
 * <p>收集设备连接、断开、解析规则执行等关键业务指标
 *
 * <h3>指标列表：</h3>
 * <ul>
 *   <li>device.connected.count - 设备连接数计数器</li>
 *   <li>device.disconnected.count - 设备断开数计数器</li>
 *   <li>parse.rule.execution.time - 解析规则执行时间</li>
 *   <li>parse.rule.success.count - 解析成功数</li>
 *   <li>parse.rule.failure.count - 解析失败数</li>
 *   <li>forward.success.count - 转发成功数（Kafka）</li>
 *   <li>forward.failure.count - 转发失败数（Kafka）</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ConnectMetrics {

    private static final String PREFIX = "openiot.connect.";

    private final MeterRegistry meterRegistry;

    // ==================== 设备连接指标 ====================

    /**
     * 设备连接计数器
     */
    @Getter
    private final Counter deviceConnectedCounter;

    /**
     * 设备断开计数器
     */
    @Getter
    private final Counter deviceDisconnectedCounter;

    // ==================== 解析规则指标 ====================

    /**
     * 解析规则执行计时器
     */
    @Getter
    private final Timer parseRuleExecutionTimer;

    /**
     * 解析成功计数器
     */
    @Getter
    private final Counter parseSuccessCounter;

    /**
     * 解析失败计数器
     */
    @Getter
    private final Counter parseFailureCounter;

    // ==================== 转发指标 ====================

    /**
     * 转发成功计数器
     */
    @Getter
    private final Counter forwardSuccessCounter;

    /**
     * 转发失败计数器
     */
    @Getter
    private final Counter forwardFailureCounter;

    public ConnectMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("初始化连接服务业务指标收集器");

        // 初始化设备连接指标
        this.deviceConnectedCounter = Counter.builder(PREFIX + "device.connected.count")
                .description("设备连接数计数器")
                .register(meterRegistry);

        this.deviceDisconnectedCounter = Counter.builder(PREFIX + "device.disconnected.count")
                .description("设备断开数计数器")
                .register(meterRegistry);

        // 初始化解析规则指标
        this.parseRuleExecutionTimer = Timer.builder(PREFIX + "parse.rule.execution.time")
                .description("解析规则执行时间")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(java.time.Duration.ofMillis(1))
                .maximumExpectedValue(java.time.Duration.ofSeconds(10))
                .register(meterRegistry);

        this.parseSuccessCounter = Counter.builder(PREFIX + "parse.rule.success.count")
                .description("解析成功数")
                .register(meterRegistry);

        this.parseFailureCounter = Counter.builder(PREFIX + "parse.rule.failure.count")
                .description("解析失败数")
                .register(meterRegistry);

        // 初始化转发指标
        this.forwardSuccessCounter = Counter.builder(PREFIX + "forward.success.count")
                .description("转发成功数（发送到 Kafka）")
                .register(meterRegistry);

        this.forwardFailureCounter = Counter.builder(PREFIX + "forward.failure.count")
                .description("转发失败数（发送到 Kafka 失败）")
                .register(meterRegistry);

        log.info("连接服务业务指标初始化完成");
    }

    // ==================== 设备连接指标方法 ====================

    /**
     * 记录设备连接
     *
     * @param tenantId 租户ID
     * @param deviceId 设备ID
     * @param protocol 协议类型
     */
    public void recordDeviceConnected(String tenantId, String deviceId, String protocol) {
        deviceConnectedCounter.increment();

        // 按租户和协议分类统计
        Counter.builder(PREFIX + "device.connected.count")
                .tag("tenant_id", tenantId)
                .tag("protocol", protocol)
                .register(meterRegistry)
                .increment();

        log.debug("记录设备连接: tenant={}, device={}, protocol={}", tenantId, deviceId, protocol);
    }

    /**
     * 记录设备断开
     *
     * @param tenantId 租户ID
     * @param deviceId 设备ID
     * @param reason   断开原因
     */
    public void recordDeviceDisconnected(String tenantId, String deviceId, String reason) {
        deviceDisconnectedCounter.increment();

        // 按租户和断开原因分类统计
        Counter.builder(PREFIX + "device.disconnected.count")
                .tag("tenant_id", tenantId)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        log.debug("记录设备断开: tenant={}, device={}, reason={}", tenantId, deviceId, reason);
    }

    // ==================== 解析规则指标方法 ====================

    /**
     * 记录解析规则执行时间
     *
     * @param durationMillis 执行时间（毫秒）
     * @param ruleType       规则类型
     * @param success        是否成功
     */
    public void recordParseRuleExecution(long durationMillis, String ruleType, boolean success) {
        parseRuleExecutionTimer.record(durationMillis, TimeUnit.MILLISECONDS);

        // 按规则类型分类统计
        Timer.builder(PREFIX + "parse.rule.execution.time")
                .tag("rule_type", ruleType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMillis, TimeUnit.MILLISECONDS);

        if (success) {
            parseSuccessCounter.increment();
            Counter.builder(PREFIX + "parse.rule.success.count")
                    .tag("rule_type", ruleType)
                    .register(meterRegistry)
                    .increment();
        } else {
            parseFailureCounter.increment();
            Counter.builder(PREFIX + "parse.rule.failure.count")
                    .tag("rule_type", ruleType)
                    .register(meterRegistry)
                    .increment();
        }

        log.debug("记录解析规则执行: type={}, duration={}ms, success={}", ruleType, durationMillis, success);
    }

    /**
     * 开始计时
     *
     * @return 计时样本
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 结束计时并记录解析规则执行时间
     *
     * @param sample   计时样本
     * @param ruleType 规则类型
     * @param success  是否成功
     */
    public void stopParseTimer(Timer.Sample sample, String ruleType, boolean success) {
        long durationNanos = sample.stop(Timer.builder(PREFIX + "parse.rule.execution.time")
                .tag("rule_type", ruleType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));

        long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        if (success) {
            parseSuccessCounter.increment();
        } else {
            parseFailureCounter.increment();
        }

        log.debug("解析规则执行完成: type={}, duration={}ms, success={}", ruleType, durationMillis, success);
    }

    // ==================== 转发指标方法 ====================

    /**
     * 记录转发成功（发送到 Kafka 成功）
     *
     * @param tenantId  租户ID
     * @param deviceId  设备ID
     * @param eventType 事件类型
     */
    public void recordForwardSuccess(String tenantId, String deviceId, String eventType) {
        forwardSuccessCounter.increment();

        // 按租户和事件类型分类统计
        Counter.builder(PREFIX + "forward.success.count")
                .tag("tenant_id", tenantId)
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();

        log.debug("记录转发成功: tenant={}, device={}, eventType={}", tenantId, deviceId, eventType);
    }

    /**
     * 记录转发失败（发送到 Kafka 失败）
     *
     * @param tenantId  租户ID
     * @param deviceId  设备ID
     * @param eventType 事件类型
     * @param errorType 错误类型
     */
    public void recordForwardFailure(String tenantId, String deviceId, String eventType, String errorType) {
        forwardFailureCounter.increment();

        // 按租户、事件类型和错误类型分类统计
        Counter.builder(PREFIX + "forward.failure.count")
                .tag("tenant_id", tenantId)
                .tag("event_type", eventType)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();

        log.warn("记录转发失败: tenant={}, device={}, eventType={}, errorType={}",
                tenantId, deviceId, eventType, errorType);
    }
}
