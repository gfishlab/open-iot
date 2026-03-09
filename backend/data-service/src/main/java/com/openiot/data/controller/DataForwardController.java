package com.openiot.data.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.data.entity.DataForwardConfig;
import com.openiot.data.entity.DataForwardLog;
import com.openiot.data.forward.DataForwardService;
import com.openiot.data.mapper.DataForwardConfigMapper;
import com.openiot.data.mapper.DataForwardLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 数据转发配置控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/forward")
@RequiredArgsConstructor
@Tag(name = "数据转发", description = "数据转发配置和管理接口")
public class DataForwardController {

    private final DataForwardConfigMapper configMapper;
    private final DataForwardLogMapper logMapper;
    private final DataForwardService forwardService;

    /**
     * 分页查询转发配置
     */
    @GetMapping("/config/page")
    @Operation(summary = "转发配置分页", description = "分页查询数据转发配置列表")
    public ApiResponse<Page<DataForwardConfig>> queryConfigPage(
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(value = "size", defaultValue = "10") Integer size,
            @Parameter(description = "配置名称") @RequestParam(value = "configName", required = false) String configName,
            @Parameter(description = "目标类型") @RequestParam(value = "targetType", required = false) String targetType) {

        Page<DataForwardConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DataForwardConfig> wrapper = new LambdaQueryWrapper<>();

        if (configName != null && !configName.isEmpty()) {
            wrapper.like(DataForwardConfig::getConfigName, configName);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(DataForwardConfig::getTargetType, targetType);
        }
        wrapper.eq(DataForwardConfig::getDelFlag, "0");
        wrapper.orderByDesc(DataForwardConfig::getCreateTime);

        Page<DataForwardConfig> result = configMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 创建转发配置
     */
    @PostMapping("/config")
    @Operation(summary = "创建转发配置", description = "创建新的数据转发配置")
    public ApiResponse<Void> createConfig(@Valid @RequestBody CreateConfigRequest request) {
        DataForwardConfig config = new DataForwardConfig();
        config.setTenantId(request.getTenantId());
        config.setConfigName(request.getConfigName());
        config.setTargetType(request.getTargetType());
        config.setEndpointConfig(request.getEndpointConfig());
        config.setFilterCondition(request.getFilterCondition());
        config.setTransformRule(request.getTransformRule());
        config.setBatchSize(request.getBatchSize());
        config.setBatchTimeoutMs(request.getBatchTimeoutMs());
        config.setRetryTimes(request.getRetryTimes());
        config.setRetryIntervalMs(request.getRetryIntervalMs());
        config.setStatus("1");
        config.setDelFlag("0");
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        configMapper.insert(config);
        return ApiResponse.success();
    }

    /**
     * 更新转发配置
     */
    @PutMapping("/config/{id}")
    @Operation(summary = "更新转发配置", description = "更新已有的数据转发配置")
    public ApiResponse<Void> updateConfig(
            @Parameter(description = "配置ID") @PathVariable(value = "id") Long id,
            @Valid @RequestBody UpdateConfigRequest request) {

        DataForwardConfig config = configMapper.selectById(id);
        if (config == null) {
            return ApiResponse.error("转发配置不存在");
        }

        if (request.getConfigName() != null) {
            config.setConfigName(request.getConfigName());
        }
        if (request.getEndpointConfig() != null) {
            config.setEndpointConfig(request.getEndpointConfig());
        }
        if (request.getFilterCondition() != null) {
            config.setFilterCondition(request.getFilterCondition());
        }
        if (request.getTransformRule() != null) {
            config.setTransformRule(request.getTransformRule());
        }
        if (request.getBatchSize() != null) {
            config.setBatchSize(request.getBatchSize());
        }
        if (request.getBatchTimeoutMs() != null) {
            config.setBatchTimeoutMs(request.getBatchTimeoutMs());
        }
        if (request.getRetryTimes() != null) {
            config.setRetryTimes(request.getRetryTimes());
        }
        if (request.getRetryIntervalMs() != null) {
            config.setRetryIntervalMs(request.getRetryIntervalMs());
        }
        if (request.getStatus() != null) {
            config.setStatus(request.getStatus());
        }

        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);

        return ApiResponse.success();
    }

    /**
     * 删除转发配置
     */
    @DeleteMapping("/config/{id}")
    @Operation(summary = "删除转发配置", description = "删除数据转发配置")
    public ApiResponse<Void> deleteConfig(@Parameter(description = "配置ID") @PathVariable(value = "id") Long id) {
        DataForwardConfig config = configMapper.selectById(id);
        if (config == null) {
            return ApiResponse.error("转发配置不存在");
        }

        config.setDelFlag("1");
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);

