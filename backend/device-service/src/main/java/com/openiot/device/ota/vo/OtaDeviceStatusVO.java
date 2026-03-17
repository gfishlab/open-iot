package com.openiot.device.ota.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 设备升级状态 VO
 *
 * @author open-iot
 */
@Data
public class OtaDeviceStatusVO {

    /** 记录ID */
    private Long id;

    /** 设备ID */
    private Long deviceId;

    /** 升级状态 */
    private String upgradeStatus;

    /** 进度百分比 */
    private Integer progress;

    /** 当前固件版本 */
    private String currentVersion;

    /** 目标固件版本 */
    private String targetVersion;

    /** 已下载字节数 */
    private Long downloadedBytes;

    /** 错误码 */
    private String errorCode;

    /** 错误详情 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime finishTime;
}
