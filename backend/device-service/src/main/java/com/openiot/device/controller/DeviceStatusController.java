package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.service.DeviceStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备状态查询控制器
 * 提供设备在线状态、设备列表等查询接口
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/status")
@RequiredArgsConstructor
@Tag(name = "设备状态", description = "设备在线状态、在线设备列表等接口")
public class DeviceStatusController {

    private final DeviceStatusService deviceStatusService;

    /**
     * 查询单个设备的在线状态
     *
     * @param deviceId 设备 ID
     * @return 设备状态信息
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "查询设备状态", description = "查询单个设备的在线状态")
    public ApiResponse<DeviceStatusService.DeviceStatusVO> getDeviceStatus(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {
        log.info("查询设备状态: deviceId={}", deviceId);

        DeviceStatusService.DeviceStatusVO status = deviceStatusService.getStatus(deviceId);

        return ApiResponse.success(status);
    }

    /**
     * 批量查询设备在线状态
     *
     * @param deviceIds 设备 ID 列表
     * @return 设备状态列表
     */
    @PostMapping("/batch")
    @Operation(summary = "批量查询设备状态", description = "批量查询多个设备的在线状态")
    public ApiResponse<List<DeviceStatusService.DeviceStatusVO>> batchGetStatus(
            @RequestBody List<Long> deviceIds) {
        log.info("批量查询设备状态: deviceIds={}", deviceIds);

        List<DeviceStatusService.DeviceStatusVO> statuses = deviceStatusService.batchGetStatus(deviceIds);

        return ApiResponse.success(statuses);
    }

    /**
     * 查询租户下所有在线设备
     *
     * @return 在线设备ID列表
     */
    @GetMapping("/online")
    @Operation(summary = "查询在线设备", description = "查询租户下所有在线设备的ID列表")
    public ApiResponse<Set<Long>> getOnlineDevices() {
        String tenantId = TenantContext.getTenantId();
        log.info("查询在线设备: tenantId={}", tenantId);

        Set<Long> onlineDevices = deviceStatusService.getOnlineDevices(tenantId);

        return ApiResponse.success(onlineDevices);
    }

    /**
     * 查询在线设备数量
     *
     * @return 在线设备数量
     */
    @GetMapping("/online/count")
    @Operation(summary = "查询在线设备数量", description = "查询租户在线设备总数")
    public ApiResponse<Map<String, Long>> getOnlineCount() {
        String tenantId = TenantContext.getTenantId();
        log.info("查询在线设备数量: tenantId={}", tenantId);

        long count = deviceStatusService.getOnlineCount(tenantId);

        return ApiResponse.success(Map.of("onlineCount", count));
    }

    /**
     * 检查设备是否在线
     *
     * @param deviceId 设备ID
     * @return 是否在线
     */
    @GetMapping("/{deviceId}/online")
    @Operation(summary = "检查设备在线状态", description = "检查指定设备是否在线")
    public ApiResponse<Map<String, Boolean>> isOnline(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {

        boolean online = deviceStatusService.isOnline(deviceId);

        return ApiResponse.success(Map.of("online", online));
    }

    /**
     * 查询设备状态历史
     *
     * @param deviceId 设备ID
     * @param limit    数量限制
     * @return 状态历史列表
     */
    @GetMapping("/{deviceId}/history")
    @Operation(summary = "查询设备状态历史", description = "查询设备最近的状态变化历史")
    public ApiResponse<List<DeviceStatusService.StatusHistoryVO>> getStatusHistory(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "数量限制") @RequestParam(name = "limit", defaultValue = "10") int limit) {

        List<DeviceStatusService.StatusHistoryVO> history = deviceStatusService.getStatusHistory(deviceId, limit);

        return ApiResponse.success(history);
    }

    /**
     * 刷新设备心跳（延长在线时间）
     *
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @PostMapping("/{deviceId}/heartbeat")
    @Operation(summary = "刷新设备心跳", description = "刷新设备在线心跳，延长在线时间")
    public ApiResponse<Void> refreshHeartbeat(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {

        deviceStatusService.refreshHeartbeat(deviceId);

        return ApiResponse.success("心跳刷新成功", null);
    }
}
