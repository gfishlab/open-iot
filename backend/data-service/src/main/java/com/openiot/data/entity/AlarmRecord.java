package com.openiot.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录实体
 * 记录触发的告警信息
 *
 * @author OpenIoT Team
 */
@Data
@TableName("alarm_record")
public class AlarmRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 告警规则ID
     */
    private Long ruleId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 告警级别
     */
    private String alarmLevel;

    /**
     * 告警标题
     */
    private String alarmTitle;

    /**
     * 告警内容
     */
    private String alarmContent;

    /**
     * 触发值
     */
    private String triggerValue;

    /**
     * 告警状态：pending-active-acknowledged-resolved-closed
     */
    private String alarmStatus;

    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;

    /**
     * 恢复时间
     */
    private LocalDateTime recoverTime;

    /**
     * 确认时间
     */
    private LocalDateTime acknowledgeTime;

    /**
     * 确认人ID
     */
    private Long acknowledgedBy;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 通知状态：none-sending-sent-failed
     */
    private String notifyStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
