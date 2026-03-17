package com.openiot.connect.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.kafka.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备影子 Delta 消费者
 * <p>
 * 监听 device-events 主题中的 SHADOW_DELTA 事件，
 * 将 desired 与 reported 的差异（delta）通过 MQTT 下发给设备，
 * 通知设备更新属性以达到期望状态。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowDeltaConsumer {

    private final MqttClientManager mqttClientManager;
    private final ObjectMapper objectMapper;

    /**
     * 消费影子 Delta 事件
     * <p>
     * 过滤 eventType == SHADOW_DELTA 的事件，提取 delta 数据，
     * 通过 MQTT 发布到 /devices/{deviceKey}/shadow/update 主题。
     * </p>
     *
     * @param record Kafka 消息记录（EventEnvelope 格式）
     * @param ack    手动确认句柄
     */
    @KafkaListener(topics = "device-events", groupId = "connect-shadow")
    public void consumeShadowDelta(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();

        // 仅处理 SHADOW_DELTA 类型事件，其余跳过
        if (!EventEnvelope.EVENT_TYPE_SHADOW_DELTA.equals(event.getEventType())) {
            ack.acknowledge();
            return;
        }

        // EventEnvelope 中的 deviceId 实际为 deviceKey（MQTT clientId）
        String deviceKey = event.getDeviceId();
        log.info("[Shadow] 收到影子 Delta 事件: deviceKey={}, eventId={}", deviceKey, event.getEventId());

        try {
            // 构建下发给设备的 delta payload
            Map<String, Object> deltaPayload = buildDeltaPayload(event);

            // 序列化为 JSON 字符串
            String payloadJson = objectMapper.writeValueAsString(deltaPayload);

            // 构建 MQTT 主题: /devices/{deviceKey}/shadow/update
            String topic = String.format("/devices/%s/shadow/update", deviceKey);

            // 通过 MQTT 下发影子 delta（QoS 1 确保至少送达一次）
            mqttClientManager.publish(topic, payloadJson, 1)
                    .thenAccept(v -> log.info("[Shadow] 影子 Delta 已下发: deviceKey={}, topic={}", deviceKey, topic))
                    .exceptionally(throwable -> {
                        log.error("[Shadow] 影子 Delta 下发失败: deviceKey={}, error={}", deviceKey, throwable.getMessage());
                        return null;
                    });

            // 确认消息消费
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[Shadow] 处理影子 Delta 事件异常: deviceKey={}, eventId={}", deviceKey, event.getEventId(), e);
            // 不确认，触发 Kafka 重试
        }
    }

    /**
     * 构建下发给设备的 delta payload
     * <p>
     * 将 EventEnvelope 中的 payload（delta 差异数据）包装为标准格式，
     * 包含 delta 内容、版本号、时间戳等元数据。
     * </p>
     *
     * @param event 事件信封
     * @return delta payload
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDeltaPayload(EventEnvelope event) {
        Map<String, Object> payload = new LinkedHashMap<>();

        Object eventPayload = event.getPayload();
        if (eventPayload instanceof Map) {
            Map<String, Object> payloadMap = (Map<String, Object>) eventPayload;

            // delta 差异数据（desired 与 reported 的差值）
            payload.put("state", payloadMap.getOrDefault("delta", payloadMap));
            // 影子版本号（用于设备端乐观锁校验）
            if (payloadMap.containsKey("version")) {
                payload.put("version", payloadMap.get("version"));
            }
        } else {
            // payload 非 Map 类型时，直接作为 state 传递
            payload.put("state", eventPayload);
        }

        // 附加事件元数据
        payload.put("eventId", event.getEventId());
        payload.put("timestamp", event.getTimestamp());

        return payload;
    }
}
