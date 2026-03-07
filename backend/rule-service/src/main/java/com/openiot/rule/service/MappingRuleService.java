package com.openiot.rule.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.redis.util.RedisUtil;
import com.openiot.common.security.context.TenantContext;
import com.openiot.rule.entity.MappingRule;
import com.openiot.rule.mapper.MappingRuleMapper;
import com.openiot.rule.vo.MappingRuleCreateVO;
import com.openiot.rule.vo.MappingTestRequestVO;
import com.openiot.rule.vo.MappingTestResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 映射规则服务
 * 负责映射规则的 CRUD 操作、循环依赖检测和测试功能
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
@Service
@RequiredArgsConstructor
public class MappingRuleService extends ServiceImpl<MappingRuleMapper, MappingRule> {

    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis 发布/订阅通道名称（用于通知 connect-service 刷新缓存）
     */
    private static final String MAPPING_RULE_UPDATE_CHANNEL = "openiot:mapping_rule:update";

    /**
     * 创建映射规则
     *
     * <p>创建时会进行以下验证：
     * <ol>
     *   <li>验证映射配置格式是否合法</li>
     *   <li>检查同一产品下规则名称是否重复</li>
     *   <li>检测循环依赖（如果 A→B 且 B→A，则拒绝创建）</li>
     * </ol>
     *
     * @param vo 创建请求 VO
     * @return 创建的规则实体
     */
    @Transactional(rollbackFor = Exception.class)
    public MappingRule createMappingRule(MappingRuleCreateVO vo) {
        // 获取租户ID
        Long tenantId = getTenantId();

        // 验证映射配置格式
        validateFieldMappings(vo.getFieldMappings());

        // 检查同一产品下规则名称是否重复
        if (existsRuleName(tenantId, vo.getProductId(), vo.getRuleName(), null)) {
            throw BusinessException.badRequest("规则名称已存在: " + vo.getRuleName());
        }

        // 检测循环依赖
        detectCircularDependency(vo.getFieldMappings(), tenantId, vo.getProductId(), null);

        // 创建规则实体
        MappingRule rule = new MappingRule();
        BeanUtils.copyProperties(vo, rule);
        rule.setTenantId(tenantId);

        // 设置默认值
        if (rule.getStatus() == null) {
            rule.setStatus("1");
        }

        // 保存规则
        this.save(rule);

        log.info("创建映射规则成功: {} (id={}, productId={})",
                rule.getRuleName(), rule.getId(), rule.getProductId());

        return rule;
    }

    /**
     * 更新映射规则
     *
     * @param id 规则ID
     * @param vo 更新请求 VO
     * @return 更新后的规则实体
     */
    @Transactional(rollbackFor = Exception.class)
    public MappingRule updateMappingRule(Long id, MappingRuleCreateVO vo) {
        // 查询规则
        MappingRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        // 验证映射配置格式
        validateFieldMappings(vo.getFieldMappings());

        // 检查规则名称是否重复
        if (existsRuleName(rule.getTenantId(), vo.getProductId(), vo.getRuleName(), id)) {
            throw BusinessException.badRequest("规则名称已存在: " + vo.getRuleName());
        }

        // 检测循环依赖
        detectCircularDependency(vo.getFieldMappings(), rule.getTenantId(), vo.getProductId(), id);

        // 更新字段
        rule.setProductId(vo.getProductId());
        rule.setRuleName(vo.getRuleName());
        rule.setFieldMappings(vo.getFieldMappings());
        if (vo.getStatus() != null) {
            rule.setStatus(vo.getStatus());
        }

        // 保存更新
        this.updateById(rule);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleUpdate(rule);

        log.info("更新映射规则成功: {} (id={})", rule.getRuleName(), id);

        return rule;
    }

