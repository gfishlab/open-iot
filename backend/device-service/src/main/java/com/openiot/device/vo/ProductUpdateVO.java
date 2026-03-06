package com.openiot.device.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品更新 VO
 *
 * @author open-iot
 */
@Data
public class ProductUpdateVO {

    /**
     * 产品名称
     */
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
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
     * 产品描述
     */
    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    private String description;

    /**
     * 状态（1=启用，0=禁用）
     */
    private String status;
}
