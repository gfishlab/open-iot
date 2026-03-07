package com.openiot.device.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 死信队列处理器
 *
 * @author OpenIoT Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ReplayRecordService replayRecordService;

    /**
     * 处理设备数据死信消息
     */
    @DltHandler
    public void handleDeviceDataDlt(
            @Payload String message,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.EXCEPTION_CLASS) String exceptionClass,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("设备数据死信消息: topic={}, offset={}, exception={}, message={}",
                topic, offset, exceptionClass, exceptionMessage);

        handleDeadLetter(message, topic, exceptionMessage, exceptionClass);
    }

    /**
     * 处理设备状态变化死信消息
     */
    @DltHandler
    public void handleDeviceStatusChangeDlt(
            @Payload String message,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.EXCEPTION_CLASS) String exceptionClass,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("设备状态变化死信消息: topic={}, exception={}, message={}",
                topic, exceptionClass, exceptionMessage);

        handleDeadLetter(message, topic, exceptionMessage, exceptionClass);
    }

    /**
     * 处理设备属性变化死信消息
     */
    @DltHandler
    public void handleDevicePropertyChangeDlt(
            @Payload String message,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.EXCEPTION_CLASS) String exceptionClass,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("设备属性变化死信消息: topic={}, exception={}, message={}",
                topic, exceptionClass, exceptionMessage);

        handleDeadLetter(message, topic, exceptionMessage, exceptionClass);
    }

    /**
     * 处理设备事件死信消息
     */
    @DltHandler
    public void handleDeviceEventDlt(
            @Payload String message,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.EXCEPTION_CLASS) String exceptionClass,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("设备事件死信消息: topic={}, exception={}, message={}",
                topic, exceptionClass, exceptionMessage);

        handleDeadLetter(message, topic, exceptionMessage, exceptionClass);
    }

    /**
     * 统一处理死信消息
     */
    private void handleDeadLetter(String message, String topic, String exceptionMessage, String exceptionClass) {
        try {
            // 创建死信消息对象
            DeadLetterMessage deadLetter = DeadLetterMessage.builder()
                    .originalMessageId(java.util.UUID.randomUUID().toString())
                    .originalTopic(topic)
                    .originalMessage(message)
                    .exceptionMessage(exceptionMessage)
                    .exceptionType(exceptionClass)
                    .retryCount(0)
                    .maxRetryCount(3)
                    .retryable(true)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 保存死信记录到数据库
            replayRecordService.saveDeadLetter(deadLetter);

            // 发送到死信主题
            kafkaTemplate.send("dead-letter-queue", objectMapper.writeValueAsString(deadLetter));

            log.info("死信消息已保存并发送到死信队列: originalTopic={}", topic);

        } catch (Exception e) {
            log.error("处理死信消息失败: {}", e.getMessage(), e);
        }
    }
}
