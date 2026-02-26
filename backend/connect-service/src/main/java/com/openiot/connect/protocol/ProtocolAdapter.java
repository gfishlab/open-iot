package com.openiot.connect.protocol;

import com.openiot.common.kafka.model.EventEnvelope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

/**
 * 协议适配器接口
 * 用于解析不同协议的设备数据
 */
public interface ProtocolAdapter {

    /**
     * 解析消息
     */
    ParseResult parse(String message);

    /**
     * 发送到 Kafka
     */
    void sendToKafka(ParseResult result);

    /**
     * 解析结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ParseResult {
        private boolean success;
        private String error;
        private String deviceToken;
        private String tenantId;
        private String deviceId;
        private String eventType;
        private Object payload;
        private String rawPayload;

        public static ParseResult success(String deviceToken, String tenantId, String deviceId,
                                          String eventType, Object payload, String rawPayload) {
            return ParseResult.builder()
                    .success(true)
                    .deviceToken(deviceToken)
                    .tenantId(tenantId)
                    .deviceId(deviceId)
                    .eventType(eventType)
                    .payload(payload)
                    .rawPayload(rawPayload)
                    .build();
        }

        public static ParseResult fail(String error) {
            return ParseResult.builder()
                    .success(false)
                    .error(error)
                    .build();
        }
    }
}
