package com.openiot.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品实体类
 *
 * @author open-iot
 * @since 2026-03-06
 */
@Data
@TableName(value = "product", autoResultMap = true)
public class Product {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 产品密钥（租户内唯一，如 PROD_A1B2C3）
     */
    @TableField("product_key")
    private String productKey;

    /**
     * 产品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 产品类型（DEVICE/GATEWAY）
     */
    @TableField("product_type")
    private String productType;

    /**
     * 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）
     */
    @TableField("protocol_type")
    private String protocolType;

    /**
     * 节点类型（DIRECT/GATEWAY）
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    @TableField("data_format")
    private String dataFormat;

    /**
     * 物模型定义（JSON格式）
     */
    @TableField(value = "thing_model", typeHandler = JsonbTypeHandler.class)
    private JsonNode thingModel;

    /**
     * 状态（'1'=启用，'0'=禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 删除标记（'0'=未删除，'1'=已删除）
     */
    @TableField("delete_flag")
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

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
