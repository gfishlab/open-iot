package com.openiot.connect.mqtt;

import com.openiot.common.kafka.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * MQTT Webhook 控制器
 * <p>
 * 接收 EMQX Rule Engine 转发的三类事件：
 * 1. client.connected - 设备上线
 * 2. client.disconnected - 设备离线
 * 3. message.publish - 设备消息发布
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mqtt/webhook")
@RequiredArgsConstructor
public class MqttWebhookController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Kafka Topic 常量 */
    private static final String TOPIC_DEVICE_EVENTS = "device-events";
    private static final String TOPIC_STATUS_CHANGE = "device-status-change";

    /**
     * 设备上线事件回调
     * <p>
     * EMQX Rule Engine 在设备 MQTT 连接成功后触发。
     * clientid = deviceKey, username = productKey
     * </p>
     */
    @PostMapping("/connected")
    public Map<String, Object> onConnected(@RequestBody Map<String, Object> request) {
        String clientId = getString(request, "clientid");
        String username = getString(request, "username");
        String peerHost = getString(request, "peerhost");

        log.info("[MQTT Webhook] 设备上线: clientId={}, username={}, ip={}", clientId, username, peerHost);

        // 构建设备上线事件，发送到 Kafka
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(clientId)
                .eventType(EventEnvelope.EVENT_TYPE_STATUS)
                .protocol(EventEnvelope.PROTOCOL_MQTT)
                .payload(Map.of(
                        "status", "online",
                        "deviceKey", clientId,
                        "productKey", username != null ? username : "",
                        "ip", peerHost != null ? peerHost : ""
                ))
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send(TOPIC_STATUS_CHANGE, envelope.getKafkaKey(), envelope);
        return Map.of("result", "ok");
    }

    /**
     * 设备离线事件回调
     * <p>
     * EMQX Rule Engine 在设备 MQTT 连接断开后触发。
     * </p>
     */
    @PostMapping("/disconnected")
    public Map<String, Object> onDisconnected(@RequestBody Map<String, Object> request) {
        String clientId = getString(request, "clientid");
        String username = getString(request, "username");
        String reason = getString(request, "reason");

        log.info("[MQTT Webhook] 设备离线: clientId={}, username={}, reason={}", clientId, username, reason);

        // 构建设备离线事件，发送到 Kafka
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(clientId)
                .eventType(EventEnvelope.EVENT_TYPE_STATUS)
                .protocol(EventEnvelope.PROTOCOL_MQTT)
                .payload(Map.of(
                        "status", "offline",
                        "deviceKey", clientId,
                        "productKey", username != null ? username : "",
                        "reason", reason != null ? reason : "unknown"
                ))
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send(TOPIC_STATUS_CHANGE, envelope.getKafkaKey(), envelope);
        return Map.of("result", "ok");
    }

    /**
     * 设备消息发布事件回调
     * <p>
     * EMQX Rule Engine 在设备发布 MQTT 消息后触发。
     * 将消息路由到现有的解析管道（Kafka → 解析 → 映射 → 存储）。
     * Topic 格式: /devices/{deviceKey}/properties/report
     * </p>
     */
    @PostMapping("/message")
    public Map<String, Object> onMessage(@RequestBody Map<String, Object> request) {
        String clientId = getString(request, "clientid");
        String username = getString(request, "username");
        String topic = getString(request, "topic");
        String payload = getString(request, "payload");
        Object qosObj = request.get("qos");
        int qos = qosObj instanceof Number ? ((Number) qosObj).intValue() : 0;

        log.info("[MQTT Webhook] 收到消息: clientId={}, topic={}, qos={}, payloadSize={}",
                clientId, topic, qos, payload != null ? payload.length() : 0);

        // 解析 Topic 提取事件类型
        String eventType = resolveEventType(topic);

        // 构建事件信封，发送到 Kafka 进入解析管道
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(clientId)
                .eventType(eventType)
                .protocol(EventEnvelope.PROTOCOL_MQTT)
                .rawPayload(payload)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send(TOPIC_DEVICE_EVENTS, envelope.getKafkaKey(), envelope);
        return Map.of("result", "ok");
    }

    /**
     * 根据 MQTT Topic 解析事件类型
     * <p>
     * Topic 格式: /devices/{deviceKey}/{domain}/{action}
     * - properties/report → TELEMETRY
     * - events/report → ALARM
     * - ota/progress → OTA_PROGRESS
     * - shadow/* → SHADOW_REPORTED
     * - 其他 → TELEMETRY（默认）
     * </p>
     */
    private String resolveEventType(String topic) {
        if (topic == null) {
            return EventEnvelope.EVENT_TYPE_TELEMETRY;
        }

        if (topic.contains("/properties/report")) {
            return EventEnvelope.EVENT_TYPE_TELEMETRY;
        } else if (topic.contains("/events/report")) {
            return EventEnvelope.EVENT_TYPE_ALARM;
        } else if (topic.contains("/ota/progress")) {
            return EventEnvelope.EVENT_TYPE_OTA_PROGRESS;
        } else if (topic.contains("/shadow/")) {
            return EventEnvelope.EVENT_TYPE_SHADOW_REPORTED;
        }

        return EventEnvelope.EVENT_TYPE_TELEMETRY;
    }

    /** 安全提取 String 值 */
    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
