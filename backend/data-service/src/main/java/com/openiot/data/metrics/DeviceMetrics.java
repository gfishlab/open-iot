package com.openiot.data.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 设备监控指标
 * 收集设备连接、消息处理等关键指标
 */
@Slf4j
@Component
public class DeviceMetrics {

    private final MeterRegistry meterRegistry;

    // 指标名称常量
    private static final String METRIC_PREFIX = "openiot.device.";

    // 设备连接数
    private final AtomicLong connectedDevices = new AtomicLong(0);

    // 租户维度设备连接数
    private final ConcurrentMap<String, AtomicLong> tenantDeviceCount = new ConcurrentHashMap<>();

    // 计数器
    private final Counter messagesReceived;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    private final Counter dlqMessages;

    // 计时器
    private final Timer messageProcessTimer;

    public DeviceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 注册 Gauge 指标
        Gauge.builder(METRIC_PREFIX + "connected.count", connectedDevices, AtomicLong::get)
                .description("当前连接的设备数量")
                .register(meterRegistry);

        // 初始化计数器
        this.messagesReceived = Counter.builder(METRIC_PREFIX + "messages.received")
                .description("接收到的消息总数")
                .register(meterRegistry);

        this.messagesProcessed = Counter.builder(METRIC_PREFIX + "messages.processed")
                .description("处理成功的消息总数")
                .register(meterRegistry);

        this.messagesFailed = Counter.builder(METRIC_PREFIX + "messages.failed")
                .description("处理失败的消息总数")
                .register(meterRegistry);

        this.dlqMessages = Counter.builder(METRIC_PREFIX + "dlq.count")
                .description("进入死信队列的消息总数")
                .register(meterRegistry);

        // 初始化计时器
        this.messageProcessTimer = Timer.builder(METRIC_PREFIX + "process.duration")
                .description("消息处理耗时")
                .register(meterRegistry);

        log.info("设备监控指标初始化完成");
    }

    /**
     * 设备上线
     */
    public void deviceConnected(String tenantId, String deviceId) {
        connectedDevices.incrementAndGet();
        incrementTenantDeviceCount(tenantId);

        Counter.builder(METRIC_PREFIX + "connections")
                .tag("tenant", tenantId)
                .tag("event", "connect")
                .description("设备连接事件")
                .register(meterRegistry)
                .increment();

        log.debug("设备上线指标更新: tenant={}, device={}", tenantId, deviceId);
    }

    /**
     * 设备离线
     */
    public void deviceDisconnected(String tenantId, String deviceId) {
        connectedDevices.decrementAndGet();
        decrementTenantDeviceCount(tenantId);

        Counter.builder(METRIC_PREFIX + "connections")
                .tag("tenant", tenantId)
                .tag("event", "disconnect")
                .description("设备断开事件")
                .register(meterRegistry)
                .increment();

        log.debug("设备离线指标更新: tenant={}, device={}", tenantId, deviceId);
    }

    /**
     * 消息接收
     */
    public void messageReceived(String tenantId, String protocol) {
        messagesReceived.increment();

        Counter.builder(METRIC_PREFIX + "messages.by.protocol")
                .tag("tenant", tenantId)
                .tag("protocol", protocol)
                .description("按协议分类的消息数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 消息处理成功
     */
    public void messageProcessed(String tenantId) {
        messagesProcessed.increment();

        Counter.builder(METRIC_PREFIX + "messages.by.tenant")
                .tag("tenant", tenantId)
                .tag("status", "success")
                .description("按租户分类的处理成功消息数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 消息处理失败
     */
    public void messageFailed(String tenantId, String errorType) {
        messagesFailed.increment();

        Counter.builder(METRIC_PREFIX + "errors")
                .tag("tenant", tenantId)
                .tag("type", errorType)
                .description("处理错误数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 死信队列消息
     */
    public void dlqMessage(String tenantId) {
        dlqMessages.increment();
        log.warn("死信队列消息: tenant={}", tenantId);
    }

    /**
     * 记录处理耗时
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 结束计时
     */
    public void stopTimer(Timer.Sample sample, String tenantId) {
        sample.stop(Timer.builder(METRIC_PREFIX + "process.latency")
                .tag("tenant", tenantId)
                .description("消息处理延迟")
                .register(meterRegistry));
    }

    /**
     * 获取当前连接数
     */
    public long getConnectedDeviceCount() {
        return connectedDevices.get();
    }

    // ==================== 私有方法 ====================

    private void incrementTenantDeviceCount(String tenantId) {
        tenantDeviceCount.computeIfAbsent(tenantId, k -> {
            AtomicLong counter = new AtomicLong(0);
            Gauge.builder(METRIC_PREFIX + "tenant.connected", counter, AtomicLong::get)
                    .tag("tenant", tenantId)
                    .description("租户设备连接数")
                    .register(meterRegistry);
            return counter;
        }).incrementAndGet();
    }

    private void decrementTenantDeviceCount(String tenantId) {
        AtomicLong counter = tenantDeviceCount.get(tenantId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
