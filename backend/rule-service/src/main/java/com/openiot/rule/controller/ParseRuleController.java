package com.openiot.rule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.rule.entity.ParseRule;
import com.openiot.rule.service.ParseRuleService;
import com.openiot.rule.vo.ParseRuleCreateVO;
import com.openiot.rule.vo.ParseRuleVO;
import com.openiot.rule.vo.ParseTestRequestVO;
import com.openiot.rule.vo.ParseTestResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 解析规则控制器
 * 提供解析规则的 CRUD 和测试接口
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parse-rules")
@RequiredArgsConstructor
@Tag(name = "解析规则管理", description = "解析规则的增删改查和测试接口")
public class ParseRuleController {

    private final ParseRuleService parseRuleService;

    /**
     * 创建解析规则
     */
    @PostMapping
    @Operation(summary = "创建解析规则", description = "为指定产品创建解析规则")
    public ApiResponse<ParseRuleVO> createParseRule(@Valid @RequestBody ParseRuleCreateVO vo) {
        ParseRule rule = parseRuleService.createParseRule(vo);
        return ApiResponse.success("创建解析规则成功", toVO(rule));
    }

    /**
     * 根据ID查询解析规则
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询解析规则", description = "根据ID查询解析规则详情")
    public ApiResponse<ParseRuleVO> getParseRule(
            @Parameter(description = "规则ID") @PathVariable(value = "id") Long id) {
        ParseRule rule = parseRuleService.getRuleById(id);
        return ApiResponse.success(toVO(rule));
    }

    /**
     * 更新解析规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新解析规则", description = "更新指定ID的解析规则")
    public ApiResponse<ParseRuleVO> updateParseRule(
            @Parameter(description = "规则ID") @PathVariable(value = "id") Long id,
            @Valid @RequestBody ParseRuleCreateVO vo) {
        ParseRule rule = parseRuleService.updateParseRule(id, vo);
        return ApiResponse.success("更新解析规则成功", toVO(rule));
    }

    /**
     * 删除解析规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除解析规则", description = "删除指定ID的解析规则")
    public ApiResponse<Void> deleteParseRule(
            @Parameter(description = "规则ID") @PathVariable(value = "id") Long id) {
        parseRuleService.deleteParseRule(id);
        return ApiResponse.success("删除解析规则成功", null);
    }

    /**
     * 测试解析规则
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试解析规则", description = "使用指定规则测试解析原始数据")
    public ApiResponse<ParseTestResultVO> testParseRule(
            @Parameter(description = "规则ID") @PathVariable(value = "id") Long id,
            @Valid @RequestBody ParseTestRequestVO vo) {
        ParseTestResultVO result = parseRuleService.testParseRule(id, vo);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询解析规则
     */
    @GetMapping
    @Operation(summary = "分页查询解析规则", description = "根据条件分页查询解析规则列表")
    public ApiResponse<Page<ParseRuleVO>> getParseRuleList(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @Parameter(description = "产品ID") @RequestParam(value = "productId", required = false) Long productId,
            @Parameter(description = "规则类型") @RequestParam(value = "ruleType", required = false) String ruleType,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false) String status) {

        Page<ParseRule> page = parseRuleService.getParseRuleList(pageNum, pageSize, productId, ruleType, status);

        // 转换为 VO
        Page<ParseRuleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));

        return ApiResponse.success(voPage);
    }

    /**
     * 根据产品ID查询解析规则列表
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "查询产品的解析规则", description = "查询指定产品的所有启用的解析规则")
    public ApiResponse<List<ParseRuleVO>> getParseRulesByProductId(
            @Parameter(description = "产品ID") @PathVariable(value = "productId") Long productId) {

        List<ParseRule> rules = parseRuleService.getParseRuleByProductId(productId);

        List<ParseRuleVO> voList = rules.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return ApiResponse.success(voList);
    }

    /**
     * 启用/禁用解析规则
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新规则状态", description = "启用或禁用解析规则")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "规则ID") @PathVariable(value = "id") Long id,
            @Parameter(description = "状态：1-启用，0-禁用") @RequestParam(value = "status") String status) {

        parseRuleService.updateStatus(id, status);
        return ApiResponse.success("更新状态成功", null);
    }

    /**
     * 实体转 VO
     */
    private ParseRuleVO toVO(ParseRule rule) {
        ParseRuleVO vo = new ParseRuleVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }
}
