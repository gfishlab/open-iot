package com.openiot.connect.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射规则引擎
 * 负责将解析后的原始字段映射到物模型属性
 *
 * <p>支持的转换类型：
 * <ul>
 *   <li>NONE: 直接赋值，无需转换</li>
 *   <li>UNIT_CONVERT: 单位转换（如摄氏度转华氏度）</li>
 *   <li>FORMULA: 公式计算（支持 Aviator 表达式）</li>
 * </ul>
 *
 * <p>映射配置格式示例：
 * <pre>
 * {
 *   "mappings": [
 *     {
 *       "sourceField": "temp",
 *       "targetProperty": "temperature",
 *       "transformType": "FORMULA",
 *       "transformConfig": "value * 1.8 + 32"
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>特性：
 * <ul>
 *   <li>支持字段重命名</li>
 *   <li>支持单位转换（预定义常用转换）</li>
 *   <li>支持 Aviator 表达式公式计算</li>
 *   <li>未映射的属性设置为 null</li>
 *   <li>表达式编译结果缓存，提升性能</li>
 * </ul>
 *
 * @author open-iot
 */
@Slf4j
@Component
public class MappingRuleEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 表达式编译缓存（提升性能）
     * Key: 表达式字符串
     * Value: 编译后的表达式对象
     */
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 执行字段映射
     *
     * @param parsedData    解析后的数据（key=源字段名，value=字段值）
     * @param fieldMappings 字段映射配置（JSON格式）
     * @return 映射后的数据（key=物模型属性名，value=转换后的值）
     */
    public Map<String, Object> applyMapping(Map<String, Object> parsedData, String fieldMappings) {
        Map<String, Object> result = new HashMap<>();

        if (parsedData == null || parsedData.isEmpty()) {
            log.debug("输入数据为空，跳过映射");
            return result;
        }

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            log.debug("映射配置为空，直接返回原始数据");
            return new HashMap<>(parsedData);
        }

        try {
            // 解析映射配置
            JsonNode configNode = objectMapper.readTree(fieldMappings);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null || !mappingsNode.isArray()) {
                log.warn("映射配置格式错误：缺少 'mappings' 数组");
                return new HashMap<>(parsedData);
            }

            // 收集已映射的源字段
            Set<String> mappedSourceFields = new HashSet<>();

            // 遍历映射规则
            for (JsonNode mapping : mappingsNode) {
                String sourceField = mapping.path("sourceField").asText();
                String targetProperty = mapping.path("targetProperty").asText();
                String transformType = mapping.path("transformType").asText("NONE");
                String transformConfig = mapping.path("transformConfig").asText(null);

                // 获取源字段值
                Object rawValue = parsedData.get(sourceField);
                if (rawValue == null) {
                    log.debug("源字段不存在: {}", sourceField);
                    continue;
                }

                // 标记已映射
                mappedSourceFields.add(sourceField);

                // 应用转换
                Object transformedValue = applyTransform(rawValue, transformType, transformConfig);

                // 存储结果
                result.put(targetProperty, transformedValue);
            }

            // 未映射的字段设置为 null（根据需求）
            for (String sourceField : parsedData.keySet()) {
                if (!mappedSourceFields.contains(sourceField)) {
                    log.debug("未映射的源字段: {}", sourceField);
                    // 不保留未映射的字段
                }
            }

            log.debug("映射完成: 输入字段数={}, 输出字段数={}", parsedData.size(), result.size());
            return result;

        } catch (JsonProcessingException e) {
            log.error("解析映射配置失败: {}", e.getMessage());
            return new HashMap<>(parsedData);
        }
    }

    /**
     * 执行字段映射（使用 JSON 字符串输入）
     *
     * @param parsedDataJson 解析后的数据（JSON格式）
     * @param fieldMappings  字段映射配置（JSON格式）
     * @return 映射后的数据
     */
    public Map<String, Object> applyMapping(String parsedDataJson, String fieldMappings) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedData = objectMapper.readValue(parsedDataJson, Map.class);
            return applyMapping(parsedData, fieldMappings);
        } catch (JsonProcessingException e) {
            log.error("解析输入数据失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 验证映射配置是否合法
     *
     * @param fieldMappings 映射配置
     * @return 是否合法
     */
    public boolean validateConfig(String fieldMappings) {
        try {
            JsonNode configNode = objectMapper.readTree(fieldMappings);
            JsonNode mappingsNode = configNode.get("mappings");

            if (mappingsNode == null || !mappingsNode.isArray()) {
                return false;
            }

            // 验证每个映射项
            for (JsonNode mapping : mappingsNode) {
                String sourceField = mapping.path("sourceField").asText();
                String targetProperty = mapping.path("targetProperty").asText();
                String transformType = mapping.path("transformType").asText("NONE");

                if (sourceField.isEmpty() || targetProperty.isEmpty()) {
                    return false;
                }

                // 验证转换类型
                if (!Set.of("NONE", "UNIT_CONVERT", "FORMULA").contains(transformType)) {
                    return false;
                }

                // 如果是公式转换，验证表达式语法
                if ("FORMULA".equals(transformType)) {
                    String transformConfig = mapping.path("transformConfig").asText();
                    if (transformConfig.isEmpty()) {
                        return false;
                    }
                    try {
                        compileExpression(transformConfig);
                    } catch (Exception e) {
                        log.warn("公式表达式语法错误: {}", transformConfig);
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            log.error("验证映射配置失败", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 应用转换函数
     */
    private Object applyTransform(Object rawValue, String transformType, String transformConfig) {
        if (rawValue == null) {
            return null;
        }

        try {
            return switch (transformType) {
                case "NONE" -> rawValue;
                case "UNIT_CONVERT" -> applyUnitConvert(rawValue, transformConfig);
                case "FORMULA" -> applyFormula(rawValue, transformConfig);
                default -> rawValue;
            };
        } catch (Exception e) {
            log.error("转换失败: type={}, config={}, value={}, error={}",
                    transformType, transformConfig, rawValue, e.getMessage());
            return rawValue; // 转换失败时返回原值
        }
    }

    /**
     * 应用单位转换
     *
     * <p>预定义的单位转换：
     * <ul>
     *   <li>C_TO_F: 摄氏度转华氏度</li>
     *   <li>F_TO_C: 华氏度转摄氏度</li>
     *   <li>KM_TO_MILE: 公里转英里</li>
     *   <li>MILE_TO_KM: 英里转公里</li>
     *   <li>KG_TO_LB: 千克转磅</li>
     *   <li>LB_TO_KG: 磅转千克</li>
     *   <li>M_TO_CM: 米转厘米</li>
     *   <li>CM_TO_M: 厘米转米</li>
     *   <li>MM_TO_CM: 毫米转厘米</li>
     *   <li>CM_TO_MM: 厘米转毫米</li>
     *   <li>PERCENT_TO_DECIMAL: 百分比转小数</li>
     *   <li>DECIMAL_TO_PERCENT: 小数转百分比</li>
     * </ul>
     */
    private Object applyUnitConvert(Object rawValue, String transformConfig) {
        if (transformConfig == null || transformConfig.isEmpty()) {
            return rawValue;
        }

        double value = toDouble(rawValue);
        if (Double.isNaN(value)) {
            log.warn("单位转换失败：无法将值转换为数字: {}", rawValue);
            return rawValue;
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
     *   <li>math.round(value * 100) / 100.0</li>
     *   <li>value > 100 ? 100 : value</li>
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

            // 编译并执行表达式（使用缓存）
            Expression expression = compileExpression(transformConfig);
            Object result = expression.execute(env);

            log.debug("公式计算: {} -> {} (公式: {})", rawValue, result, transformConfig);

            return result;

        } catch (Exception e) {
            log.error("公式计算失败: value={}, formula={}, error={}",
                    rawValue, transformConfig, e.getMessage());
            return rawValue; // 计算失败时返回原值
        }
    }

    /**
     * 编译表达式（带缓存）
     */
    private Expression compileExpression(String expression) {
        return expressionCache.computeIfAbsent(expression, AviatorEvaluator::compile);
    }

    /**
     * 将对象转换为 double
     */
    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * 清理表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
        log.info("映射规则表达式缓存已清理");
    }
}
