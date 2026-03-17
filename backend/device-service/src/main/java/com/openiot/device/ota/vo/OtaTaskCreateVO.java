package com.openiot.device.ota.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OTA 任务创建 VO
 *
 * @author open-iot
 */
@Data
public class OtaTaskCreateVO {

    /** 任务名称 */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /** 目标产品ID */
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 目标固件版本ID */
    @NotNull(message = "固件版本ID不能为空")
    private Long firmwareVersionId;

    /** 升级范围：ALL-全部 SELECTED-指定设备 */
    private String upgradeScope = "ALL";

    /** 指定设备ID列表（upgradeScope=SELECTED 时必填） */
    private List<Long> targetDeviceIds;

    /** 推送策略：immediate/scheduled/staged */
    private String strategy = "immediate";

    /** 定时执行时间（strategy=scheduled 时必填） */
    private LocalDateTime scheduledTime;

    /** 分批大小 */
    private Integer batchSize = 100;

    /** 最大重试次数 */
    private Integer retryCount = 3;
}
