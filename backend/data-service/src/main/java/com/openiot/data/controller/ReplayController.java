package com.openiot.data.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.data.replay.ReplayService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 重放控制器
 */
@RestController
@RequestMapping("/api/v1/replay")
@RequiredArgsConstructor
public class ReplayController {

    private final ReplayService replayService;

    /**
     * 按时间窗口重放
     */
    @PostMapping("/time-window")
    public ApiResponse<ReplayResponse> replayByTimeWindow(
            @RequestBody TimeWindowRequest request) {

        int count = replayService.replayByTimeWindow(
                request.getTenantId(),
                request.getDeviceId(),
                request.getStartTime(),
                request.getEndTime()
        );

        ReplayResponse response = new ReplayResponse();
        response.setProcessedCount(count);
        return ApiResponse.success("重放完成", response);
    }

    /**
     * 手动触发单条重放
     */
    @PostMapping("/single/{eventId}")
    public ApiResponse<Void> replaySingle(@PathVariable(value = "eventId") String eventId) {
        boolean success = replayService.replaySingle(eventId);
        if (success) {
            return ApiResponse.success("重放成功", null);
        } else {
            return ApiResponse.error("重放失败");
        }
    }

    /**
     * 重新发送到 Kafka
     */
    @PostMapping("/resend/{eventId}")
    public ApiResponse<Void> resendToKafka(@PathVariable(value = "eventId") String eventId) {
        boolean success = replayService.resendToKafka(eventId);
        if (success) {
            return ApiResponse.success("重新发送成功", null);
        } else {
            return ApiResponse.error("重新发送失败");
        }
    }

    @Data
    public static class TimeWindowRequest {
        private String tenantId;
        private String deviceId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime startTime;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime endTime;
    }

    @Data
    public static class ReplayResponse {
        private int processedCount;
    }
}
