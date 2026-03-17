package com.openiot.device.ota.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.ota.entity.OtaDeviceStatus;
import com.openiot.device.ota.entity.OtaUpgradeTask;
import com.openiot.device.ota.service.OtaTaskService;
import com.openiot.device.ota.vo.OtaTaskCreateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * OTA 升级任务控制器
 * 提供创建升级任务、查询任务列表、查询任务设备状态等接口
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ota/tasks")
@RequiredArgsConstructor
@Tag(name = "OTA 升级任务", description = "OTA 升级任务管理接口")
public class OtaTaskController {

    private final OtaTaskService otaTaskService;

    /**
     * 创建 OTA 升级任务
     */
    @PostMapping
    @Operation(summary = "创建升级任务", description = "创建 OTA 固件升级任务，指定目标产品和设备")
    public ApiResponse<OtaUpgradeTask> createTask(
            @Valid @RequestBody OtaTaskCreateVO vo) {

        log.info("创建 OTA 升级任务: taskName={}, productId={}, firmwareVersionId={}, scope={}",
                vo.getTaskName(), vo.getProductId(), vo.getFirmwareVersionId(), vo.getUpgradeScope());

        OtaUpgradeTask task = otaTaskService.createTask(vo);
        return ApiResponse.success("升级任务创建成功", task);
    }

    /**
     * 分页查询升级任务列表
     */
    @GetMapping
    @Operation(summary = "查询任务列表", description = "分页查询 OTA 升级任务列表")
    public ApiResponse<Page<OtaUpgradeTask>> listTasks(
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size) {

        log.info("查询 OTA 任务列表: page={}, size={}", page, size);

        Page<OtaUpgradeTask> result = otaTaskService.listTasks(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 查询任务下的设备升级状态
     */
    @GetMapping("/{taskId}/devices")
    @Operation(summary = "查询任务设备状态", description = "分页查询指定任务下各设备的升级状态")
    public ApiResponse<Page<OtaDeviceStatus>> getTaskDeviceStatuses(
            @Parameter(description = "任务ID") @PathVariable(name = "taskId") Long taskId,
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size) {

        log.info("查询任务设备状态: taskId={}, page={}, size={}", taskId, page, size);

        Page<OtaDeviceStatus> result = otaTaskService.getTaskDeviceStatuses(taskId, page, size);
        return ApiResponse.success(result);
    }
}
