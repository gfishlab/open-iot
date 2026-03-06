package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备事件实体类
 *
 * @author open-iot
 */
@Data
@TableName("device_event")
public class DeviceEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long deviceId;

    private String eventIdentifier;

    private String eventType;

    @TableField(typeHandler = com.openiot.common.core.handler.JsonbTypeHandler.class)
    private String eventData;

    private LocalDateTime eventTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
