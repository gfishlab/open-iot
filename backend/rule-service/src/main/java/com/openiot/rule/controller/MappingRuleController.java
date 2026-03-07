package com.openiot.rule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.rule.entity.MappingRule;
import com.openiot.rule.service.MappingRuleService;
import com.openiot.rule.vo.MappingRuleCreateVO;
import com.openiot.rule.vo.MappingRuleVO;
import com.openiot.rule.vo.MappingTestRequestVO;
import com.openiot.rule.vo.MappingTestResultVO;
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
 * 映射规则控制器
 * 提供映射规则的 CRUD 和测试接口
 *
 * <p>映射规则用于将解析后的原始字段映射到物模型属性，支持：
 * <ul>
 *   <li>字段重命名：sourceField -> targetProperty</li>
 *   <li>单位转换：UNIT_CONVERT（如摄氏度转华氏度）</li>
 *   <li>公式计算：FORMULA（支持 Aviator 表达式）</li>
 * </ul>
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mapping-rules")
@RequiredArgsConstructor
@Tag(name = "映射规则管理", description = "映射规则的增删改查和测试接口")
public class MappingRuleController {

    private final MappingRuleService mappingRuleService;

    /**
     * 创建映射规则
     */
    @PostMapping
    @Operation(summary = "创建映射规则", description = "为指定产品创建映射规则，支持字段重命名、单位转换、公式计算")
    public ApiResponse<MappingRuleVO> createMappingRule(@Valid @RequestBody MappingRuleCreateVO vo) {
        MappingRule rule = mappingRuleService.createMappingRule(vo);
        return ApiResponse.success("创建映射规则成功", toVO(rule));
    }

    /**
     * 根据ID查询映射规则
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询映射规则", description = "根据ID查询映射规则详情")
    public ApiResponse<MappingRuleVO> getMappingRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {
        MappingRule rule = mappingRuleService.getRuleById(id);
        return ApiResponse.success(toVO(rule));
    }

    /**
     * 更新映射规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新映射规则", description = "更新指定ID的映射规则")
    public ApiResponse<MappingRuleVO> updateMappingRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody MappingRuleCreateVO vo) {
        MappingRule rule = mappingRuleService.updateMappingRule(id, vo);
        return ApiResponse.success("更新映射规则成功", toVO(rule));
    }

    /**
     * 删除映射规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除映射规则", description = "删除指定ID的映射规则")
    public ApiResponse<Void> deleteMappingRule(
            @Parameter(description = "规则ID") @PathVariable Long id) {
        mappingRuleService.deleteMappingRule(id);
        return ApiResponse.success("删除映射规则成功", null);
    }

    /**
     * 测试映射规则
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试映射规则", description = "使用指定规则测试映射解析后的数据")
    public ApiResponse<MappingTestResultVO> testMappingRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody MappingTestRequestVO vo) {
        MappingTestResultVO result = mappingRuleService.testMappingRule(id, vo);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询映射规则
     */
    @GetMapping
    @Operation(summary = "分页查询映射规则", description = "根据条件分页查询映射规则列表")
    public ApiResponse<Page<MappingRuleVO>> getMappingRuleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {

        Page<MappingRule> page = getMappingRulePage(pageNum, pageSize, productId, status);

        // 转换为 VO
        Page<MappingRuleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));

        return ApiResponse.success(voPage);
    }

    /**
     * 根据产品ID查询映射规则列表
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "查询产品的映射规则", description = "查询指定产品的所有启用的映射规则")
    public ApiResponse<List<MappingRuleVO>> getMappingRulesByProductId(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        List<MappingRule> rules = mappingRuleService.getMappingRuleByProductId(productId);

        List<MappingRuleVO> voList = rules.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return ApiResponse.success(voList);
    }

    /**
     * 启用/禁用映射规则
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新规则状态", description = "启用或禁用映射规则")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Parameter(description = "状态：1-启用，0-禁用") @RequestParam String status) {

        mappingRuleService.updateStatus(id, status);
        return ApiResponse.success("更新状态成功", null);
    }

    /**
     * 实体转 VO
     */
    private MappingRuleVO toVO(MappingRule rule) {
        MappingRuleVO vo = new MappingRuleVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }

    /**
     * 分页查询映射规则（内部方法）
     */
    private Page<MappingRule> getMappingRulePage(int pageNum, int pageSize, Long productId, String status) {
        // 使用 lambdaQuery 构建分页查询
        return mappingRuleService.lambdaQuery()
                .eq(productId != null, MappingRule::getProductId, productId)
                .eq(status != null && !status.isEmpty(), MappingRule::getStatus, status)
                .orderByDesc(MappingRule::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }
}
