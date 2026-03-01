package com.openiot.data.service;

import com.openiot.common.mongodb.document.DeadLetterDocument;
import com.openiot.common.mongodb.document.RawEventDocument;
import com.openiot.data.parser.TrajectoryParser;
import com.openiot.data.repository.DeadLetterRepository;
import com.openiot.data.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 重试服务
 * 处理死信队列中的失败消息，支持手动重试和自动重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final DeadLetterRepository deadLetterRepository;
    private final RawEventRepository rawEventRepository;
    private final DeadLetterService deadLetterService;
    private final TrajectoryParser trajectoryParser;
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "dlq:retry:";
    private static final int LOCK_WAIT_SECONDS = 3;
    private static final int LOCK_LEASE_SECONDS = 30;
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 重试单条死信记录
     *
     * @param deadLetterId 死信记录 ID
     * @return 是否重试成功
     */
    public boolean retrySingle(String deadLetterId) {
        String lockKey = LOCK_PREFIX + deadLetterId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                return doRetry(deadLetterId);
            } else {
                log.warn("获取重试锁失败: deadLetterId={}", deadLetterId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: deadLetterId={}", deadLetterId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 批量重试待处理的死信记录
     *
     * @return 成功重试的数量
     */
    public int retryPending() {
        List<DeadLetterDocument> pendingList = deadLetterRepository
                .findByStatus(DeadLetterDocument.STATUS_PENDING);

        log.info("待重试死信数量: {}", pendingList.size());

        int successCount = 0;
        for (DeadLetterDocument deadLetter : pendingList) {
            // 跳过超过最大重试次数的记录
            if (deadLetter.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("死信已超过最大重试次数: id={}, retryCount={}",
                        deadLetter.getId(), deadLetter.getRetryCount());
                continue;
            }

            if (retrySingle(deadLetter.getId())) {
                successCount++;
            }
        }

        log.info("批量重试完成: 成功={}/{}", successCount, pendingList.size());
        return successCount;
    }

    /**
     * 执行重试逻辑
     */
    private boolean doRetry(String deadLetterId) {
        return deadLetterRepository.findById(deadLetterId)
                .map(deadLetter -> {
                    // 更新状态为重试中
                    deadLetter.setStatus(DeadLetterDocument.STATUS_RETRYING);
                    deadLetterRepository.save(deadLetter);

                    try {
                        // 查找原始事件
                        boolean success = rawEventRepository
                                .findById(deadLetter.getOriginalEventId())
                                .map(rawEvent -> retryParse(deadLetter, rawEvent))
                                .orElseGet(() -> {
                                    log.warn("原始事件不存在: originalEventId={}",
                                            deadLetter.getOriginalEventId());
                                    deadLetterService.createDeadLetter(
                                            deadLetter.getOriginalEventId(),
                                            deadLetter.getTenantId(),
                                            deadLetter.getDeviceId(),
                                            deadLetter.getRawPayload(),
                                            "原始事件不存在"
                                    );
                                    return false;
                                });

                        return success;

                    } catch (Exception e) {
                        log.error("重试异常: deadLetterId={}", deadLetterId, e);
                        handleRetryFailure(deadLetter, e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    /**
     * 重试解析
     */
    private boolean retryParse(DeadLetterDocument deadLetter, RawEventDocument rawEvent) {
        try {
            // 重新解析并保存
            trajectoryParser.parseAndSave(rawEvent);

            // 标记死信为已解决
            deadLetter.markResolved();
            deadLetterRepository.save(deadLetter);

            log.info("重试解析成功: deadLetterId={}, originalEventId={}",
                    deadLetter.getId(), deadLetter.getOriginalEventId());
            return true;

        } catch (Exception e) {
            log.error("重试解析失败: deadLetterId={}", deadLetter.getId(), e);
            handleRetryFailure(deadLetter, e.getMessage());
            return false;
        }
    }

    /**
     * 处理重试失败
     */
    private void handleRetryFailure(DeadLetterDocument deadLetter, String failureReason) {
        deadLetter.incrementRetryCount();
        deadLetter.setLastRetryTime(LocalDateTime.now());

        if (deadLetter.getRetryCount() >= MAX_RETRY_COUNT) {
            // 超过最大重试次数，保持 PENDING 状态等待人工处理
            deadLetter.setStatus(DeadLetterDocument.STATUS_PENDING);
            deadLetter.setFailureReason(failureReason + " (已达最大重试次数)");
            log.warn("死信达到最大重试次数，需人工处理: id={}", deadLetter.getId());
        } else {
            // 未达到最大重试次数，恢复为 PENDING 等待下次重试
            deadLetter.setStatus(DeadLetterDocument.STATUS_PENDING);
            deadLetter.setFailureReason(failureReason);
        }

        deadLetterRepository.save(deadLetter);
    }

    /**
     * 强制重试（忽略重试次数限制）
     *
     * @param deadLetterId 死信记录 ID
     * @return 是否重试成功
     */
    public boolean forceRetry(String deadLetterId) {
        String lockKey = LOCK_PREFIX + deadLetterId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                // 重置重试次数
                return deadLetterRepository.findById(deadLetterId)
                        .map(deadLetter -> {
                            deadLetter.setRetryCount(0);
                            deadLetter.setStatus(DeadLetterDocument.STATUS_PENDING);
                            deadLetterRepository.save(deadLetter);
                            return doRetry(deadLetterId);
                        })
                        .orElse(false);
            } else {
                log.warn("获取重试锁失败: deadLetterId={}", deadLetterId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: deadLetterId={}", deadLetterId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取待重试的死信列表
     */
    public List<DeadLetterDocument> getPendingDeadLetters() {
        return deadLetterRepository.findByStatus(DeadLetterDocument.STATUS_PENDING);
    }

    /**
     * 获取死信统计信息
     */
    public DeadLetterStats getStats() {
        List<DeadLetterDocument> pending = deadLetterRepository
                .findByStatus(DeadLetterDocument.STATUS_PENDING);
        List<DeadLetterDocument> resolved = deadLetterRepository
                .findByStatus(DeadLetterDocument.STATUS_RESOLVED);

        DeadLetterStats stats = new DeadLetterStats();
        stats.setPendingCount(pending.size());
        stats.setResolvedCount(resolved.size());
        stats.setTotalCount(pending.size() + resolved.size());
        return stats;
    }

    /**
     * 死信统计信息
     */
    @lombok.Data
    public static class DeadLetterStats {
        private int totalCount;
        private int pendingCount;
        private int resolvedCount;
    }
}
