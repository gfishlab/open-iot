package com.openiot.device.shadow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备影子 VO
 *
 * @author open-iot
 */
@Data
public class DeviceShadowVO {

    /** 设备ID */
    private Long deviceId;

    /** 设备上报属性（JSON 字符串） */
    private String reported;

    /** 期望属性（JSON 字符串） */
    private String desired;

    /** 差异值（JSON 字符串） */
    private String delta;

    /** 版本号 */
    private Long version;

    /** 最后 reported 更新时间 */
    private LocalDateTime reportedTime;

    /** 最后 desired 更新时间 */
    private LocalDateTime desiredTime;

    /** 元数据 */
    private String metadata;
}
