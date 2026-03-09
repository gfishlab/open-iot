package com.openiot.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.service.AlertService;
import com.openiot.device.service.AlertService.AlertStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警管理控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警查询、处理、统计接口")
public class AlertController {

    private final AlertService alertService;

    /**
     * 分页查询告警
     */
    @GetMapping
    @Operation(summary = "查询告警列表", description = "支持按设备、级别、状态过滤")
    public ApiResponse<Page<AlertRecord>> getAlerts(
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "设备ID") @RequestParam(name = "deviceId", required = false) Long deviceId,
            @Parameter(description = "告警级别") @RequestParam(name = "level", required = false) String level,
            @Parameter(description = "处理状态") @RequestParam(name = "status", required = false) String status) {

        log.info("查询告警列表: page={}, size={}, deviceId={}, level={}, status={}",
                page, size, deviceId, level, status);

        Page<AlertRecord> result = alertService.queryAlerts(page, size, deviceId, level, status);
        return ApiResponse.success(result);
    }

    /**
     * 查询告警详情
     */
    @GetMapping("/{alertId}")
    @Operation(summary = "查询告警详情", description = "根据告警ID查询详情")
    public ApiResponse<AlertRecord> getAlertDetail(
            @Parameter(description = "告警ID") @PathVariable(name = "alertId") Long alertId) {

        log.info("查询告警详情: alertId={}", alertId);

        AlertRecord alert = alertService.getAlertDetail(alertId);
        return ApiResponse.success(alert);
    }

    /**
     * 处理告警
     */
    @PutMapping("/{alertId}/handle")
    @Operation(summary = "处理告警", description = "更新告警处理状态")
    public ApiResponse<AlertRecord> handleAlert(
            @Parameter(description = "告警ID") @PathVariable(name = "alertId") Long alertId,
            @RequestBody HandleRequest request) {

        log.info("处理告警: alertId={}, status={}", alertId, request.getStatus());

        AlertRecord alert = alertService.handleAlert(alertId, request.getStatus());
        return ApiResponse.success("告警处理成功", alert);
    }

    /**
     * 批量处理告警
     */
    @PutMapping("/batch-handle")
    @Operation(summary = "批量处理告警", description = "批量更新告警状态")
    public ApiResponse<BatchHandleResponse> batchHandleAlerts(
            @RequestBody BatchHandleRequest request) {

        log.info("批量处理告警: alertIds={}, status={}", request.getAlertIds(), request.getStatus());

        int count = alertService.batchHandleAlerts(request.getAlertIds(), request.getStatus());

        BatchHandleResponse response = new BatchHandleResponse();
        response.setTotalCount(request.getAlertIds().size());
        response.setSuccessCount(count);

        return ApiResponse.success("批量处理完成", response);
    }

    /**
     * 查询告警统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "告警统计", description = "查询告警统计数据")
    public ApiResponse<AlertStatisticsVO> getStatistics(
            @Parameter(description = "设备ID") @RequestParam(name = "deviceId", required = false) Long deviceId) {

        log.info("查询告警统计: deviceId={}", deviceId);

        AlertStatisticsVO statistics = alertService.getStatistics(deviceId);
        return ApiResponse.success(statistics);
    }

    /**
     * 查询待处理告警
     */
    @GetMapping("/pending")
    @Operation(summary = "待处理告警", description = "查询待处理和正在处理的告警")
    public ApiResponse<List<AlertRecord>> getPendingAlerts(
            @Parameter(description = "条数") @RequestParam(name = "limit", defaultValue = "10") int limit) {

        log.info("查询待处理告警: limit={}", limit);

        List<AlertRecord> alerts = alertService.getRecentAlerts(limit);
        return ApiResponse.success(alerts);
    }

    /**
     * 查询设备告警历史
     */
    @GetMapping("/device/{deviceId}/history")
    @Operation(summary = "设备告警历史", description = "查询指定设备的告警历史")
    public ApiResponse<List<AlertRecord>> getDeviceAlertHistory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "条数") @RequestParam(name = "limit", defaultValue = "20") int limit) {

        log.info("查询设备告警历史: deviceId={}, limit={}", deviceId, limit);

        List<AlertRecord> alerts = alertService.getDeviceAlertHistory(deviceId, limit);
        return ApiResponse.success(alerts);
    }

    /**
     * 处理告警请求
     */
    @Data
    public static class HandleRequest {
        /**
         * 处理状态：processing-处理中，resolved-已解决，ignored-已忽略
         */
        @NotEmpty(message = "状态不能为空")
        private String status;
    }

    /**
     * 批量处理请求
     */
    @Data
    public static class BatchHandleRequest {
        /**
         * 告警ID列表
         */
        @NotEmpty(message = "告警ID列表不能为空")
        private List<Long> alertIds;

        /**
         * 处理状态
         */
        @NotEmpty(message = "状态不能为空")
        private String status;
    }

    /**
     * 批量处理响应
     */
    @Data
    public static class BatchHandleResponse {
        private int totalCount;
        private int successCount;
    }
}
