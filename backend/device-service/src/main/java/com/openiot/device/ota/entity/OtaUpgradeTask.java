package com.openiot.device.ota.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 升级任务实体
 *
 * @author open-iot
 */
@Data
@TableName(value = "ota_upgrade_task", autoResultMap = true)
public class OtaUpgradeTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 任务名称 */
    private String taskName;

    /** 目标产品ID */
    private Long productId;

    /** 目标固件版本ID */
    private Long firmwareVersionId;

    /** 升级范围：ALL-全部 SELECTED-指定设备 */
    private String upgradeScope;

    /** 指定设备ID列表（JSONB数组） */
    @TableField(value = "target_device_ids", typeHandler = JsonbTypeHandler.class)
    private JsonNode targetDeviceIds;

    /** 总设备数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failedCount;

    /** 任务状态：created/running/paused/completed/cancelled */
    private String taskStatus;

    /** 推送策略：immediate/scheduled/staged */
    private String strategy;

    /** 定时执行时间 */
    private LocalDateTime scheduledTime;

    /** 分批大小 */
    private Integer batchSize;

    /** 最大重试次数 */
    private Integer retryCount;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 删除标志：0-正常 1-删除 */
    @TableLogic
    private String delFlag;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    private Long createBy;

    /** 更新人 */
    private Long updateBy;
}
