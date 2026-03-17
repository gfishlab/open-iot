package com.openiot.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品 DTO - 跨服务传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 产品ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 产品密钥 */
    private String productKey;

    /** 产品名称 */
    private String productName;

    /** 产品类型：DEVICE/GATEWAY */
    private String productType;

    /** 协议类型：MQTT/HTTP/CoAP/LwM2M/CUSTOM */
    private String protocolType;

    /** 数据格式：JSON/XML/BINARY/CUSTOM */
    private String dataFormat;

    /** 产品描述 */
    private String description;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
