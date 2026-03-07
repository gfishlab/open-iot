package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.replay.DeadLetterMessage;
import com.openiot.device.replay.ReplayRecordService;
import com.openiot.device.replay.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据重放控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/replay")
@RequiredArgsConstructor
@Tag(name = "数据重放", description = "死信队列重放和数据恢复接口")
public class ReplayController {

    private final ReplayService replayService;
    private final ReplayRecordService replayRecordService;

    /**
     * 创建重放任务
     */
    @PostMapping("/tasks")
    @Operation(summary = "创建重放任务", description = "创建死信消息重放任务")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody CreateTaskRequest request) {
        log.info("创建重放任务: {}", request);

        ReplayService.ReplayRequest replayRequest = new ReplayService.ReplayRequest();
        replayRequest.setTopic(request.getTopic());
        replayRequest.setStartTime(request.getStartTime());
        replayRequest.setEndTime(request.getEndTime());
        replayRequest.setBatchSize(request.getBatchSize());
        replayRequest.setRetryableOnly(request.getRetryableOnly());

        String taskId = replayService.createReplayTask(replayRequest);

        return ApiResponse.success(Map.of(
                "taskId", taskId,
                "message", "重放任务已创建"
        ));
    }

    /**
     * 查询重放任务状态
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态", description = "查询重放任务的执行状态")
    public ApiResponse<ReplayService.ReplayTask> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        ReplayService.ReplayTask task = replayService.getTaskStatus(taskId);
        if (task == null) {
            return ApiResponse.error("任务不存在", null);
        }

        return ApiResponse.success(task);
    }

    /**
     * 停止重放任务
     */
    @PostMapping("/tasks/{taskId}/stop")
    @Operation(summary = "停止任务", description = "停止正在执行的重放任务")
    public ApiResponse<Void> stopTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        boolean success = replayService.stopTask(taskId);
        if (success) {
            return ApiResponse.success("任务已停止", null);
        } else {
            return ApiResponse.error("任务不存在或无法停止", null);
        }
    }

    /**
     * 查询运行中的任务
     */
    @GetMapping("/tasks/running")
    @Operation(summary = "查询运行中任务", description = "查询所有正在执行的重放任务")
    public ApiResponse<List<ReplayService.ReplayTask>> getRunningTasks() {
        List<ReplayService.ReplayTask> tasks = replayService.getRunningTasks();
        return ApiResponse.success(tasks);
    }

    /**
     * 查询死信消息列表
     */
    @GetMapping("/dead-letters")
    @Operation(summary = "查询死信消息", description = "查询死信队列中的消息")
    public ApiResponse<List<DeadLetterMessage>> getDeadLetters(
            @Parameter(description = "原始主题") @RequestParam(required = false) String topic,
            @Parameter(description = "开始时间") @RequestParam(required = false) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) LocalDateTime endTime,
            @Parameter(description = "仅可重试") @RequestParam(required = false, defaultValue = "false") Boolean retryableOnly) {

        List<DeadLetterMessage> messages = replayRecordService.queryDeadLetters(
                topic, startTime, endTime, retryableOnly);

        return ApiResponse.success(messages);
    }

    /**
     * 查询死信统计
     */
    @GetMapping("/dead-letters/statistics")
    @Operation(summary = "查询死信统计", description = "查询死信队列的统计信息")
    public ApiResponse<ReplayRecordService.DeadLetterStatistics> getStatistics() {
        ReplayRecordService.DeadLetterStatistics stats = replayRecordService.getStatistics();
        return ApiResponse.success(stats);
    }

    /**
     * 清理过期死信
     */
    @DeleteMapping("/dead-letters/cleanup")
    @Operation(summary = "清理过期死信", description = "清理指定时间之前的死信消息")
    public ApiResponse<Map<String, Integer>> cleanup(
            @Parameter(description = "清理此时间之前的消息") @RequestParam LocalDateTime beforeTime) {

        int count = replayRecordService.cleanupExpired(beforeTime);
        return ApiResponse.success(Map.of("cleanedCount", count));
    }

    /**
     * 清理已完成任务
     */
    @PostMapping("/tasks/cleanup")
    @Operation(summary = "清理已完成任务", description = "清理已完成的重放任务记录")
    public ApiResponse<Void> cleanupTasks() {
        replayService.cleanupCompletedTasks();
        return ApiResponse.success("任务记录已清理", null);
    }

    /**
     * 创建任务请求
     */
    @Data
    public static class CreateTaskRequest {
        /**
         * 原始主题
         */
        private String topic;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 结束时间
         */
        private LocalDateTime endTime;

        /**
         * 批量大小（默认10）
         */
        private Integer batchSize = 10;

        /**
         * 仅重放可重试的消息
         */
        private Boolean retryableOnly = true;
    }
}
