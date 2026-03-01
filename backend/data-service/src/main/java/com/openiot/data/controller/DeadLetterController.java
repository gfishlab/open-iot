package com.openiot.data.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.mongodb.document.DeadLetterDocument;
import com.openiot.data.service.RetryService;
import com.openiot.data.service.RetryService.DeadLetterStats;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 死信队列控制器
 * 提供死信查询和重试接口
 */
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DeadLetterController {

    private final RetryService retryService;

    /**
     * 获取待重试的死信列表
     */
    @GetMapping("/pending")
    public ApiResponse<List<DeadLetterDocument>> getPendingDeadLetters() {
        List<DeadLetterDocument> list = retryService.getPendingDeadLetters();
        return ApiResponse.success(list);
    }

    /**
     * 获取死信统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<DeadLetterStats> getStats() {
        DeadLetterStats stats = retryService.getStats();
        return ApiResponse.success(stats);
    }

    /**
     * 重试单条死信
     */
    @PostMapping("/retry/{deadLetterId}")
    public ApiResponse<Void> retrySingle(@PathVariable String deadLetterId) {
        boolean success = retryService.retrySingle(deadLetterId);
        if (success) {
            return ApiResponse.success("重试成功", null);
        } else {
            return ApiResponse.error("重试失败");
        }
    }

    /**
     * 强制重试（忽略重试次数限制）
     */
    @PostMapping("/retry/{deadLetterId}/force")
    public ApiResponse<Void> forceRetry(@PathVariable String deadLetterId) {
        boolean success = retryService.forceRetry(deadLetterId);
        if (success) {
            return ApiResponse.success("强制重试成功", null);
        } else {
            return ApiResponse.error("强制重试失败");
        }
    }

    /**
     * 批量重试所有待处理的死信
     */
    @PostMapping("/retry/batch")
    public ApiResponse<BatchRetryResponse> retryBatch() {
        int successCount = retryService.retryPending();
        BatchRetryResponse response = new BatchRetryResponse();
        response.setSuccessCount(successCount);
        return ApiResponse.success("批量重试完成", response);
    }

    @lombok.Data
    public static class BatchRetryResponse {
        private int successCount;
    }
}
