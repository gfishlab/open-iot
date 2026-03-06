package com.openiot.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 转发规则实体类
 *
 * @author open-iot
 */
@Data
@TableName("forward_rule")
public class ForwardRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long productId;

    private String ruleName;

    private String ruleSql;

    private String targetType;

    @TableField(typeHandler = com.openiot.common.core.handler.JsonbTypeHandler.class)
    private String targetConfig;

    private String status;

    @TableLogic
    private String deleteFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
