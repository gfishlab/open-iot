package com.openiot.data.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.data.entity.AlarmRecord;
import com.openiot.data.entity.AlarmRule;
import com.openiot.data.alarm.AlarmService;
import com.openiot.data.mapper.AlarmRecordMapper;
import com.openiot.data.mapper.AlarmRuleMapper;
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
import java.util.Arrays;
import java.util.List;

/**
 * 告警管理控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alarms")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警规则和告警记录管理接口")
public class AlarmController {

    private final AlarmRuleMapper ruleMapper;
    private final AlarmRecordMapper recordMapper;
    private final AlarmService alarmService;

    // ==================== 告警规则管理 ====================

    /**
     * 分页查询告警规则
     */
    @GetMapping("/rules/page")
    @Operation(summary = "告警规则分页", description = "分页查询告警规则列表")
    public ApiResponse<Page<AlarmRule>> queryRulePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "规则名称") @RequestParam(required = false) String ruleName,
            @Parameter(description = "告警级别") @RequestParam(required = false) String alarmLevel,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "设备ID") @RequestParam(required = false) Long deviceId) {

        Page<AlarmRule> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();

        if (ruleName != null && !ruleName.isEmpty()) {
            wrapper.like(AlarmRule::getRuleName, ruleName);
        }
        if (alarmLevel != null && !alarmLevel.isEmpty()) {
            wrapper.eq(AlarmRule::getAlarmLevel, alarmLevel);
        }
        if (productId != null) {
            wrapper.eq(AlarmRule::getProductId, productId);
        }
        if (deviceId != null) {
            wrapper.eq(AlarmRule::getDeviceId, deviceId);
        }
        wrapper.eq(AlarmRule::getDelFlag, "0");
        wrapper.orderByDesc(AlarmRule::getCreateTime);

        Page<AlarmRule> result = ruleMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    @Operation(summary = "创建告警规则", description = "创建新的告警规则")
    public ApiResponse<Void> createRule(@Valid @RequestBody CreateRuleRequest request) {
        AlarmRule rule = new AlarmRule();
        rule.setTenantId(request.getTenantId());
        rule.setProductId(request.getProductId());
        rule.setDeviceId(request.getDeviceId());
        rule.setRuleName(request.getRuleName());
        rule.setAlarmLevel(request.getAlarmLevel());
        rule.setTriggerType(request.getTriggerType());
        rule.setTriggerCondition(request.getTriggerCondition());
        rule.setDurationSeconds(request.getDurationSeconds());
        rule.setContentTemplate(request.getContentTemplate());
        rule.setRecoveryCondition(request.getRecoveryCondition());
        rule.setNotifyType(request.getNotifyType());
        rule.setNotifyConfig(request.getNotifyConfig());
        rule.setSilenceEnabled(request.getSilenceEnabled());
        rule.setSilenceSeconds(request.getSilenceSeconds());
        rule.setStatus("1");
        rule.setDelFlag("0");
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());

        ruleMapper.insert(rule);
        return ApiResponse.success();
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{id}")
    @Operation(summary = "更新告警规则", description = "更新已有的告警规则")
    public ApiResponse<Void> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody UpdateRuleRequest request) {

        AlarmRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return ApiResponse.fail("告警规则不存在");
        }

        if (request.getRuleName() != null) {
            rule.setRuleName(request.getRuleName());
        }
        if (request.getAlarmLevel() != null) {
            rule.setAlarmLevel(request.getAlarmLevel());
        }
        if (request.getTriggerCondition() != null) {
            rule.setTriggerCondition(request.getTriggerCondition());
        }
        if (request.getDurationSeconds() != null) {
            rule.setDurationSeconds(request.getDurationSeconds());
        }
        if (request.getContentTemplate() != null) {
            rule.setContentTemplate(request.getContentTemplate());
        }
        if (request.getRecoveryCondition() != null) {
            rule.setRecoveryCondition(request.getRecoveryCondition());
        }
        if (request.getNotifyType() != null) {
            rule.setNotifyType(request.getNotifyType());
        }
        if (request.getNotifyConfig() != null) {
            rule.setNotifyConfig(request.getNotifyConfig());
        }
        if (request.getSilenceEnabled() != null) {
            rule.setSilenceEnabled(request.getSilenceEnabled());
        }
        if (request.getSilenceSeconds() != null) {
            rule.setSilenceSeconds(request.getSilenceSeconds());
        }
        if (request.getStatus() != null) {
            rule.setStatus(request.getStatus());
        }

        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);

        return ApiResponse.success();
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除告警规则", description = "删除告警规则")
    public ApiResponse<Void> deleteRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        AlarmRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return ApiResponse.fail("告警规则不存在");
        }

        rule.setDelFlag("1");
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);

        return ApiResponse.success();
    }

    /**
     * 启用/禁用告警规则
     */
    @PutMapping("/rules/{id}/status")
    @Operation(summary = "切换规则状态", description = "启用或禁用告警规则")
    public ApiResponse<Void> toggleRuleStatus(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {

        AlarmRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return ApiResponse.fail("告警规则不存在");
        }

        rule.setStatus(status);
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);

        return ApiResponse.success();
    }

    // ==================== 告警记录管理 ====================

    /**
     * 分页查询告警记录
     */
    @GetMapping("/records/page")
    @Operation(summary = "告警记录分页", description = "分页查询告警记录列表")
    public ApiResponse<Page<AlarmRecord>> queryRecordPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "规则ID") @RequestParam(required = false) Long ruleId,
            @Parameter(description = "设备ID") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "告警级别") @RequestParam(required = false) String alarmLevel,
            @Parameter(description = "告警状态") @RequestParam(required = false) String alarmStatus,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Page<AlarmRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();

        if (ruleId != null) {
            wrapper.eq(AlarmRecord::getRuleId, ruleId);
        }
        if (deviceId != null) {
            wrapper.eq(AlarmRecord::getDeviceId, deviceId);
        }
        if (alarmLevel != null && !alarmLevel.isEmpty()) {
            wrapper.eq(AlarmRecord::getAlarmLevel, alarmLevel);
        }
        if (alarmStatus != null && !alarmStatus.isEmpty()) {
            wrapper.eq(AlarmRecord::getAlarmStatus, alarmStatus);
        }
        if (startTime != null) {
            wrapper.ge(AlarmRecord::getAlarmTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AlarmRecord::getAlarmTime, endTime);
        }
        wrapper.orderByDesc(AlarmRecord::getAlarmTime);

        Page<AlarmRecord> result = recordMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 确认告警
     */
    @PutMapping("/records/{id}/acknowledge")
    @Operation(summary = "确认告警", description = "确认告警记录")
    public ApiResponse<Void> acknowledgeAlarm(
            @Parameter(description = "告警记录ID") @PathVariable Long id,
            @Parameter(description = "确认人ID") @RequestParam Long userId,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {

        AlarmRecord record = recordMapper.selectById(id);
        if (record == null) {
            return ApiResponse.fail("告警记录不存在");
        }

        record.setAlarmStatus("acknowledged");
        record.setAcknowledgeTime(LocalDateTime.now());
        record.setAcknowledgedBy(userId);
        record.setHandleRemark(remark);
        record.setUpdateTime(LocalDateTime.now());

        recordMapper.updateById(record);
        return ApiResponse.success();
    }

    /**
     * 关闭告警
     */
    @PutMapping("/records/{id}/close")
    @Operation(summary = "关闭告警", description = "关闭告警记录")
    public ApiResponse<Void> closeAlarm(
            @Parameter(description = "告警记录ID") @PathVariable Long id,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {

        AlarmRecord record = recordMapper.selectById(id);
        if (record == null) {
            return ApiResponse.fail("告警记录不存在");
        }

        record.setAlarmStatus("closed");
        record.setHandleRemark(remark);
        record.setUpdateTime(LocalDateTime.now());

        recordMapper.updateById(record);
        return ApiResponse.success();
    }

    /**
     * 批量关闭告警
     */
    @PutMapping("/records/batch-close")
    @Operation(summary = "批量关闭告警", description = "批量关闭告警记录")
    public ApiResponse<Void> batchCloseAlarms(
            @Parameter(description = "告警记录ID列表") @RequestBody List<Long> ids,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {

        if (ids == null || ids.isEmpty()) {
            return ApiResponse.fail("告警记录ID列表不能为空");
        }

        for (Long id : ids) {
            AlarmRecord record = recordMapper.selectById(id);
            if (record != null) {
                record.setAlarmStatus("closed");
                record.setHandleRemark(remark);
                record.setUpdateTime(LocalDateTime.now());
                recordMapper.updateById(record);
            }
        }

        return ApiResponse.success();
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "告警统计", description = "获取告警统计信息")
    public ApiResponse<AlarmStatistics> getStatistics(
            @Parameter(description = "设备ID") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null) {
            wrapper.eq(AlarmRecord::getDeviceId, deviceId);
        }
        if (startTime != null) {
            wrapper.ge(AlarmRecord::getAlarmTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AlarmRecord::getAlarmTime, endTime);
        }

        long totalCount = recordMapper.selectCount(wrapper);
        long activeCount = recordMapper.selectCount(wrapper.eq(AlarmRecord::getAlarmStatus, "active"));
        long resolvedCount = recordMapper.selectCount(wrapper.eq(AlarmRecord::getAlarmStatus, "resolved"));
        long criticalCount = recordMapper.selectCount(wrapper.eq(AlarmRecord::getAlarmLevel, "critical")
                .and(w -> w.in(AlarmRecord::getAlarmStatus, Arrays.asList("active", "pending"))));

        AlarmStatistics stats = new AlarmStatistics();
        stats.setTotalCount(totalCount);
        stats.setActiveCount(activeCount);
        stats.setResolvedCount(resolvedCount);
        stats.setCriticalCount(criticalCount);

        return ApiResponse.success(stats);
    }

    // ========== DTO 类 ==========

    @Data
    public static class CreateRuleRequest {
        @NotNull(message = "租户ID不能为空")
        private Long tenantId;

        private Long productId;
        private Long deviceId;

        @NotBlank(message = "规则名称不能为空")
        private String ruleName;

        @NotBlank(message = "告警级别不能为空")
        private String alarmLevel;

        @NotBlank(message = "触发类型不能为空")
        private String triggerType;

        @NotBlank(message = "触发条件不能为空")
        private String triggerCondition;

        private Integer durationSeconds;
        private String contentTemplate;
        private String recoveryCondition;
        private String notifyType;
        private String notifyConfig;
        private Boolean silenceEnabled;
        private Integer silenceSeconds;
    }

    @Data
    public static class UpdateRuleRequest {
        private String ruleName;
        private String alarmLevel;
        private String triggerCondition;
        private Integer durationSeconds;
        private String contentTemplate;
        private String recoveryCondition;
        private String notifyType;
        private String notifyConfig;
        private Boolean silenceEnabled;
        private Integer silenceSeconds;
        private String status;
    }

    @Data
    public static class AlarmStatistics {
        private Long totalCount;
        private Long activeCount;
        private Long resolvedCount;
        private Long criticalCount;
    }
}
