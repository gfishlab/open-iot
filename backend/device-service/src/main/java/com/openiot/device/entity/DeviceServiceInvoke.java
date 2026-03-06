package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备服务调用实体类
 *
 * @author open-iot
 */
@Data
@TableName("device_service_invoke")
public class DeviceServiceInvoke {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long deviceId;

    private String serviceIdentifier;

    private String invokeId;

    private String invokeType;

    @TableField(typeHandler = com.openiot.common.core.handler.JsonbTypeHandler.class)
    private String inputData;

    @TableField(typeHandler = com.openiot.common.core.handler.JsonbTypeHandler.class)
    private String outputData;

    private String status;

    private String errorMessage;

    private LocalDateTime invokeTime;

    private LocalDateTime completeTime;
}
