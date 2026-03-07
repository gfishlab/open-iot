package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命令下发记录实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "command_send_record", autoResultMap = true)
public class CommandSendRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long deviceId;
    private Long productId;
    private String commandIdentifier;
    private String commandName;

    @TableField(value = "command_params", typeHandler = JsonbTypeHandler.class)
    private JsonNode commandParams;

    /**
     * 状态：pending-待下发，sent-已发送，success-成功，failed-失败，timeout-超时
     */
    private String status;

    @TableField(value = "response_data", typeHandler = JsonbTypeHandler.class)
    private JsonNode responseData;

    private String errorMessage;
    private LocalDateTime sendTime;
    private LocalDateTime responseTime;

    @TableLogic
    private String deleteFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
