package com.openiot.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据转发日志实体
 * 记录数据转发的执行结果
 *
 * @author OpenIoT Team
 */
@Data
@TableName("data_forward_log")
public class DataForwardLog {

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
     * 转发配置ID
     */
    private Long configId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 数据类型：property-event-status
     */
    private String dataType;

    /**
     * 转发目标
     */
    private String target;

    /**
     * 转发状态：success-fail-pending
     */
    private String forwardStatus;

    /**
     * 响应内容
     */
    private String response;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 耗时（毫秒）
     */
    private Long duration;

    /**
     * 转发时间
     */
    private LocalDateTime forwardTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
