package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务调用记录实体
 *
 * @author OpenIoT Team
 */
@Data
@TableName(value = "service_call_record", autoResultMap = true)
public class ServiceCallRecord {

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
     * 设备ID
     */
    private Long deviceId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 服务标识符
     */
    private String serviceIdentifier;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 输入参数
     */
    @TableField(value = "input_params", typeHandler = JsonbTypeHandler.class)
    private JsonNode inputParams;

    /**
     * 输出参数
     */
    @TableField(value = "output_params", typeHandler = JsonbTypeHandler.class)
    private JsonNode outputParams;

    /**
     * 调用方式：sync-同步，async-异步
     */
    private String callType;

    /**
     * 状态：pending-待调用，calling-调用中，success-成功，failed-失败，timeout-超时
     */
    private String status;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 调用时间
     */
    private LocalDateTime callTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 删除标记
     */
    @TableLogic
    private String deleteFlag;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
