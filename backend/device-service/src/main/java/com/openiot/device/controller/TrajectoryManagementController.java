package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.DeviceTrajectory;
import com.openiot.device.service.DeviceTrajectoryService;
import com.openiot.device.service.DeviceTrajectoryService.*;
import com.openiot.device.service.DeviceTrajectoryStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备轨迹控制器
 * 统一管理设备轨迹上报（写操作）和历史统计分析（读操作）
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}/trajectory")
@RequiredArgsConstructor
@Tag(name = "设备轨迹", description = "设备轨迹上报、查询和统计分析接口")
public class TrajectoryManagementController {

    private final DeviceTrajectoryStorageService trajectoryStorageService;
    private final DeviceTrajectoryService trajectoryService;

    /**
     * 上报设备轨迹点
     */
    @PostMapping("/report")
    @Operation(summary = "上报轨迹", description = "设备上报位置信息")
    public ApiResponse<DeviceTrajectory> reportTrajectory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @RequestBody TrajectoryReportRequest request) {

        log.debug("接收设备轨迹上报: deviceId={}, lat={}, lng={}",
                deviceId, request.getLatitude(), request.getLongitude());

        DeviceTrajectory trajectory = new DeviceTrajectory();
        trajectory.setDeviceId(deviceId);
        trajectory.setLatitude(request.getLatitude());
        trajectory.setLongitude(request.getLongitude());
        trajectory.setSpeed(request.getSpeed());
        trajectory.setHeading(request.getHeading());
        trajectory.setEventTime(request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now());

        DeviceTrajectory saved = trajectoryStorageService.save(trajectory);

        return ApiResponse.success("轨迹上报成功", saved);
    }

    /**
     * 批量上报轨迹点
     */
    @PostMapping("/batch")
    @Operation(summary = "批量上报轨迹", description = "批量上报设备位置信息")
    public ApiResponse<Map<String, Object>> batchReportTrajectory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @RequestBody List<TrajectoryReportRequest> requests) {

        log.info("接收批量设备轨迹上报: deviceId={}, count={}", deviceId, requests.size());

        List<DeviceTrajectory> trajectories = requests.stream()
                .map(req -> {
                    DeviceTrajectory t = new DeviceTrajectory();
                    t.setDeviceId(deviceId);
                    t.setLatitude(req.getLatitude());
                    t.setLongitude(req.getLongitude());
                    t.setSpeed(req.getSpeed());
                    t.setHeading(req.getHeading());
                    t.setEventTime(req.getEventTime() != null ? req.getEventTime() : LocalDateTime.now());
                    return t;
                })
                .toList();

        int count = trajectoryStorageService.batchSave(trajectories);

        return ApiResponse.success(Map.of(
                "total", requests.size(),
                "success", count
        ));
    }

    /**
     * 查询设备轨迹
     */
    @GetMapping("/query")
    @Operation(summary = "查询轨迹", description = "查询指定时间范围内的设备轨迹")
    public ApiResponse<List<DeviceTrajectory>> queryTrajectory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备轨迹: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        List<DeviceTrajectory> trajectories = trajectoryStorageService.query(deviceId, startTime, endTime);

        return ApiResponse.success(trajectories);
    }

    /**
     * 分页查询设备轨迹
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询轨迹", description = "分页查询设备轨迹")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeviceTrajectory>> queryTrajectoryPage(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "100") int pageSize) {

        log.info("分页查询设备轨迹: deviceId={}, pageNum={}, pageSize={}", deviceId, pageNum, pageSize);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeviceTrajectory> page =
                trajectoryStorageService.queryPage(deviceId, startTime, endTime, pageNum, pageSize);

        return ApiResponse.success(page);
    }

    /**
     * 获取最新轨迹点
     */
    @GetMapping("/latest")
    @Operation(summary = "最新轨迹", description = "获取设备最新的位置信息")
    public ApiResponse<DeviceTrajectory> getLatestTrajectory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {

        log.debug("查询设备最新轨迹: deviceId={}", deviceId);

        DeviceTrajectory trajectory = trajectoryStorageService.getLatest(deviceId);

        return ApiResponse.success(trajectory);
    }

    /**
     * 计算轨迹距离
     */
    @GetMapping("/distance")
    @Operation(summary = "轨迹距离", description = "计算指定时间范围内的轨迹总距离")
    public ApiResponse<Map<String, Object>> calculateDistance(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("计算设备轨迹距离: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        double distance = trajectoryStorageService.calculateDistance(deviceId, startTime, endTime);

        return ApiResponse.success(Map.of(
                "deviceId", deviceId,
                "startTime", startTime,
                "endTime", endTime,
                "distance", distance, // 米
                "distanceKm", distance / 1000 // 千米
        ));
    }

    /**
     * 轨迹统计（距离、速度等）
     */
    @GetMapping("/distance-stats")
    @Operation(summary = "轨迹距离统计", description = "统计设备轨迹距离、速度等信息")
    public ApiResponse<DeviceTrajectoryStorageService.TrajectoryStatisticsVO> getTrajectoryStatistics(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("统计设备轨迹: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);

        // 查询轨迹点
        List<DeviceTrajectory> trajectories = trajectoryStorageService.query(deviceId, startTime, endTime);

        // 构建统计信息
        DeviceTrajectoryStorageService.TrajectoryStatisticsVO statistics =
                new DeviceTrajectoryStorageService.TrajectoryStatisticsVO();
        statistics.setDeviceId(deviceId);
        statistics.setStartTime(startTime);
        statistics.setEndTime(endTime);
        statistics.setPointCount(trajectories.size());

        if (!trajectories.isEmpty()) {
            statistics.setFirstPointTime(trajectories.get(0).getEventTime());
            statistics.setLastPointTime(trajectories.get(trajectories.size() - 1).getEventTime());

            // 计算总距离
            double totalDistance = trajectoryStorageService.calculateDistance(deviceId, startTime, endTime);
            statistics.setTotalDistance(totalDistance);

            // 计算平均速度和最大速度
            double avgSpeed = trajectories.stream()
                    .filter(t -> t.getSpeed() != null)
                    .mapToDouble(t -> t.getSpeed().doubleValue())
                    .average()
                    .orElse(0.0);
            statistics.setAvgSpeed(avgSpeed);

            double maxSpeed = trajectories.stream()
                    .filter(t -> t.getSpeed() != null)
                    .mapToDouble(t -> t.getSpeed().doubleValue())
                    .max()
                    .orElse(0.0);
            statistics.setMaxSpeed(maxSpeed);
        }

        return ApiResponse.success(statistics);
    }

    // ==================== 统计分析接口 ====================

    /**
     * 查询设备数据统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "数据统计", description = "查询指定时间范围内的数据统计信息")
    public ApiResponse<DataStatisticsVO> getStatistics(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("查询设备数据统计: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);
        DataStatisticsVO statistics = trajectoryService.getStatistics(deviceId, startTime, endTime);
        return ApiResponse.success(statistics);
    }

    /**
     * 查询数据趋势
     */
    @GetMapping("/trend")
    @Operation(summary = "数据趋势", description = "查询属性值随时间的变化趋势")
    public ApiResponse<List<TrendPointVO>> getTrend(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam(name = "property") String property,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "时间间隔（分钟）") @RequestParam(name = "interval", defaultValue = "5") int interval) {

        log.info("查询设备数据趋势: deviceId={}, property={}, interval={}min", deviceId, property, interval);
        List<TrendPointVO> trend = trajectoryService.getTrend(deviceId, property, startTime, endTime, interval);
        return ApiResponse.success(trend);
    }

    /**
     * 查询数据分布
     */
    @GetMapping("/distribution")
    @Operation(summary = "数据分布", description = "查询属性值的分布情况")
    public ApiResponse<List<DistributionBucketVO>> getDistribution(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @RequestParam(name = "property") String property,
            @Parameter(description = "开始时间") @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "分桶数量") @RequestParam(name = "buckets", defaultValue = "10") int buckets) {

        log.info("查询设备数据分布: deviceId={}, property={}, buckets={}", deviceId, property, buckets);
        List<DistributionBucketVO> distribution = trajectoryService.getDistribution(
                deviceId, property, startTime, endTime, buckets);
        return ApiResponse.success(distribution);
    }

    /**
     * 轨迹上报请求
     */
    @lombok.Data
    public static class TrajectoryReportRequest {
        /**
         * 纬度
         */
        private java.math.BigDecimal latitude;

        /**
         * 经度
         */
        private java.math.BigDecimal longitude;

        /**
         * 速度 (km/h)
         */
        private java.math.BigDecimal speed;

        /**
         * 航向角 (度)
         */
        private java.math.BigDecimal heading;

        /**
         * 事件时间
         */
        private LocalDateTime eventTime;
    }
}