    /**
     * 删除映射规则
     *
     * @param id 规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMappingRule(Long id) {
        // 查询规则
        MappingRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        // 软删除
        this.removeById(id);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleDelete(rule);

        log.info("删除映射规则成功: {} (id={}, productId={})",
                rule.getRuleName(), id, rule.getProductId());
    }

    /**
     * 测试映射规则
     *
     * @param id 规则ID
     * @param vo 测试请求 VO
     * @return 映射结果
     */
    public MappingTestResultVO testMappingRule(Long id, MappingTestRequestVO vo) {
        // 查询规则
        MappingRule rule = getRuleById(id);

        // 检查租户权限
        checkTenantAccess(rule);

        // 使用规则配置或请求中的自定义配置
        String fieldMappings = vo.getFieldMappings() != null ?
                vo.getFieldMappings() : rule.getFieldMappings();

        return testMappingRuleInternal(fieldMappings, vo.getParsedData());
    }

    /**
     * 测试映射规则（直接使用映射配置）
     *
     * @param fieldMappings 字段映射配置
     * @param parsedData    解析后的数据（JSON格式）
     * @return 映射结果
     */
    public MappingTestResultVO testMappingRuleInternal(String fieldMappings, String parsedData) {
        long startTime = System.currentTimeMillis();

        try {
            // 解析映射配置
            JsonNode configNode = objectMapper.readTree(fieldMappings);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null || !mappingsNode.isArray()) {
                throw BusinessException.badRequest("映射配置必须包含 'mappings' 数组");
            }

            // 解析输入数据
            JsonNode dataNode;
            try {
                dataNode = objectMapper.readTree(parsedData);
            } catch (JsonProcessingException e) {
                throw BusinessException.badRequest("测试数据不是合法的 JSON 格式: " + e.getMessage());
            }

            // 收集所有源字段名（用于检测未映射字段）
            Set<String> allSourceFields = new HashSet<>();
            dataNode.fieldNames().forEachRemaining(allSourceFields::add);

            // 执行映射
            Map<String, Object> result = new HashMap<>();

            for (JsonNode mapping : mappingsNode) {
                String sourceField = mapping.path("sourceField").asText();
                String targetProperty = mapping.path("targetProperty").asText();
                String transformType = mapping.path("transformType").asText("NONE");
                String transformConfig = mapping.path("transformConfig").asText(null);

                // 从源数据中获取值
                JsonNode valueNode = dataNode.get(sourceField);
                if (valueNode == null || valueNode.isNull()) {
                    log.debug("源字段不存在或为空: {}", sourceField);
                    continue;
                }

                // 标记该源字段已被映射
                allSourceFields.remove(sourceField);

                // 获取原始值
                Object rawValue = extractValue(valueNode);

                // 应用转换
                Object transformedValue = applyTransform(rawValue, transformType, transformConfig);

                // 存储结果
                result.put(targetProperty, transformedValue);
            }

            long durationMs = System.currentTimeMillis() - startTime;
            log.debug("映射测试成功: 映射字段数={}, 未映射字段={}, 耗时={}ms",
                    result.size(), allSourceFields, durationMs);

            return MappingTestResultVO.success(result, allSourceFields, durationMs);

        } catch (BusinessException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.warn("映射测试失败: error={}", e.getMessage());
            return MappingTestResultVO.fail(e.getMessage(), durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("映射测试异常", e);
            return MappingTestResultVO.fail("映射异常: " + e.getMessage(), durationMs);
        }
    }

    /**
     * 根据产品ID获取映射规则列表
     *
     * @param productId 产品ID
     * @return 规则列表
     */
    public List<MappingRule> getMappingRuleByProductId(Long productId) {
        Long tenantId = getTenantId();

        return lambdaQuery()
                .eq(MappingRule::getTenantId, tenantId)
                .eq(MappingRule::getProductId, productId)
                .eq(MappingRule::getStatus, "1") // 只返回启用的规则
                .orderByDesc(MappingRule::getCreateTime)
                .list();
    }

