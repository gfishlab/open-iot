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
 * OTA 升级消费者
 * <p>
 * 监听 device-events 主题中的 OTA_UPGRADE 事件，
 * 将固件升级通知通过 MQTT 下发给目标设备。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtaUpgradeConsumer {

    private final MqttClientManager mqttClientManager;
    private final ObjectMapper objectMapper;

    /**
     * 消费 OTA 升级事件
     * <p>
     * 过滤 eventType == OTA_UPGRADE 的事件，提取固件下载地址、版本号、MD5 等信息，
     * 构建 MQTT 主题并下发 OTA 通知给设备。
     * </p>
     *
     * @param record Kafka 消息记录（EventEnvelope 格式）
     * @param ack    手动确认句柄
     */
    @KafkaListener(topics = "device-events", groupId = "connect-ota")
    public void consumeOtaUpgrade(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();

        // 仅处理 OTA_UPGRADE 类型事件，其余跳过
        if (!EventEnvelope.EVENT_TYPE_OTA_UPGRADE.equals(event.getEventType())) {
            ack.acknowledge();
            return;
        }

        // EventEnvelope 中的 deviceId 实际为 deviceKey（MQTT clientId）
        String deviceKey = event.getDeviceId();
        log.info("[OTA] 收到 OTA 升级事件: deviceKey={}, eventId={}", deviceKey, event.getEventId());

        try {
            // 从 payload 中提取 OTA 固件信息
            Map<String, Object> otaPayload = extractOtaPayload(event);

            // 序列化为 JSON 字符串
            String payloadJson = objectMapper.writeValueAsString(otaPayload);

            // 构建 MQTT 主题: /devices/{deviceKey}/ota/notify
            String topic = String.format("/devices/%s/ota/notify", deviceKey);

            // 通过 MQTT 下发 OTA 升级通知（QoS 1 确保至少送达一次）
            mqttClientManager.publish(topic, payloadJson, 1)
                    .thenAccept(v -> log.info("[OTA] OTA 升级通知已下发: deviceKey={}, topic={}", deviceKey, topic))
                    .exceptionally(throwable -> {
                        log.error("[OTA] OTA 升级通知下发失败: deviceKey={}, error={}", deviceKey, throwable.getMessage());
                        return null;
                    });

            // 确认消息消费
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[OTA] 处理 OTA 升级事件异常: deviceKey={}, eventId={}", deviceKey, event.getEventId(), e);
            // 不确认，触发 Kafka 重试
        }
    }

    /**
     * 从 EventEnvelope 的 payload 中提取 OTA 固件信息
     * <p>
     * 提取字段包括：固件下载地址、目标版本、MD5 校验值、文件大小等。
     * </p>
     *
     * @param event 事件信封
     * @return OTA 通知 payload
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractOtaPayload(EventEnvelope event) {
        Map<String, Object> otaNotify = new LinkedHashMap<>();

        Object payload = event.getPayload();
        if (payload instanceof Map) {
            Map<String, Object> payloadMap = (Map<String, Object>) payload;

            // 固件下载地址
            otaNotify.put("url", payloadMap.getOrDefault("url", ""));
            // 目标固件版本
            otaNotify.put("version", payloadMap.getOrDefault("version", ""));
            // 固件 MD5 校验值
            otaNotify.put("md5", payloadMap.getOrDefault("md5", ""));
            // 固件文件大小（字节）
            otaNotify.put("size", payloadMap.getOrDefault("size", 0));
            // 升级描述信息（可选）
            if (payloadMap.containsKey("description")) {
                otaNotify.put("description", payloadMap.get("description"));
            }
            // 升级策略：立即升级 / 空闲升级（可选）
            if (payloadMap.containsKey("upgradePolicy")) {
                otaNotify.put("upgradePolicy", payloadMap.get("upgradePolicy"));
            }
        }

        // 附加事件元数据
        otaNotify.put("eventId", event.getEventId());
        otaNotify.put("timestamp", event.getTimestamp());

        return otaNotify;
    }
}
