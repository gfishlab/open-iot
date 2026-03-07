package com.openiot.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 * 用于配置设备告警触发条件
 *
 * @author OpenIoT Team
 */
@Data
@TableName("alarm_rule")
public class AlarmRule {

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
     * 产品ID（可选，为空则适用所有产品）
     */
    private Long productId;

    /**
     * 设备ID（可选，为空则适用所有设备）
     */
    private Long deviceId;

    /**
     * 告警规则名称
     */
    private String ruleName;

    /**
     * 告警级别：info-warning-critical-emergency
     */
    private String alarmLevel;

    /**
     * 触发条件类型：threshold-expression-rate
     */
    private String triggerType;

    /**
     * 触发条件配置（JSON格式）
     * threshold: {"property": "temp", "operator": ">", "value": 50}
     * expression: "temp > 50 && humidity < 20"
     * rate: {"property": "temp", "change": 10, "window": "1m"}
     */
    private String triggerCondition;

    /**
     * 持续时间（秒），超过此时间才触发告警
     */
    private Integer durationSeconds;

    /**
     * 告警内容模板
     */
    private String contentTemplate;

    /**
     * 恢复条件（可选）
     */
    private String recoveryCondition;

    /**
     * 通知方式：email-sms-webhook-none
     */
    private String notifyType;

    /**
     * 通知配置（JSON格式）
     * email: {"recipients": ["user@example.com"]}
     * webhook: {"url": "http://example.com/alarm"}
     */
    private String notifyConfig;

    /**
     * 是否启用静默期
     */
    private Boolean silenceEnabled;

    /**
     * 静默期时长（秒）
     */
    private Integer silenceSeconds;

    /**
     * 状态：0-禁用，1-启用
     */
    private String status;

    /**
     * 删除标记：0-正常，1-已删除
     */
    private String delFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
