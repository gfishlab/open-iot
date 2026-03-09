package com.openiot.data.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.data.influxdb.InfluxDBQueryService;
import com.openiot.data.influxdb.InfluxDBService;
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
 * 历史数据控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}")
@RequiredArgsConstructor
@Tag(name = "历史数据", description = "设备历史数据查询接口")
public class HistoryDataController {

    private final InfluxDBQueryService influxDBQueryService;
    private final InfluxDBService influxDBService;

    /**
     * 查询设备属性历史
     */
    @GetMapping("/properties/history")
    @Operation(summary = "属性历史", description = "查询设备属性的历史数据")
    public ApiResponse<List<InfluxDBQueryService.PropertyDataPoint>> queryPropertyHistory(
            @Parameter(description = "设备ID") @PathVariable(value = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam(value = "property") String property,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备属性历史: deviceId={}, property={}, startTime={}, endTime={}",
                deviceId, property, startTime, endTime);

        if (!influxDBService.isAvailable()) {
            return ApiResponse.fail("InfluxDB 不可用，请检查配置");
        }

        List<InfluxDBQueryService.PropertyDataPoint> data =
                influxDBQueryService.queryPropertyHistory(deviceId, property, startTime, endTime);

        return ApiResponse.success(data);
    }

    /**
     * 查询设备属性聚合数据
     */
    @GetMapping("/properties/aggregate")
    @Operation(summary = "属性聚合", description = "查询设备属性的聚合统计数据")
    public ApiResponse<List<InfluxDBQueryService.PropertyDataPoint>> queryPropertyAggregate(
            @Parameter(description = "设备ID") @PathVariable(value = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam(value = "property") String property,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "聚合窗口") @RequestParam(value = "window", defaultValue = "5m") String window,
            @Parameter(description = "聚合函数") @RequestParam(value = "function", defaultValue = "mean") String function) {

        log.info("查询设备属性聚合: deviceId={}, property={}, window={}, function={}",
                deviceId, property, window, function);

        if (!influxDBService.isAvailable()) {
            return ApiResponse.fail("InfluxDB 不可用，请检查配置");
        }

        List<InfluxDBQueryService.PropertyDataPoint> data =
                influxDBQueryService.queryPropertyAggregated(deviceId, property, startTime, endTime, window, function);

        return ApiResponse.success(data);
    }

    /**
     * 查询设备状态历史
     */
    @GetMapping("/status/history")
    @Operation(summary = "状态历史", description = "查询设备状态变化历史")
    public ApiResponse<List<InfluxDBQueryService.StatusDataPoint>> queryStatusHistory(
            @Parameter(description = "设备ID") @PathVariable(value = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备状态历史: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        if (!influxDBService.isAvailable()) {
            return ApiResponse.fail("InfluxDB 不可用，请检查配置");
        }

        List<InfluxDBQueryService.StatusDataPoint> data =
                influxDBQueryService.queryStatusHistory(deviceId, startTime, endTime);

        return ApiResponse.success(data);
    }

    /**
     * 查询设备事件历史
     */
    @GetMapping("/events/history")
    @Operation(summary = "事件历史", description = "查询设备事件历史记录")
    public ApiResponse<List<InfluxDBQueryService.EventDataPoint>> queryEventHistory(
            @Parameter(description = "设备ID") @PathVariable(value = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备事件历史: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        if (!influxDBService.isAvailable()) {
            return ApiResponse.fail("InfluxDB 不可用，请检查配置");
        }

        List<InfluxDBQueryService.EventDataPoint> data =
                influxDBQueryService.queryEventHistory(deviceId, startTime, endTime);

        return ApiResponse.success(data);
    }

    /**
     * 查询 InfluxDB 健康状态
     */
    @GetMapping("/influxdb/health")
    @Operation(summary = "InfluxDB状态", description = "检查 InfluxDB 连接状态")
    public ApiResponse<Map<String, Object>> checkInfluxDBHealth() {
        boolean available = influxDBService.isAvailable();

        return ApiResponse.success(Map.of(
                "available", available,
                "url", available ? "连接正常" : "连接失败",
                "org", influxDBService.getOrg(),
                "bucket", influxDBService.getBucket()
        ));
    }
}
