package com.openiot.device.replay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 死信队列消息
 *
 * @author OpenIoT Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterMessage {

    /**
     * 原始消息ID
     */
    private String originalMessageId;

    /**
     * 原始主题
     */
    private String originalTopic;

    /**
     * 原始消息内容
     */
    private String originalMessage;

    /**
     * 死信原因
     */
    private String reason;

    /**
     * 异常类型
     */
    private String exceptionType;

    /**
     * 异常消息
     */
    private String exceptionMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 是否可重试
     */
    private Boolean retryable;

    /**
     * 发生时间
     */
    private Long timestamp;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 扩展信息（JSON）
     */
    private String extensions;

    /**
     * 创建可重试的死信消息
     */
    public static DeadLetterMessage retryable(String originalMessageId, String originalTopic,
                                               String originalMessage, Exception exception) {
        return DeadLetterMessage.builder()
                .originalMessageId(originalMessageId)
                .originalTopic(originalTopic)
                .originalMessage(originalMessage)
                .reason(exception.getClass().getSimpleName())
                .exceptionType(exception.getClass().getName())
                .exceptionMessage(exception.getMessage())
                .retryCount(0)
                .maxRetryCount(3)
                .retryable(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建不可重试的死信消息
     */
    public static DeadLetterMessage nonRetryable(String originalMessageId, String originalTopic,
                                                  String originalMessage, Exception exception) {
        return DeadLetterMessage.builder()
                .originalMessageId(originalMessageId)
                .originalTopic(originalTopic)
                .originalMessage(originalMessage)
                .reason(exception.getClass().getSimpleName())
                .exceptionType(exception.getClass().getName())
                .exceptionMessage(exception.getMessage())
                .retryCount(0)
                .maxRetryCount(0)
                .retryable(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    /**
     * 是否可以继续重试
     */
    public boolean canRetry() {
        return Boolean.TRUE.equals(retryable) &&
                retryCount != null &&
                maxRetryCount != null &&
                retryCount < maxRetryCount;
    }
}
