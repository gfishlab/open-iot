package com.openiot.data.consumer;

import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.data.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 * 消费解析失败的消息，存入 MongoDB 死信集合
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final DeadLetterService deadLetterService;

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 消费死信队列消息
     * 监听 dlq-parse-failed topic
     */
    @KafkaListener(
            topics = "dlq-parse-failed",
            groupId = "dlq-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();
        log.warn("收到死信消息: eventId={}, deviceId={}", event.getEventId(), event.getDeviceId());

        try {
            // 提取失败原因（从 header 中获取，如果没有则使用默认值）
            String failureReason = extractFailureReason(record);

            // 存入死信集合
            deadLetterService.createDeadLetter(
                    event.getEventId(),
                    event.getTenantId(),
                    event.getDeviceId(),
                    event.getRawPayload(),
                    failureReason
            );

            // 确认消息
            ack.acknowledge();

            log.info("死信消息已记录: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("处理死信消息异常: eventId={}", event.getEventId(), e);
            // 不确认，触发 Kafka 重试
        }
    }

    /**
     * 从 Kafka header 中提取失败原因
     */
    private String extractFailureReason(ConsumerRecord<String, EventEnvelope> record) {
        var headers = record.headers();
        var reasonHeader = headers.lastHeader("failure-reason");
        if (reasonHeader != null) {
            return new String(reasonHeader.value());
        }
        return "未知错误";
    }
}
