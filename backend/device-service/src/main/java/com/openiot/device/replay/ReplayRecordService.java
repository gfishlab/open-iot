package com.openiot.device.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 重放记录服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayRecordService {

    private final ObjectMapper objectMapper;

    // TODO: 实际应该使用数据库存储，这里使用内存简化
    private final List<DeadLetterMessage> deadLetterStore = new ArrayList<>();

    /**
     * 保存死信消息
     *
     * @param deadLetter 死信消息
     */
    public void saveDeadLetter(DeadLetterMessage deadLetter) {
        deadLetterStore.add(deadLetter);
        log.debug("保存死信消息: messageId={}, topic={}",
                deadLetter.getOriginalMessageId(), deadLetter.getOriginalTopic());
    }

    /**
     * 查询死信消息
     *
     * @param topic         原始主题
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param retryableOnly 仅可重试
     * @return 死信消息列表
     */
    public List<DeadLetterMessage> queryDeadLetters(String topic, LocalDateTime startTime,
                                                     LocalDateTime endTime, Boolean retryableOnly) {
        return deadLetterStore.stream()
                .filter(msg -> topic == null || topic.equals(msg.getOriginalTopic()))
                .filter(msg -> {
                    if (startTime == null && endTime == null) {
                        return true;
                    }
                    long msgTime = msg.getTimestamp() != null ? msg.getTimestamp() : 0;
                    long start = startTime != null ? toEpochMilli(startTime) : 0;
                    long end = endTime != null ? toEpochMilli(endTime) : Long.MAX_VALUE;
                    return msgTime >= start && msgTime <= end;
                })
                .filter(msg -> retryableOnly == null || !retryableOnly || Boolean.TRUE.equals(msg.getRetryable()))
                .toList();
    }

    /**
     * 更新重放状态
     *
     * @param messageId 消息ID
     * @param status    状态
     * @param taskId    任务ID
     */
    public void updateReplayStatus(String messageId, String status, String taskId) {
        deadLetterStore.stream()
                .filter(msg -> messageId.equals(msg.getOriginalMessageId()))
                .findFirst()
                .ifPresent(msg -> {
                    // 更新状态（实际应该更新数据库字段）
                    log.debug("更新重放状态: messageId={}, status={}, taskId={}",
                            messageId, status, taskId);
                });
    }

    /**
     * 查询死信统计
     *
     * @return 统计信息
     */
    public DeadLetterStatistics getStatistics() {
        DeadLetterStatistics stats = new DeadLetterStatistics();
        stats.setTotalCount(deadLetterStore.size());
        stats.setRetryableCount((int) deadLetterStore.stream()
                .filter(msg -> Boolean.TRUE.equals(msg.getRetryable()))
                .count());
        stats.setNonRetryableCount(stats.getTotalCount() - stats.getRetryableCount());

        // 按主题统计
        stats.setTopicCount(deadLetterStore.stream()
                .map(DeadLetterMessage::getOriginalTopic)
                .distinct()
                .toList()
                .size());

        return stats;
    }

    /**
     * 清理过期的死信消息
     *
     * @param beforeTime 清理此时间之前的消息
     * @return 清理数量
     */
    public int cleanupExpired(LocalDateTime beforeTime) {
        long before = toEpochMilli(beforeTime);
        int originalSize = deadLetterStore.size();
        deadLetterStore.removeIf(msg -> {
            long msgTime = msg.getTimestamp() != null ? msg.getTimestamp() : 0;
            return msgTime < before;
        });
        int removed = originalSize - deadLetterStore.size();
        log.info("清理过期死信消息: before={}, removed={}", beforeTime, removed);
        return removed;
    }

    private long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 死信统计
     */
    @lombok.Data
    public static class DeadLetterStatistics {
        private Integer totalCount;
        private Integer retryableCount;
        private Integer nonRetryableCount;
        private Integer topicCount;
    }
}
