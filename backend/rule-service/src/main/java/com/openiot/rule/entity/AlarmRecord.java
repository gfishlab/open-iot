package com.openiot.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录实体类
 *
 * @author open-iot
 */
@Data
@TableName("alarm_record")
public class AlarmRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long alarmRuleId;

    private Long deviceId;

    private String alarmLevel;

    private String alarmTitle;

    private String alarmContent;

    private String status;

    private LocalDateTime triggerTime;

    private LocalDateTime recoverTime;

    private LocalDateTime ackTime;

    private Long ackBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