        return ApiResponse.success();
    }

    /**
     * 启用/禁用转发配置
     */
    @PutMapping("/config/{id}/status")
    @Operation(summary = "切换配置状态", description = "启用或禁用转发配置")
    public ApiResponse<Void> toggleConfigStatus(
            @Parameter(description = "配置ID") @PathVariable(value = "id") Long id,
            @Parameter(description = "状态") @RequestParam(value = "status") String status) {

        DataForwardConfig config = configMapper.selectById(id);
        if (config == null) {
            return ApiResponse.error("转发配置不存在");
        }

        config.setStatus(status);
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);

        return ApiResponse.success();
    }

    /**
     * 查询转发日志
     */
    @GetMapping("/log/page")
    @Operation(summary = "转发日志分页", description = "分页查询数据转发日志")
    public ApiResponse<Page<DataForwardLog>> queryLogPage(
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(value = "size", defaultValue = "10") Integer size,
            @Parameter(description = "配置ID") @RequestParam(value = "configId", required = false) Long configId,
            @Parameter(description = "设备ID") @RequestParam(value = "deviceId", required = false) Long deviceId,
            @Parameter(description = "转发状态") @RequestParam(value = "forwardStatus", required = false) String forwardStatus,
            @Parameter(description = "开始时间") @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Page<DataForwardLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DataForwardLog> wrapper = new LambdaQueryWrapper<>();

        if (configId != null) {
            wrapper.eq(DataForwardLog::getConfigId, configId);
        }
        if (deviceId != null) {
            wrapper.eq(DataForwardLog::getDeviceId, deviceId);
        }
        if (forwardStatus != null && !forwardStatus.isEmpty()) {
            wrapper.eq(DataForwardLog::getForwardStatus, forwardStatus);
        }
        if (startTime != null) {
            wrapper.ge(DataForwardLog::getForwardTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(DataForwardLog::getForwardTime, endTime);
        }
        wrapper.orderByDesc(DataForwardLog::getForwardTime);

        Page<DataForwardLog> result = logMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 获取转发统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "转发统计", description = "获取数据转发统计信息")
    public ApiResponse<ForwardStatistics> getStatistics(
            @Parameter(description = "配置ID") @RequestParam(value = "configId", required = false) Long configId,
            @Parameter(description = "设备ID") @RequestParam(value = "deviceId", required = false) Long deviceId) {

        LambdaQueryWrapper<DataForwardLog> wrapper = new LambdaQueryWrapper<>();
        if (configId != null) {
            wrapper.eq(DataForwardLog::getConfigId, configId);
        }
        if (deviceId != null) {
            wrapper.eq(DataForwardLog::getDeviceId, deviceId);
        }

        long totalCount = logMapper.selectCount(wrapper);
        long successCount = logMapper.selectCount(wrapper.eq(DataForwardLog::getForwardStatus, "success"));
        long failCount = logMapper.selectCount(wrapper.eq(DataForwardLog::getForwardStatus, "fail"));

        ForwardStatistics stats = new ForwardStatistics();
        stats.setTotalCount(totalCount);
        stats.setSuccessCount(successCount);
        stats.setFailCount(failCount);
        stats.setSuccessRate(totalCount > 0 ? (double) successCount / totalCount * 100 : 0);

        return ApiResponse.success(stats);
    }

    // ========== DTO 类 ==========

    @Data
    public static class CreateConfigRequest {
        @NotNull(message = "租户ID不能为空")
        private Long tenantId;

        @NotBlank(message = "配置名称不能为空")
        private String configName;

        @NotBlank(message = "目标类型不能为空")
        private String targetType;

        @NotBlank(message = "端点配置不能为空")
        private String endpointConfig;

        private String filterCondition;
        private String transformRule;
        private Integer batchSize = 100;
        private Integer batchTimeoutMs = 5000;
        private Integer retryTimes = 3;
        private Integer retryIntervalMs = 1000;
    }

    @Data
    public static class UpdateConfigRequest {
        private String configName;
        private String endpointConfig;
        private String filterCondition;
        private String transformRule;
        private Integer batchSize;
        private Integer batchTimeoutMs;
        private Integer retryTimes;
        private Integer retryIntervalMs;
        private String status;
    }

    @Data
    public static class ForwardStatistics {
        private Long totalCount;
        private Long successCount;
        private Long failCount;
        private Double successRate;
    }
}
