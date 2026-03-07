package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 属性设置记录实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "property_set_record", autoResultMap = true)
public class PropertySetRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long deviceId;
    private Long productId;
    private String propertyIdentifier;
    private String propertyName;

    @TableField(value = "old_value", typeHandler = JsonbTypeHandler.class)
    private JsonNode oldValue;

    @TableField(value = "new_value", typeHandler = JsonbTypeHandler.class)
    private JsonNode newValue;

    /**
     * 设置类型：user-用户设置，system-系统设置
     */
    private String setType;

    /**
     * 状态：pending-待设置，success-成功，failed-失败
     */
    private String status;

    private String errorMessage;
    private LocalDateTime createTime;
}
