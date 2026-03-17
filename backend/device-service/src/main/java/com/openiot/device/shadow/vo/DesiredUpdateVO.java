package com.openiot.device.shadow.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 期望属性更新 VO
 *
 * @author open-iot
 */
@Data
public class DesiredUpdateVO {

    /** 期望属性（JSON 字符串） */
    @NotNull(message = "期望属性不能为空")
    private String desired;

    /** 期望的版本号（用于乐观锁） */
    @NotNull(message = "版本号不能为空")
    private Long version;
}
