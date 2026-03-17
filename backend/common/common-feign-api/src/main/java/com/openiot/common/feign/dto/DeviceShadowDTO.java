package com.openiot.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备影子 DTO - 跨服务传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceShadowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备上报属性（JSON 字符串） */
    private String reported;

    /** 期望属性（JSON 字符串） */
    private String desired;

    /** 差异值（JSON 字符串） */
    private String delta;

    /** 乐观锁版本号 */
    private Long version;

    /** 最后 reported 更新时间 */
    private LocalDateTime reportedTime;

    /** 最后 desired 更新时间 */
    private LocalDateTime desiredTime;
}
