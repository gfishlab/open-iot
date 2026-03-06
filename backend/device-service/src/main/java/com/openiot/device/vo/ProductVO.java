package com.openiot.device.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品 VO
 *
 * @author open-iot
 */
@Data
public class ProductVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品密钥（租户内唯一）
     */
    private String productKey;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品类型（DEVICE/GATEWAY）
     */
    private String productType;

    /**
     * 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）
     */
    private String protocolType;

    /**
     * 节点类型（DIRECT/GATEWAY）
     */
    private String nodeType;

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    private String dataFormat;

    /**
     * 物模型定义（JSON格式）
     */
    private JsonNode thingModel;

    /**
     * 产品描述
     */
    private String description;

    /**
     * 状态（1=启用，0=禁用）
     */
    private String status;

    /**
     * 关联设备数量
     */
    private Integer deviceCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
