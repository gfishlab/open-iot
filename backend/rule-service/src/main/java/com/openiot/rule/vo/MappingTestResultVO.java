package com.openiot.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * 映射规则测试结果 VO
 *
 * @author open-iot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "映射规则测试结果")
public class MappingTestResultVO {

    /**
     * 是否映射成功
     */
    @Schema(description = "是否映射成功")
    private Boolean success;

    /**
     * 映射结果（key=物模型属性名，value=转换后的值）
     */
    @Schema(description = "映射结果（物模型属性映射）")
    private Map<String, Object> result;

    /**
     * 未映射的源字段列表
     */
    @Schema(description = "未映射的源字段列表")
    private Set<String> unmappedFields;

    /**
     * 错误信息（映射失败时）
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 映射耗时（毫秒）
     */
    @Schema(description = "映射耗时（毫秒）")
    private Long durationMs;

    /**
     * 创建成功结果
     */
    public static MappingTestResultVO success(Map<String, Object> result, Set<String> unmappedFields, Long durationMs) {
        return MappingTestResultVO.builder()
                .success(true)
                .result(result)
                .unmappedFields(unmappedFields)
                .durationMs(durationMs)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static MappingTestResultVO fail(String errorMessage, Long durationMs) {
        return MappingTestResultVO.builder()
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}
