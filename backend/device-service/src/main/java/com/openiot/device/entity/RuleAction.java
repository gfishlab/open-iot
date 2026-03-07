package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则动作实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "rule_action", autoResultMap = true)
public class RuleAction {

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
     * 动作类型：alert-告警，webhook-回调，device-设备控制
     */
    private String actionType;

    /**
     * 动作配置（JSON格式）
     */
    @TableField(value = "action_config", typeHandler = JsonbTypeHandler.class)
    private JsonNode actionConfig;

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
