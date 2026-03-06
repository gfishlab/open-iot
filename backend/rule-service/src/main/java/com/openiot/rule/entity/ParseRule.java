package com.openiot.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析规则实体类
 *
 * @author open-iot
 */
@Data
@TableName("parse_rule")
public class ParseRule {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型（JSON/JAVASCRIPT/REGEX/BINARY）
     */
    private String ruleType;

    /**
     * 规则配置（JSON格式）
     */
    @TableField(typeHandler = com.openiot.common.core.handler.JsonbTypeHandler.class)
    private String ruleConfig;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

    /**
     * 状态（1=启用，0=禁用）
     */
    private String status;

    /**
     * 删除标记（0=未删除，1=已删除）
     */
    @TableLogic
    private String deleteFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
