package com.openiot.device.replay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据重放服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReplayRecordService replayRecordService;

    /**
     * 重放任务状态
     */
    private final Map<String, ReplayTask> runningTasks = new ConcurrentHashMap<>();

    /**
     * 创建重放任务
     *
     * @param request 重放请求
     * @return 重放任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createReplayTask(ReplayRequest request) {
        log.info("创建重放任务: topic={}, startTime={}, endTime={}",
                request.getTopic(), request.getStartTime(), request.getEndTime());

        // 生成任务ID
        String taskId = UUID.randomUUID().toString();

        // 查询可重放的消息
        List<DeadLetterMessage> messages = replayRecordService.queryDeadLetters(
                request.getTopic(),
                request.getStartTime(),
                request.getEndTime(),
                request.getRetryableOnly()
        );

        log.info("查询到可重放消息: count={}", messages.size());

        if (messages.isEmpty()) {
            return taskId;
        }

        // 创建重放任务
        ReplayTask task = new ReplayTask();
        task.setTaskId(taskId);
        task.setTopic(request.getTopic());
        task.setTotalCount(messages.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setStatus("running");
        task.setCreateTime(LocalDateTime.now());

        runningTasks.put(taskId, task);

        // 异步执行重放
        executeReplay(taskId, messages, request.getBatchSize());

        return taskId;
    }

    /**
     * 执行重放任务
     */
    private void executeReplay(String taskId, List<DeadLetterMessage> messages, int batchSize) {
        new Thread(() -> {
            ReplayTask task = runningTasks.get(taskId);
            int size = batchSize > 0 ? batchSize : 10;

            try {
                // 分批重放
                for (int i = 0; i < messages.size(); i += size) {
                    int end = Math.min(i + size, messages.size());
                    List<DeadLetterMessage> batch = messages.subList(i, end);

                    for (DeadLetterMessage message : batch) {
                        try {
                            // 重放消息到原始主题
                            kafkaTemplate.send(message.getOriginalTopic(), message.getOriginalMessage());

                            // 更新死信记录状态
                            replayRecordService.updateReplayStatus(
                                    message.getOriginalMessageId(),
                                    "replayed",
                                    taskId
                            );

                            task.incrementSuccessCount();

                        } catch (Exception e) {
                            log.error("重放消息失败: messageId={}, error={}",
                                    message.getOriginalMessageId(), e.getMessage());

                            replayRecordService.updateReplayStatus(
                                    message.getOriginalMessageId(),
                                    "replay_failed",
                                    taskId
                            );

                            task.incrementFailCount();
                        }
                    }

                    // 更新进度
                    task.setProgress((int) ((double) end / messages.size() * 100));

                    // 短暂休眠，避免压垮 Kafka
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 任务完成
                task.setStatus("completed");
                task.setCompleteTime(LocalDateTime.now());

                log.info("重放任务完成: taskId={}, success={}, fail={}",
                        taskId, task.getSuccessCount(), task.getFailCount());

            } catch (Exception e) {
                log.error("重放任务异常: taskId={}, error={}", taskId, e.getMessage(), e);
                task.setStatus("failed");
                task.setError(e.getMessage());
            }
        }, "replay-task-" + taskId).start();
    }

    /**
     * 查询重放任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public ReplayTask getTaskStatus(String taskId) {
        return runningTasks.get(taskId);
    }

    /**
     * 停止重放任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean stopTask(String taskId) {
        ReplayTask task = runningTasks.get(taskId);
        if (task == null) {
            return false;
        }

        task.setStatus("stopped");
        task.setCompleteTime(LocalDateTime.now());

        log.info("停止重放任务: taskId={}", taskId);
        return true;
    }

    /**
     * 查询运行中的任务列表
     *
     * @return 任务列表
     */
    public List<ReplayTask> getRunningTasks() {
        return runningTasks.values().stream()
                .filter(t -> "running".equals(t.getStatus()))
                .toList();
    }

    /**
     * 清理已完成的任务
     */
    public void cleanupCompletedTasks() {
        runningTasks.entrySet().removeIf(entry -> {
            ReplayTask task = entry.getValue();
            return "completed".equals(task.getStatus()) ||
                   "stopped".equals(task.getStatus()) ||
                   "failed".equals(task.getStatus());
        });

        log.info("清理已完成的重放任务");
    }

    /**
     * 重放请求
     */
    @Data
    public static class ReplayRequest {
        private String topic;           // 原始主题
        private LocalDateTime startTime; // 开始时间
        private LocalDateTime endTime;   // 结束时间
        private Integer batchSize;       // 批量大小
        private Boolean retryableOnly;   // 仅可重试
    }

    /**
     * 重放任务
     */
    @Data
    public static class ReplayTask {
        private String taskId;
        private String topic;
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private Integer progress;
        private String status;  // running, completed, stopped, failed
        private String error;
        private LocalDateTime createTime;
        private LocalDateTime completeTime;

        void incrementSuccessCount() {
            this.successCount = (this.successCount == null ? 0 : this.successCount) + 1;
            updateProgress();
        }

        void incrementFailCount() {
            this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
            updateProgress();
        }

        void setProgress(Integer progress) {
            this.progress = progress;
            updateProgress();
        }

        private void updateProgress() {
            if (totalCount != null && totalCount > 0) {
                int processed = (successCount == null ? 0 : successCount) +
                               (failCount == null ? 0 : failCount);
                this.progress = (int) ((double) processed / totalCount * 100);
            }
        }
    }
}
