package com.openiot.device.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备创建 VO
 *
 * @author open-iot
 */
@Data
public class DeviceCreateVO {

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 设备编码
     */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50个字符")
    private String deviceCode;

    /**
     * 设备名称
     */
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    /**
     * 协议类型（MQTT/TCP/HTTP）
     */
    @NotBlank(message = "协议类型不能为空")
    private String protocolType;
}
