package com.openiot.connect.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.kafka.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 私有协议解析器
 * 协议格式: TOKEN|JSON_DATA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateProtocolParser implements ProtocolAdapter {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEVICE_EVENTS_TOPIC = "device-events";

    @Override
    public ParseResult parse(String message) {
        try {
            // 解析格式: TOKEN|JSON_DATA
            int separatorIndex = message.indexOf('|');
            if (separatorIndex < 0) {
                return ParseResult.fail("无效的协议格式");
            }

            String token = message.substring(0, separatorIndex);
            String jsonData = message.substring(separatorIndex + 1);

            // 解析 JSON
            JsonNode json = objectMapper.readTree(jsonData);

            String tenantId = json.has("tenantId") ? json.get("tenantId").asText() : null;
            String deviceId = json.has("deviceId") ? json.get("deviceId").asText() : null;
            String eventType = json.has("eventType") ? json.get("eventType").asText() : "TELEMETRY";
            Object payload = json.has("payload") ? json.get("payload") : json;

            if (tenantId == null || deviceId == null) {
                return ParseResult.fail("缺少 tenantId 或 deviceId");
            }

            return ParseResult.success(
                    token,
                    tenantId,
                    deviceId,
                    eventType,
                    payload,
                    Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8))
            );

        } catch (Exception e) {
            log.error("解析消息失败: {}", message, e);
            return ParseResult.fail("解析异常: " + e.getMessage());
        }
    }

    @Override
    public void sendToKafka(ParseResult result) {
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(result.getTenantId())
                .deviceId(result.getDeviceId())
                .eventType(result.getEventType())
                .protocol(EventEnvelope.PROTOCOL_TCP)
                .payload(result.getPayload())
                .rawPayload(result.getRawPayload())
                .timestamp(System.currentTimeMillis())
                .traceId(UUID.randomUUID().toString().substring(0, 16))
                .build();

        kafkaTemplate.send(DEVICE_EVENTS_TOPIC, envelope.getKafkaKey(), envelope);
        log.info("TCP数据发送到Kafka: deviceId={}, eventType={}", result.getDeviceId(), result.getEventType());
    }
}
