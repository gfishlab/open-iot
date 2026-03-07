package com.openiot.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据转发配置实体
 * 用于配置设备数据转发到外部系统的规则
 *
 * @author OpenIoT Team
 */
@Data
@TableName("data_forward_config")
public class DataForwardConfig {

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
     * 转发配置名称
     */
    private String configName;

    /**
     * 转发目标类型：http-kafka-mqtt-influxdb-mongodb
     */
    private String targetType;

    /**
     * 目标端点配置（JSON格式）
     * HTTP: {"url": "http://api.example.com/data", "method": "POST", "headers": {}}
     * Kafka: {"topic": "forward-topic", "bootstrapServers": "localhost:9092"}
     * MQTT: {"topic": "forward/topic", "broker": "tcp://localhost:1883"}
     */
    private String endpointConfig;

    /**
     * 数据过滤条件（JSON格式）
     * {"deviceIds": [1,2,3], "propertyIdentifiers": ["temp", "humidity"]}
     */
    private String filterCondition;

    /**
     * 数据转换规则（JSON格式）
     * {"template": "${deviceId}-${propertyIdentifier}=${value}"}
     */
    private String transformRule;

    /**
     * 批量大小
     */
    private Integer batchSize;

    /**
     * 批量超时时间（毫秒）
     */
    private Integer batchTimeoutMs;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 重试间隔（毫秒）
     */
    private Integer retryIntervalMs;

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
