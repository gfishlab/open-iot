package com.openiot.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备 DTO - 跨服务传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 产品ID */
    private Long productId;

    /** 设备编码 */
    private String deviceCode;

    /** 设备唯一标识 */
    private String deviceKey;

    /** 设备名称 */
    private String deviceName;

    /** 协议类型：MQTT/TCP/HTTP */
    private String protocolType;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
