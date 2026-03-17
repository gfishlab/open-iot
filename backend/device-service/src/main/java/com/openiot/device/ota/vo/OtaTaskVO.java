package com.openiot.device.ota.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 任务 VO（列表展示用）
 *
 * @author open-iot
 */
@Data
public class OtaTaskVO {

    /** 任务ID */
    private Long id;

    /** 任务名称 */
    private String taskName;

    /** 产品ID */
    private Long productId;

    /** 固件版本ID */
    private Long firmwareVersionId;

    /** 目标固件版本号 */
    private String targetVersion;

    /** 升级范围 */
    private String upgradeScope;

    /** 总设备数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failedCount;

    /** 任务状态 */
    private String taskStatus;

    /** 推送策略 */
    private String strategy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
