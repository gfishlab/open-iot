package com.openiot.device.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE 连接管理器
 *
 * @author OpenIoT Team
 */
@Slf4j
@Component
public class SseEmitterManager {

    /**
     * 存储所有 SSE 连接
     * Key: clientId
     * Value: SseEmitter
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 存储连接元数据
     * Key: clientId
     * Value: ConnectionMetadata
     */
    private final Map<String, ConnectionMetadata> metadataMap = new ConcurrentHashMap<>();

    /**
     * 心跳定时器（每30秒发送一次心跳）
     */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);

    public SseEmitterManager() {
        // 启动心跳任务
        startHeartbeat();
    }

    /**
     * 创建 SSE 连接
     *
     * @param clientId 客户端ID
     * @param timeout  超时时间（毫秒）
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String clientId, Long timeout) {
        // 如果 clientId 为空，则自动生成一个唯一的客户端ID
        final String actualClientId;
        if (clientId == null || clientId.isEmpty()) {
            actualClientId = "auto-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        } else {
            actualClientId = clientId;
        }

        log.info("创建 SSE 连接: clientId={}, timeout={}ms", actualClientId, timeout);

        // 设置超时时间（默认30分钟）
        Long actualTimeout = timeout != null ? timeout : 30 * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(actualTimeout);

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: clientId={}", actualClientId);
            removeEmitter(actualClientId);
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: clientId={}", actualClientId);
            removeEmitter(actualClientId);
        });

        // 设置错误回调
        emitter.onError(throwable -> {
            log.error("SSE 连接错误: clientId={}, error={}", actualClientId, throwable.getMessage());
            removeEmitter(actualClientId);
        });

        emitters.put(actualClientId, emitter);

        // 保存元数据
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setClientId(actualClientId);
        metadata.setConnectTime(System.currentTimeMillis());
        metadataMap.put(actualClientId, metadata);

        // 发送连接成功消息
        sendMessage(actualClientId, SseMessage.builder()
                .type("connected")
                .data(Map.of("clientId", actualClientId, "timestamp", System.currentTimeMillis()))
                .build());

        return emitter;
    }

    /**
     * 发送消息给指定客户端
     *
     * @param clientId 客户端ID
     * @param message  消息内容
     * @return 是否发送成功
     */
    public boolean sendMessage(String clientId, SseMessage message) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            log.warn("客户端连接不存在: clientId={}", clientId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(message.getType())
                    .data(message.getData())
                    .id(message.getId()));
            return true;
        } catch (IOException e) {
            log.error("发送消息失败: clientId={}, error={}", clientId, e.getMessage());
            removeEmitter(clientId);
            return false;
        }
    }

    /**
     * 广播消息给所有客户端
     *
     * @param message 消息内容
     * @return 成功发送的客户端数量
     */
    public int broadcast(SseMessage message) {
        int successCount = 0;
        for (String clientId : emitters.keySet()) {
            if (sendMessage(clientId, message)) {
                successCount++;
            }
        }
        log.debug("广播消息: type={}, clients={}", message.getType(), successCount);
        return successCount;
    }

    /**
     * 发送设备状态变化消息
     *
     * @param deviceId 设备ID
     * @param status   新状态
     */
    public void sendDeviceStatusChange(Long deviceId, String status) {
        SseMessage message = SseMessage.builder()
                .type("device_status_change")
                .data(Map.of(
                        "deviceId", deviceId,
                        "status", status,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();

        broadcast(message);
    }

    /**
     * 发送设备属性变化消息
     *
     * @param deviceId           设备ID
     * @param propertyIdentifier 属性标识符
     * @param value              新值
     */
    public void sendDevicePropertyChange(Long deviceId, String propertyIdentifier, Object value) {
        SseMessage message = SseMessage.builder()
                .type("device_property_change")
                .data(Map.of(
                        "deviceId", deviceId,
                        "propertyIdentifier", propertyIdentifier,
                        "value", value,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();

        broadcast(message);
    }

    /**
     * 发送设备事件消息
     *
     * @param deviceId           设备ID
     * @param eventIdentifier    事件标识符
     * @param eventType          事件类型
     * @param outputParams       输出参数
     */
    public void sendDeviceEvent(Long deviceId, String eventIdentifier, String eventType, Map<String, Object> outputParams) {
        SseMessage message = SseMessage.builder()
                .type("device_event")
                .data(Map.of(
                        "deviceId", deviceId,
                        "eventIdentifier", eventIdentifier,
                        "eventType", eventType,
                        "outputParams", outputParams,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();

        broadcast(message);
    }

    /**
     * 发送告警消息
     *
     * @param alertId    告警ID
     * @param deviceId   设备ID
     * @param alertLevel 告警级别
     * @param alertTitle 告警标题
     */
    public void sendAlert(Long alertId, Long deviceId, String alertLevel, String alertTitle) {
        SseMessage message = SseMessage.builder()
                .type("alert")
                .data(Map.of(
                        "alertId", alertId,
                        "deviceId", deviceId,
                        "alertLevel", alertLevel,
                        "alertTitle", alertTitle,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();

        broadcast(message);
    }

    /**
     * 移除客户端连接
     *
     * @param clientId 客户端ID
     */
    public void removeEmitter(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("完成连接失败: clientId={}", clientId, e);
            }
        }
        metadataMap.remove(clientId);
        log.info("移除 SSE 连接: clientId={}", clientId);
    }

    /**
     * 获取在线连接数
     *
     * @return 在线连接数
     */
    public int getOnlineCount() {
        return emitters.size();
    }

    /**
     * 检查客户端是否在线
     *
     * @param clientId 客户端ID
     * @return 是否在线
     */
    public boolean isOnline(String clientId) {
        return emitters.containsKey(clientId);
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            int count = broadcast(SseMessage.builder()
                    .type("heartbeat")
                    .data(Map.of("timestamp", System.currentTimeMillis()))
                    .build());
            log.debug("心跳发送完成: online={}", count);
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        log.info("关闭所有 SSE 连接: count={}", emitters.size());
        emitters.keySet().forEach(this::removeEmitter);
        heartbeatScheduler.shutdown();
    }

    /**
     * 连接元数据
     */
    @lombok.Data
    public static class ConnectionMetadata {
        private String clientId;
        private Long connectTime;
        private Long lastHeartbeatTime;
        private String userId;
        private String tenantId;
    }
}