    /**
     * 根据ID获取映射规则
     *
     * @param id 规则ID
     * @return 规则实体
     */
    public MappingRule getRuleById(Long id) {
        MappingRule rule = this.getById(id);
        if (rule == null) {
            throw BusinessException.notFound("映射规则不存在: " + id);
        }
        return rule;
    }

    /**
     * 启用/禁用映射规则
     *
     * @param id     规则ID
     * @param status 状态：1-启用，0-禁用
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        MappingRule rule = getRuleById(id);
        checkTenantAccess(rule);

        rule.setStatus(status);
        this.updateById(rule);

        // 发布 Redis 消息通知 connect-service 刷新缓存
        publishRuleUpdate(rule);

        log.info("更新映射规则状态: {} (id={}, status={})", rule.getRuleName(), id, status);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证字段映射配置格式
     */
    private void validateFieldMappings(String fieldMappings) {
        try {
            JsonNode configNode = objectMapper.readTree(fieldMappings);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null || !mappingsNode.isArray()) {
                throw BusinessException.badRequest("映射配置必须包含 'mappings' 数组");
            }

            // 验证每个映射项
            for (JsonNode mapping : mappingsNode) {
                String sourceField = mapping.path("sourceField").asText();
                String targetProperty = mapping.path("targetProperty").asText();
                String transformType = mapping.path("transformType").asText("NONE");

                if (sourceField.isEmpty()) {
                    throw BusinessException.badRequest("映射配置中的 sourceField 不能为空");
                }
                if (targetProperty.isEmpty()) {
                    throw BusinessException.badRequest("映射配置中的 targetProperty 不能为空");
                }

                // 验证转换类型
                if (!Set.of("NONE", "UNIT_CONVERT", "FORMULA").contains(transformType)) {
                    throw BusinessException.badRequest("不支持的转换类型: " + transformType);
                }

                // 如果是公式转换，验证表达式语法
                if ("FORMULA".equals(transformType)) {
                    String transformConfig = mapping.path("transformConfig").asText();
                    if (transformConfig.isEmpty()) {
                        throw BusinessException.badRequest("FORMULA 转换类型必须提供 transformConfig");
                    }
                    validateAviatorExpression(transformConfig);
                }
            }

        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("映射配置必须是合法的 JSON 格式");
        }
    }

    /**
     * 验证 Aviator 表达式语法
     */
    private void validateAviatorExpression(String expression) {
        try {
            // 编译表达式以验证语法
            AviatorEvaluator.compile(expression);
        } catch (Exception e) {
            throw BusinessException.badRequest("公式表达式语法错误: " + e.getMessage());
        }
    }

    /**
     * 检测循环依赖
     *
     * <p>循环依赖检测逻辑：
     * 如果映射规则中的 sourceField 和 targetProperty 形成环（如 A→B 且 B→A），
     * 则拒绝创建该规则。
     *
     * @param fieldMappings 当前规则的映射配置
     * @param tenantId      租户ID
     * @param productId     产品ID
     * @param excludeId     排除的规则ID（更新时排除自身）
     */
    private void detectCircularDependency(String fieldMappings, Long tenantId, Long productId, Long excludeId) {
        try {
            // 解析当前规则的映射关系
            JsonNode configNode = objectMapper.readTree(fieldMappings);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null) {
                return;
            }

            // 构建当前规则的映射关系：targetProperty -> sourceField
            Map<String, String> currentMappings = new HashMap<>();
            for (JsonNode mapping : mappingsNode) {
                String sourceField = mapping.path("sourceField").asText();
                String targetProperty = mapping.path("targetProperty").asText();
                currentMappings.put(targetProperty, sourceField);
            }

            // 查询同一产品下的其他映射规则
            List<MappingRule> existingRules = lambdaQuery()
                    .eq(MappingRule::getTenantId, tenantId)
                    .eq(MappingRule::getProductId, productId)
                    .ne(excludeId != null, MappingRule::getId, excludeId)
                    .list();

            // 检测与其他规则的循环依赖
            for (MappingRule existingRule : existingRules) {
                JsonNode existingConfig = objectMapper.readTree(existingRule.getFieldMappings());
                JsonNode existingMappingsNode = existingConfig.get("mappings");

                if (existingMappingsNode == null) {
                    continue;
                }

                // 构建已有规则的映射关系
                for (JsonNode existingMapping : existingMappingsNode) {
                    String existingSource = existingMapping.path("sourceField").asText();
                    String existingTarget = existingMapping.path("targetProperty").asText();

                    // 检测循环依赖：
                    // 如果当前规则的 sourceField == 已有规则的 targetProperty
                    // 且当前规则的 targetProperty == 已有规则的 sourceField
                    // 则形成循环依赖
                    for (Map.Entry<String, String> entry : currentMappings.entrySet()) {
                        String currentTarget = entry.getKey();
                        String currentSource = entry.getValue();

                        if (currentSource.equals(existingTarget) && currentTarget.equals(existingSource)) {
                            throw BusinessException.badRequest(
                                    String.format("检测到循环依赖: 字段 '%s' -> '%s' 与规则 '%s' 冲突",
                                            currentSource, currentTarget, existingRule.getRuleName()));
                        }
                    }
                }
            }

        } catch (JsonProcessingException e) {
            log.error("解析映射配置失败", e);
            // 不抛出异常，因为格式验证已经在前面完成
        }
    }

    /**
     * 从 JsonNode 提取值
     */
    private Object extractValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isTextual()) {
            return node.asText();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isBigDecimal()) {
            return node.decimalValue();
        } else if (node.isBigInteger()) {
            return node.bigIntegerValue();
        }

        return node.asText();
    }

    /**
     * 应用转换函数
     *
     * @param rawValue        原始值
     * @param transformType   转换类型
     * @param transformConfig 转换配置
     * @return 转换后的值
     */
    private Object applyTransform(Object rawValue, String transformType, String transformConfig) {
        if (rawValue == null) {
            return null;
        }

        return switch (transformType) {
            case "NONE" -> rawValue;

            case "UNIT_CONVERT" -> applyUnitConvert(rawValue, transformConfig);

            case "FORMULA" -> applyFormula(rawValue, transformConfig);

            default -> rawValue;
        };
    }

    /**
     * 应用单位转换
     *
     * <p>预定义的单位转换配置：
     * <ul>
     *   <li>C_TO_F: 摄氏度转华氏度</li>
     *   <li>F_TO_C: 华氏度转摄氏度</li>
     *   <li>KM_TO_MILE: 公里转英里</li>
     *   <li>MILE_TO_KM: 英里转公里</li>
     *   <li>KG_TO_LB: 千克转磅</li>
     *   <li>LB_TO_KG: 磅转千克</li>
     * </ul>
     */
    private Object applyUnitConvert(Object rawValue, String transformConfig) {
        if (transformConfig == null || transformConfig.isEmpty()) {
            return rawValue;
        }

        double value;
        if (rawValue instanceof Number num) {
            value = num.doubleValue();
        } else {
            try {
                value = Double.parseDouble(rawValue.toString());
            } catch (NumberFormatException e) {
                log.warn("单位转换失败：无法将值转换为数字: {}", rawValue);
                return rawValue;
            }
        }

        return switch (transformConfig.toUpperCase()) {
            case "C_TO_F" -> value * 1.8 + 32;           // 摄氏度转华氏度
            case "F_TO_C" -> (value - 32) / 1.8;         // 华氏度转摄氏度
            case "KM_TO_MILE" -> value * 0.621371;       // 公里转英里
            case "MILE_TO_KM" -> value * 1.60934;        // 英里转公里
            case "KG_TO_LB" -> value * 2.20462;          // 千克转磅
            case "LB_TO_KG" -> value * 0.453592;         // 磅转千克
            case "M_TO_CM" -> value * 100;               // 米转厘米
            case "CM_TO_M" -> value / 100;               // 厘米转米
            case "MM_TO_CM" -> value / 10;               // 毫米转厘米
            case "CM_TO_MM" -> value * 10;               // 厘米转毫米
            case "PERCENT_TO_DECIMAL" -> value / 100;    // 百分比转小数
            case "DECIMAL_TO_PERCENT" -> value * 100;    // 小数转百分比
            default -> {
                log.warn("未知的单位转换类型: {}", transformConfig);
                yield rawValue;
            }
        };
    }

    /**
     * 应用公式计算（使用 Aviator 表达式引擎）
     *
     * <p>公式中可以使用变量：
     * <ul>
     *   <li>value: 原始值</li>
     * </ul>
     *
     * <p>示例公式：
     * <ul>
     *   <li>value * 1.8 + 32</li>
     *   <li>value / 1000</li>
     *   <li>Math.round(value * 100) / 100.0</li>
     * </ul>
     */
    private Object applyFormula(Object rawValue, String transformConfig) {
        if (transformConfig == null || transformConfig.isEmpty()) {
            return rawValue;
        }

        try {
            // 准备表达式环境变量
            Map<String, Object> env = new HashMap<>();
            env.put("value", rawValue);

            // 添加常用的数学函数
            env.put("Math", Math.class);

            // 编译并执行表达式
            Expression expression = AviatorEvaluator.compile(transformConfig);
            Object result = expression.execute(env);

            log.debug("公式计算: {} -> {} (公式: {})", rawValue, result, transformConfig);

            return result;

        } catch (Exception e) {
            log.error("公式计算失败: value={}, formula={}, error={}",
                    rawValue, transformConfig, e.getMessage());
            throw BusinessException.badRequest("公式计算失败: " + e.getMessage());
        }
    }

    /**
     * 检查规则名称是否存在
     */
    private boolean existsRuleName(Long tenantId, Long productId, String ruleName, Long excludeId) {
        return lambdaQuery()
                .eq(MappingRule::getTenantId, tenantId)
                .eq(MappingRule::getProductId, productId)
                .eq(MappingRule::getRuleName, ruleName)
                .ne(excludeId != null, MappingRule::getId, excludeId)
                .exists();
    }

    /**
     * 发布规则更新消息
     */
    private void publishRuleUpdate(MappingRule rule) {
        try {
            String message = String.format("{\"action\":\"update\",\"ruleId\":%d,\"productId\":%d,\"tenantId\":%d}",
                    rule.getId(), rule.getProductId(), rule.getTenantId());
            redisTemplate.convertAndSend(MAPPING_RULE_UPDATE_CHANNEL, message);
            log.debug("发布映射规则更新消息: {}", message);
        } catch (Exception e) {
            log.error("发布映射规则更新消息失败", e);
        }
    }

    /**
     * 发布规则删除消息
     */
    private void publishRuleDelete(MappingRule rule) {
        try {
            String message = String.format("{\"action\":\"delete\",\"ruleId\":%d,\"productId\":%d,\"tenantId\":%d}",
                    rule.getId(), rule.getProductId(), rule.getTenantId());
            redisTemplate.convertAndSend(MAPPING_RULE_UPDATE_CHANNEL, message);
            log.debug("发布映射规则删除消息: {}", message);
        } catch (Exception e) {
            log.error("发布映射规则删除消息失败", e);
        }
    }

    /**
     * 获取当前租户ID
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("未找到租户信息");
        }
        return Long.valueOf(tenantId);
    }

    /**
     * 检查租户访问权限
     */
    private void checkTenantAccess(MappingRule rule) {
        Long currentTenantId = getTenantId();
        if (!currentTenantId.equals(rule.getTenantId())) {
            log.warn("跨租户访问被拒绝: current={}, target={}",
                    currentTenantId, rule.getTenantId());
            throw BusinessException.forbidden("无权访问该映射规则");
        }
    }
}
