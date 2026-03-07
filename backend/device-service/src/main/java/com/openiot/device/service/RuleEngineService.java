package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.*;
import com.openiot.device.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 规则引擎服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService extends ServiceImpl<RuleMapper, Rule> {

    private final RuleConditionMapper ruleConditionMapper;
    private final RuleActionMapper ruleActionMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final DeviceMapper deviceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 监听设备数据，执行规则检查
     */
    @KafkaListener(topics = "device-data", groupId = "rule-engine-group")
    public void processDeviceData(String message) {
        try {
            // 解析 Kafka 消息
            Map<String, Object> messageData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            Long deviceId = ((Number) messageData.get("deviceId")).longValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");

            log.debug("接收到设备数据: deviceId={}, data={}", deviceId, data);

            // 查询设备信息
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                log.warn("设备不存在: {}", deviceId);
                return;
            }

            // 执行规则检查
            checkRules(device, data);

        } catch (Exception e) {
            log.error("处理设备数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查规则并执行动作
     *
     * @param device 设备
     * @param data   设备数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkRules(Device device, Map<String, Object> data) {
        // 1. 查询设备级别的启用规则
        List<Rule> deviceRules = queryEnabledRules("device", device.getId());

        // 2. 查询产品级别的启用规则
        List<Rule> productRules = queryEnabledRules("product", device.getProductId());

        // 3. 合并规则
        List<Rule> allRules = new ArrayList<>();
        allRules.addAll(deviceRules);
        allRules.addAll(productRules);

        log.debug("查询到规则: deviceRules={}, productRules={}", deviceRules.size(), productRules.size());

        // 4. 逐个检查规则
        for (Rule rule : allRules) {
            try {
                if (evaluateRule(rule, data)) {
                    log.info("规则触发: ruleId={}, ruleName={}, device={}",
                            rule.getId(), rule.getRuleName(), device.getDeviceCode());

                    // 执行规则动作
                    executeActions(rule, device, data);
                }
            } catch (Exception e) {
                log.error("规则检查失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 评估规则条件是否满足
     *
     * @param rule 规则
     * @param data 设备数据
     * @return true-条件满足，false-条件不满足
     */
    private boolean evaluateRule(Rule rule, Map<String, Object> data) {
        // 查询规则的所有条件
        LambdaQueryWrapper<RuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleCondition::getRuleId, rule.getId())
               .orderByAsc(RuleCondition::getConditionOrder);
        List<RuleCondition> conditions = ruleConditionMapper.selectList(wrapper);

        if (conditions.isEmpty()) {
            return false;
        }

        // 如果只有一个条件，直接评估
        if (conditions.size() == 1) {
            return evaluateCondition(conditions.get(0), data);
        }

        // 多个条件，按逻辑关系评估
        boolean result = evaluateCondition(conditions.get(0), data);

        for (int i = 1; i < conditions.size(); i++) {
            RuleCondition currentCondition = conditions.get(i);
            RuleCondition prevCondition = conditions.get(i - 1);

            boolean currentResult = evaluateCondition(currentCondition, data);

            // 根据前一个条件的逻辑关系进行运算
            if ("AND".equalsIgnoreCase(prevCondition.getLogicRelation())) {
                result = result && currentResult;
            } else if ("OR".equalsIgnoreCase(prevCondition.getLogicRelation())) {
                result = result || currentResult;
            }

            // 如果已经确定最终结果，可以提前退出
            if ("AND".equalsIgnoreCase(prevCondition.getLogicRelation()) && !result) {
                return false;
            }
            if ("OR".equalsIgnoreCase(prevCondition.getLogicRelation()) && result) {
                return true;
            }
        }

        return result;
    }

    /**
     * 评估单个条件
     *
     * @param condition 规则条件
     * @param data      设备数据
     * @return true-条件满足，false-条件不满足
     */
    private boolean evaluateCondition(RuleCondition condition, Map<String, Object> data) {
        String propertyIdentifier = condition.getPropertyIdentifier();
        String operator = condition.getOperator();
        String thresholdValue = condition.getThresholdValue();

        // 获取属性值
        Object propertyValue = data.get(propertyIdentifier);
        if (propertyValue == null) {
            log.warn("属性值不存在: {}", propertyIdentifier);
            return false;
        }

        // 解析阈值
        Object threshold = parseThreshold(thresholdValue);

        // 构建表达式并执行
        String expression = buildExpression(operator, propertyValue, threshold);
        try {
            Expression compiledExpression = AviatorEvaluator.compile(expression);
            Boolean result = (Boolean) compiledExpression.execute();
            log.debug("条件评估: property={}, operator={}, value={}, threshold={}, result={}",
                    propertyIdentifier, operator, propertyValue, threshold, result);
            return result;
        } catch (Exception e) {
            log.error("表达式执行失败: expression={}, error={}", expression, e.getMessage());
            return false;
        }
    }

    /**
     * 构建表达式
     */
    private String buildExpression(String operator, Object value, Object threshold) {
        // 转换值和阈值为数字
        double numValue = toDouble(value);
        double numThreshold = toDouble(threshold);

        return switch (operator) {
            case "gt" -> String.format("%f > %f", numValue, numThreshold);
            case "lt" -> String.format("%f < %f", numValue, numThreshold);
            case "eq" -> String.format("%f == %f", numValue, numThreshold);
            case "gte" -> String.format("%f >= %f", numValue, numThreshold);
            case "lte" -> String.format("%f <= %f", numValue, numThreshold);
            case "between" -> {
                // threshold 格式：{"min": 0, "max": 100}
                if (threshold instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> range = (Map<String, Object>) threshold;
                    double min = toDouble(range.get("min"));
                    double max = toDouble(range.get("max"));
                    yield String.format("%f >= %f && %f <= %f", numValue, min, numValue, max);
                }
                yield "false";
            }
            default -> "false";
        };
    }

    /**
     * 解析阈值
     */
    private Object parseThreshold(String thresholdValue) {
        try {
            // 尝试解析为数字
            return Double.parseDouble(thresholdValue);
        } catch (NumberFormatException e) {
            // 尝试解析为 JSON（between 操作符的范围）
            try {
                return objectMapper.readValue(thresholdValue, Map.class);
            } catch (Exception ex) {
                log.warn("阈值解析失败: {}", thresholdValue);
                return 0;
            }
        }
    }

    /**
     * 转换为 double
     */
    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 执行规则动作
     *
     * @param rule   规则
     * @param device 设备
     * @param data   设备数据
     */
    private void executeActions(Rule rule, Device device, Map<String, Object> data) {
        // 查询规则的所有动作
        LambdaQueryWrapper<RuleAction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleAction::getRuleId, rule.getId());
        List<RuleAction> actions = ruleActionMapper.selectList(wrapper);

        for (RuleAction action : actions) {
            try {
                executeAction(action, rule, device, data);
            } catch (Exception e) {
                log.error("执行动作失败: actionId={}, error={}", action.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 执行单个动作
     */
    private void executeAction(RuleAction action, Rule rule, Device device, Map<String, Object> data) {
        String actionType = action.getActionType();
        JsonNode actionConfig = action.getActionConfig();

        switch (actionType) {
            case "alert":
                // 创建告警记录
                createAlert(rule, device, data, actionConfig);
                break;

            case "webhook":
                // 调用 Webhook（暂不实现）
                log.info("Webhook 动作暂未实现: actionId={}", action.getId());
                break;

            case "device":
                // 设备控制（暂不实现）
                log.info("设备控制动作暂未实现: actionId={}", action.getId());
                break;

            default:
                log.warn("未知的动作类型: {}", actionType);
        }
    }

    /**
     * 创建告警记录
     */
    private void createAlert(Rule rule, Device device, Map<String, Object> data, JsonNode actionConfig) {
        AlertRecord alert = new AlertRecord();
        alert.setTenantId(device.getTenantId());
        alert.setRuleId(rule.getId());
        alert.setDeviceId(device.getId());
        alert.setProductId(device.getProductId());

        // 从动作配置获取告警级别和消息
        String level = actionConfig.has("level") ? actionConfig.get("level").asText() : "info";
        String message = actionConfig.has("message") ? actionConfig.get("message").asText() : "规则触发";

        alert.setAlertLevel(level);
        alert.setAlertTitle(String.format("[%s] %s", rule.getRuleName(), message));
        alert.setAlertContent(buildAlertContent(data));
        alert.setAlertData(objectMapper.valueToTree(data));
        alert.setStatus("pending");

        alertRecordMapper.insert(alert);

        log.info("创建告警记录: alertId={}, rule={}, device={}, level={}",
                alert.getId(), rule.getRuleName(), device.getDeviceCode(), level);
    }

    /**
     * 构建告警内容
     */
    private String buildAlertContent(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("触发数据：");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2); // 移除最后的 ", "
        }
        return sb.toString();
    }

    /**
     * 查询启用的规则
     *
     * @param ruleType 规则类型
     * @param targetId 目标ID
     * @return 规则列表
     */
    private List<Rule> queryEnabledRules(String ruleType, Long targetId) {
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rule::getRuleType, ruleType)
               .eq(Rule::getTargetId, targetId)
               .eq(Rule::getStatus, "1");
        return this.list(wrapper);
    }

    /**
     * 创建规则
     *
     * @param rule 规则
     * @return 创建的规则
     */
    @Transactional(rollbackFor = Exception.class)
    public Rule createRule(Rule rule) {
        String tenantId = TenantContext.getTenantId();
        rule.setTenantId(Long.valueOf(tenantId));

        if (rule.getStatus() == null) {
            rule.setStatus("1"); // 默认启用
        }

        this.save(rule);
        log.info("创建规则成功: ruleName={}", rule.getRuleName());
        return rule;
    }
}
