package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则条件实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName("rule_condition")
public class RuleCondition {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 属性标识符
     */
    private String propertyIdentifier;

    /**
     * 操作符：gt, lt, eq, gte, lte, between
     */
    private String operator;

    /**
     * 阈值（JSON格式）
     */
    private String thresholdValue;

    /**
     * 条件顺序
     */
    private Integer conditionOrder;

    /**
     * 逻辑关系：AND, OR
     */
    private String logicRelation;

    /**
     * 删除标记：0-正常，1-已删除
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
}
