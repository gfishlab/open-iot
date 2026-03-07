package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 映射规则创建请求 VO
 *
 * <p>映射规则用于将解析后的原始字段映射到物模型属性，
 * 支持字段重命名、单位转换、公式计算等转换函数。</p>
 *
 * <p>映射配置格式示例：
 * <pre>
 * {
 *   "mappings": [
 *     {
 *       "sourceField": "temp",           // 源字段名（解析规则输出）
 *       "targetProperty": "temperature", // 目标属性名（物模型属性）
 *       "transformType": "FORMULA",      // 转换类型：NONE/UNIT_CONVERT/FORMULA
 *       "transformConfig": "value * 1.8 + 32"  // 转换配置（公式表达式）
 *     },
 *     {
 *       "sourceField": "humidity",
 *       "targetProperty": "humidity",
 *       "transformType": "NONE"          // 直接赋值
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author open-iot
 */
@Data
@Schema(description = "映射规则创建请求")
public class MappingRuleCreateVO {

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    /**
     * 字段映射配置（JSON格式）
     *
     * <p>配置格式：
     * <pre>
     * {
     *   "mappings": [
     *     {
     *       "sourceField": "源字段名",
     *       "targetProperty": "目标属性名",
     *       "transformType": "转换类型",
     *       "transformConfig": "转换配置"
     *     }
     *   ]
     * }
     * </pre>
     *
     * <p>转换类型说明：
     * <ul>
     *   <li>NONE: 直接赋值，无需转换</li>
     *   <li>UNIT_CONVERT: 单位转换（如摄氏度转华氏度）</li>
     *   <li>FORMULA: 公式计算（支持 Aviator 表达式）</li>
     * </ul>
     */
    @NotBlank(message = "映射配置不能为空")
    @Schema(description = "字段映射配置（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldMappings;

    /**
     * 状态（1=启用，0=禁用）
     */
    @Schema(description = "状态：1-启用，0-禁用（默认为1）")
    private String status;
}
