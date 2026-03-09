package com.openiot.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.CommandSendRecord;
import com.openiot.device.entity.PropertySetRecord;
import com.openiot.device.entity.ServiceCallRecord;
import com.openiot.device.service.DeviceControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备控制控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}")
@RequiredArgsConstructor
@Tag(name = "设备控制", description = "设备服务调用、属性设置、命令下发接口")
public class DeviceControlController {

    private final DeviceControlService deviceControlService;
    private final com.openiot.device.mapper.ServiceCallRecordMapper serviceCallRecordMapper;
    private final com.openiot.device.mapper.PropertySetRecordMapper propertySetRecordMapper;
    private final com.openiot.device.mapper.CommandSendRecordMapper commandSendRecordMapper;

    /**
     * 调用设备服务
     */
    @PostMapping("/services/{serviceIdentifier}/call")
    @Operation(summary = "调用设备服务", description = "调用设备定义的服务（如开关、重启等）")
    public ApiResponse<ServiceCallRecord> callService(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "服务标识符") @PathVariable(name = "serviceIdentifier") String serviceIdentifier,
            @RequestBody ServiceCallRequest request) {

        log.info("调用设备服务: deviceId={}, service={}", deviceId, serviceIdentifier);

        ServiceCallRecord record = deviceControlService.callService(deviceId, serviceIdentifier, request.getInputParams());

        return ApiResponse.success("服务调用已下发", record);
    }

    /**
     * 设置设备属性
     */
    @PutMapping("/properties/{propertyIdentifier}")
    @Operation(summary = "设置设备属性", description = "设置设备属性值")
    public ApiResponse<PropertySetRecord> setProperty(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "属性标识符") @PathVariable(name = "propertyIdentifier") String propertyIdentifier,
            @RequestBody PropertySetRequest request) {

        log.info("设置设备属性: deviceId={}, property={}, value={}", deviceId, propertyIdentifier, request.getValue());

        PropertySetRecord record = deviceControlService.setProperty(deviceId, propertyIdentifier, request.getValue());

        return ApiResponse.success("属性设置已下发", record);
    }

    /**
     * 发送命令
     */
    @PostMapping("/commands/{commandIdentifier}")
    @Operation(summary = "发送命令", description = "向设备下发命令")
    public ApiResponse<CommandSendRecord> sendCommand(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "命令标识符") @PathVariable(name = "commandIdentifier") String commandIdentifier,
            @RequestBody CommandSendRequest request) {

        log.info("发送设备命令: deviceId={}, command={}", deviceId, commandIdentifier);

        CommandSendRecord record = deviceControlService.sendCommand(deviceId, commandIdentifier, request.getParams());

        return ApiResponse.success("命令已下发", record);
    }

    /**
     * 查询服务调用记录
     */
    @GetMapping("/services/calls")
    @Operation(summary = "服务调用记录", description = "查询设备的服务调用历史")
    public ApiResponse<Page<ServiceCallRecord>> getServiceCallRecords(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "状态") @RequestParam(name = "status", required = false) String status) {

        Page<ServiceCallRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ServiceCallRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceCallRecord::getDeviceId, deviceId);

        if (status != null) {
            wrapper.eq(ServiceCallRecord::getStatus, status);
        }

        wrapper.orderByDesc(ServiceCallRecord::getCallTime);

        Page<ServiceCallRecord> result = serviceCallRecordMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 查询属性设置记录
     */
    @GetMapping("/properties/sets")
    @Operation(summary = "属性设置记录", description = "查询设备的属性设置历史")
    public ApiResponse<Page<PropertySetRecord>> getPropertySetRecords(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "状态") @RequestParam(name = "status", required = false) String status) {

        Page<PropertySetRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PropertySetRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertySetRecord::getDeviceId, deviceId);

        if (status != null) {
            wrapper.eq(PropertySetRecord::getStatus, status);
        }

        wrapper.orderByDesc(PropertySetRecord::getCreateTime);

        Page<PropertySetRecord> result = propertySetRecordMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 查询命令下发记录
     */
    @GetMapping("/commands/sends")
    @Operation(summary = "命令下发记录", description = "查询设备的命令下发历史")
    public ApiResponse<Page<CommandSendRecord>> getCommandSendRecords(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Parameter(description = "页码") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "状态") @RequestParam(name = "status", required = false) String status) {

        Page<CommandSendRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CommandSendRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommandSendRecord::getDeviceId, deviceId);

        if (status != null) {
            wrapper.eq(CommandSendRecord::getStatus, status);
        }

        wrapper.orderByDesc(CommandSendRecord::getSendTime);

        Page<CommandSendRecord> result = commandSendRecordMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 服务调用请求
     */
    @Data
    public static class ServiceCallRequest {
        private Map<String, Object> inputParams;
    }

    /**
     * 属性设置请求
     */
    @Data
    public static class PropertySetRequest {
        private Object value;
    }

    /**
     * 命令发送请求
     */
    @Data
    public static class CommandSendRequest {
        private Map<String, Object> params;
    }
}
