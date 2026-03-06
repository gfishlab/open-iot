package com.openiot.device.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品创建 VO
 *
 * @author open-iot
 */
@Data
public class ProductCreateVO {

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    private String productName;

    /**
     * 产品类型（DEVICE/GATEWAY）
     */
    @NotBlank(message = "产品类型不能为空")
    private String productType;

    /**
     * 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）
     */
    @NotBlank(message = "协议类型不能为空")
    private String protocolType;

    /**
     * 节点类型（DIRECT/GATEWAY）
     */
    private String nodeType = "DIRECT";

    /**
     * 数据格式（JSON/XML/BINARY/CUSTOM）
     */
    private String dataFormat = "JSON";

    /**
     * 产品描述
     */
    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    private String description;
}
