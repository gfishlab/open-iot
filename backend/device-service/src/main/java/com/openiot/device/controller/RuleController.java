package com.openiot.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.*;
import com.openiot.device.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 规则控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "规则管理", description = "规则 CRUD 接口")
public class RuleController {

    private final RuleEngineService ruleEngineService;
    private final com.openiot.device.mapper.RuleMapper ruleMapper;
    private final com.openiot.device.mapper.RuleConditionMapper ruleConditionMapper;
    private final com.openiot.device.mapper.RuleActionMapper ruleActionMapper;
    private final com.openiot.device.mapper.AlertRecordMapper alertRecordMapper;

    /**
     * 创建规则
     */
    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的规则")
    public ApiResponse<Rule> createRule(@RequestBody RuleCreateRequest request) {
        Rule rule = new Rule();
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setTargetId(request.getTargetId());
        rule.setStatus(request.getStatus() != null ? request.getStatus() : "1");

        Rule created = ruleEngineService.createRule(rule);
        return ApiResponse.success("规则创建成功", created);
    }

    /**
     * 查询规则列表
     */
    @GetMapping
    @Operation(summary = "查询规则列表", description = "分页查询规则")
    public ApiResponse<Page<Rule>> getRules(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "ruleType", required = false) String ruleType,
            @RequestParam(name = "targetId", required = false) Long targetId) {

        Page<Rule> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();

        if (ruleType != null) {
            wrapper.eq(Rule::getRuleType, ruleType);
        }
        if (targetId != null) {
            wrapper.eq(Rule::getTargetId, targetId);
        }

        wrapper.orderByDesc(Rule::getCreateTime);

        Page<Rule> result = ruleMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 查询规则详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询规则详情", description = "查询规则及其条件和动作")
    public ApiResponse<RuleDetailVO> getRuleDetail(@PathVariable(name = "id") Long id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return ApiResponse.notFound("规则不存在");
        }

        // 查询条件
        LambdaQueryWrapper<RuleCondition> conditionWrapper = new LambdaQueryWrapper<>();
        conditionWrapper.eq(RuleCondition::getRuleId, id)
                      .orderByAsc(RuleCondition::getConditionOrder);
        java.util.List<RuleCondition> conditions = ruleConditionMapper.selectList(conditionWrapper);

        // 查询动作
        LambdaQueryWrapper<RuleAction> actionWrapper = new LambdaQueryWrapper<>();
        actionWrapper.eq(RuleAction::getRuleId, id);
        java.util.List<RuleAction> actions = ruleActionMapper.selectList(actionWrapper);

        RuleDetailVO detail = new RuleDetailVO();
        detail.setRule(rule);
        detail.setConditions(conditions);
        detail.setActions(actions);

        return ApiResponse.success(detail);
    }

    /**
     * 添加规则条件
     */
    @PostMapping("/{ruleId}/conditions")
    @Operation(summary = "添加规则条件", description = "为规则添加条件")
    public ApiResponse<RuleCondition> addCondition(
            @PathVariable(name = "ruleId") Long ruleId,
            @RequestBody RuleCondition condition) {

        condition.setRuleId(ruleId);
        ruleConditionMapper.insert(condition);

        return ApiResponse.success("条件添加成功", condition);
    }

    /**
     * 添加规则动作
     */
    @PostMapping("/{ruleId}/actions")
    @Operation(summary = "添加规则动作", description = "为规则添加动作")
    public ApiResponse<RuleAction> addAction(
            @PathVariable(name = "ruleId") Long ruleId,
            @RequestBody RuleAction action) {

        action.setRuleId(ruleId);
        ruleActionMapper.insert(action);

        return ApiResponse.success("动作添加成功", action);
    }

    /**
     * 查询告警记录
     */
    @GetMapping("/alert-records")
    @Operation(summary = "查询告警记录", description = "分页查询告警记录")
    public ApiResponse<Page<AlertRecord>> getAlertRecords(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "deviceId", required = false) Long deviceId,
            @RequestParam(name = "status", required = false) String status) {

        Page<AlertRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();

        if (deviceId != null) {
            wrapper.eq(AlertRecord::getDeviceId, deviceId);
        }
        if (status != null) {
            wrapper.eq(AlertRecord::getStatus, status);
        }

        wrapper.orderByDesc(AlertRecord::getCreateTime);

        Page<AlertRecord> result = alertRecordMapper.selectPage(pageParam, wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 创建规则请求
     */
    @Data
    public static class RuleCreateRequest {
        private String ruleName;
        private String ruleType;  // device, product
        private Long targetId;
        private String status;
    }

    /**
     * 规则详情 VO
     */
    @Data
    public static class RuleDetailVO {
        private Rule rule;
        private java.util.List<RuleCondition> conditions;
        private java.util.List<RuleAction> actions;
    }
}
