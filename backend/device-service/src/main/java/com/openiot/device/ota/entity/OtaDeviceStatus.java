package com.openiot.device.ota.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 设备升级状态实体
 *
 * @author open-iot
 */
@Data
@TableName("ota_device_status")
public class OtaDeviceStatus {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 升级任务ID */
    private Long taskId;

    /** 设备ID */
    private Long deviceId;

    /** 目标固件版本ID */
    private Long firmwareVersionId;

    /** 升级状态：pending/pushing/downloading/installing/success/failed */
    private String upgradeStatus;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 当前固件版本 */
    private String currentVersion;

    /** 目标固件版本 */
    private String targetVersion;

    /** 已下载字节数（断点续传） */
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

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
