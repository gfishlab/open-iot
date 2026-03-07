package com.openiot.device.replay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 重放记录服务单元测试
 *
 * @author OpenIoT Team
 */
@DisplayName("重放记录服务测试")
class ReplayRecordServiceTest {

    private ReplayRecordService service;

    @BeforeEach
    void setUp() {
        service = new ReplayRecordService(null);
    }

    @Test
    @DisplayName("保存死信消息 - 成功")
    void saveDeadLetter_Success() {
        // Given
        DeadLetterMessage message = DeadLetterMessage.builder()
                .originalMessageId("msg-001")
                .originalTopic("device-data")
                .originalMessage("{\"data\":\"test\"}")
                .exceptionMessage("Test error")
                .exceptionType("java.lang.Exception")
                .retryCount(0)
                .maxRetryCount(3)
                .retryable(true)
                .timestamp(System.currentTimeMillis())
                .build();

        // When
        service.saveDeadLetter(message);

        // Then
        assertThat(service.getStatistics().getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("查询死信消息 - 按主题过滤")
    void queryDeadLetters_FilterByTopic() {
        // Given
        DeadLetterMessage msg1 = createMessage("msg-001", "device-data");
        DeadLetterMessage msg2 = createMessage("msg-002", "device-status");
        service.saveDeadLetter(msg1);
        service.saveDeadLetter(msg2);

        // When
        var results = service.queryDeadLetters("device-data", null, null, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalMessageId()).isEqualTo("msg-001");
    }

    @Test
    @DisplayName("查询死信消息 - 仅可重试")
    void queryDeadLetters_RetryableOnly() {
        // Given
        DeadLetterMessage msg1 = createMessage("msg-001", "device-data");
        msg1.setRetryable(true);
        DeadLetterMessage msg2 = createMessage("msg-002", "device-data");
        msg2.setRetryable(false);
        service.saveDeadLetter(msg1);
        service.saveDeadLetter(msg2);

        // When
        var results = service.queryDeadLetters(null, null, null, true);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalMessageId()).isEqualTo("msg-001");
    }

    @Test
    @DisplayName("查询死信统计")
    void getStatistics_Success() {
        // Given
        service.saveDeadLetter(createMessage("msg-001", "device-data"));
        service.saveDeadLetter(createMessage("msg-002", "device-status"));
        service.saveDeadLetter(createMessage("msg-003", "device-data"));

        // When
        var stats = service.getStatistics();

        // Then
        assertThat(stats.getTotalCount()).isEqualTo(3);
        assertThat(stats.getTopicCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("清理过期死信消息")
    void cleanupExpired_Success() {
        // Given
        DeadLetterMessage oldMsg = createMessage("old-msg", "device-data");
        oldMsg.setTimestamp(System.currentTimeMillis() - 86400000L); // 1天前

        DeadLetterMessage newMsg = createMessage("new-msg", "device-data");
        newMsg.setTimestamp(System.currentTimeMillis());

        service.saveDeadLetter(oldMsg);
        service.saveDeadLetter(newMsg);

        // When
        int cleaned = service.cleanupExpired(LocalDateTime.now().minusHours(12));

        // Then
        assertThat(cleaned).isEqualTo(1);
        assertThat(service.getStatistics().getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("更新重放状态")
    void updateReplayStatus_Success() {
        // Given
        DeadLetterMessage message = createMessage("msg-001", "device-data");
        service.saveDeadLetter(message);

        // When
        service.updateReplayStatus("msg-001", "replayed", "task-001");

        // Then
        // 验证不抛出异常
        assertThat(service.getStatistics().getTotalCount()).isEqualTo(1);
    }

    private DeadLetterMessage createMessage(String id, String topic) {
        return DeadLetterMessage.builder()
                .originalMessageId(id)
                .originalTopic(topic)
                .originalMessage("{\"data\":\"test\"}")
                .exceptionMessage("Test error")
                .exceptionType("java.lang.Exception")
                .retryCount(0)
                .maxRetryCount(3)
                .retryable(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
