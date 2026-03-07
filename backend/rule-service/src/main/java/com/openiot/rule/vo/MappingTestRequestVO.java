package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 映射规则测试请求 VO
 *
 * @author open-iot
 */
@Data
@Schema(description = "映射规则测试请求")
public class MappingTestRequestVO {

    /**
     * 解析后的原始数据（JSON格式）
     *
     * <p>这是解析规则输出的数据，格式示例：
     * <pre>
     * {
     *   "temp": 25.5,
     *   "humidity": 60,
     *   "pressure": 1013.25
     * }
     * </pre>
     */
    @NotBlank(message = "测试数据不能为空")
    @Schema(description = "解析后的原始数据（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String parsedData;

    /**
     * 可选：直接传入映射配置进行测试（不使用已保存的规则）
     *
     * <p>如果提供此字段，将使用此配置而非规则中的配置进行测试
     */
    @Schema(description = "可选：自定义映射配置（用于临时测试）")
    private String fieldMappings;
}
