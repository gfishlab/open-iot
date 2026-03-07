package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "alert_record", autoResultMap = true)
public class AlertRecord {

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
     * 设备ID
     */
    private Long deviceId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 告警级别：info, warning, critical
     */
    private String alertLevel;

    /**
     * 告警标题
     */
    private String alertTitle;

    /**
     * 告警内容
     */
    private String alertContent;

    /**
     * 告警数据（触发时的原始数据）
     */
    @TableField(value = "alert_data", typeHandler = JsonbTypeHandler.class)
    private JsonNode alertData;

    /**
     * 处理状态：pending-待处理，processing-处理中，resolved-已解决，ignored-已忽略
     */
    private String status;

    /**
     * 处理时间
     */
    private LocalDateTime handledTime;

    /**
     * 处理人ID
     */
    private Long handledBy;

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
}
