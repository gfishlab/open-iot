package com.openiot.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.DeviceData;
import com.openiot.device.service.DeviceDataReportService;
import com.openiot.device.service.DeviceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备数据上报控制器
 * 用于 HTTP 协议设备上报数据
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/data")
@RequiredArgsConstructor
@Tag(name = "设备数据", description = "设备数据上报、查询接口")
public class DeviceDataController {

    private final DeviceDataReportService reportService;
    private final DeviceDataService deviceDataService;

    /**
     * HTTP 设备数据上报
     */
    @PostMapping
    @Operation(summary = "上报设备数据", description = "HTTP 协议设备上报属性数据")
    public ApiResponse<Void> reportData(
            @RequestHeader("X-Device-Token") String deviceToken,
            @RequestBody DeviceDataRequest request) {
        // TODO: 验证 deviceToken 并获取 tenantId 和 deviceId
        String tenantId = "1"; // 临时硬编码
        String deviceId = "1"; // 临时硬编码

        reportService.reportData(
                deviceId,
                tenantId,
                request.getEventType(),
                request.getPayload(),
                request.getRawPayload()
        );

        return ApiResponse.success("上报成功", null);
    }

    /**
     * 批量上报设备数据
     */
    @PostMapping("/batch")
    @Operation(summary = "批量上报数据", description = "批量上报多个设备的数据")
    public ApiResponse<Map<String, Object>> reportDataBatch(
            @RequestBody BatchDataRequest request) {

        int successCount = 0;
        int failCount = 0;

        for (DeviceDataRequest dataRequest : request.getDatas()) {
            try {
                reportService.reportData(
                        String.valueOf(dataRequest.getDeviceId()),
                        String.valueOf(dataRequest.getTenantId()),
                        dataRequest.getEventType(),
                        dataRequest.getPayload(),
                        dataRequest.getRawPayload()
                );
                successCount++;
            } catch (Exception e) {
                log.error("数据上报失败: {}", e.getMessage());
                failCount++;
            }
        }

        return ApiResponse.success(Map.of(
                "totalCount", request.getDatas().size(),
                "successCount", successCount,
                "failCount", failCount
        ));
    }

    /**
     * 查询设备最新数据
     */
    @GetMapping("/{deviceId}/latest")
    @Operation(summary = "查询最新数据", description = "查询设备最新上报的数据")
    public ApiResponse<DeviceData> getLatestData(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {

        DeviceData latestData = deviceDataService.getLatest(deviceId);
        return ApiResponse.success(latestData);
    }

    /**
     * 分页查询设备历史数据
     */
    @GetMapping("/{deviceId}/history")
    @Operation(summary = "查询历史数据", description = "分页查询设备历史数据")
    public ApiResponse<Page<DeviceData>> getHistory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "页码") @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Page<DeviceData> page = deviceDataService.queryDeviceData(deviceId, pageNum, pageSize, startTime, endTime);
        return ApiResponse.success(page);
    }

    /**
     * 查询设备属性数据
     */
    @GetMapping("/{deviceId}/properties/{propertyIdentifier}")
    @Operation(summary = "查询属性数据", description = "查询设备指定属性的历史数据")
    public ApiResponse<List<DeviceDataService.PropertyDataVO>> getPropertyData(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @PathVariable(name = "propertyIdentifier") String propertyIdentifier,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<DeviceDataService.PropertyDataVO> data = deviceDataService.queryPropertyData(
                deviceId, propertyIdentifier, startTime, endTime);

        return ApiResponse.success(data);
    }

    /**
     * 查询设备数据统计
     */
    @GetMapping("/{deviceId}/statistics")
    @Operation(summary = "查询数据统计", description = "查询设备数据统计信息")
    public ApiResponse<DeviceDataService.DataStatisticsVO> getStatistics(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        DeviceDataService.DataStatisticsVO statistics = deviceDataService.getStatistics(
                deviceId, startTime, endTime);

        return ApiResponse.success(statistics);
    }

    /**
     * 设备数据上报请求
     */
    @Data
    public static class DeviceDataRequest {
        private Long tenantId;
        private Long deviceId;
        private String eventType;
        private Object payload;
        private String rawPayload;
        private String timestamp;
    }

    /**
     * 批量数据上报请求
     */
    @Data
    public static class BatchDataRequest {
        private List<DeviceDataRequest> datas;
    }
}
