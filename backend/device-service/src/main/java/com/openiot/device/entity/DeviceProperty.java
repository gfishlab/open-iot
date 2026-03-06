package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备属性实体类（当前状态）
 *
 * @author open-iot
 */
@Data
@TableName("device_property")
public class DeviceProperty {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long deviceId;

    private String propertyIdentifier;

    private String propertyValue;

    private String dataType;

    private LocalDateTime updateTime;
}
