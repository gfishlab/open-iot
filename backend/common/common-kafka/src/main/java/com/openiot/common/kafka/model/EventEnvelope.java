package com.openiot.common.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 统一事件信封
 * 所有协议数据统一转换为此格式后进入 Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识（UUID）
     */
    private String eventId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 事件类型
     * TELEMETRY - 遥测数据
     * STATUS - 状态数据
     * ALARM - 告警数据
     */
    private String eventType;

    /**
     * 协议类型
     * MQTT - MQTT 协议
     * TCP - TCP 私有协议
     * HTTP - HTTP 推送
     */
    private String protocol;

    /**
     * 解析后的业务数据（JSON 格式）
     */
    private Object payload;

    /**
     * 原始载荷（Base64 编码）
     */
    private String rawPayload;

    /**
     * 事件时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    // ==================== 事件类型常量 ====================

    public static final String EVENT_TYPE_TELEMETRY = "TELEMETRY";
    public static final String EVENT_TYPE_STATUS = "STATUS";
    public static final String EVENT_TYPE_ALARM = "ALARM";

    // OTA 升级相关事件类型
    public static final String EVENT_TYPE_OTA_UPGRADE = "OTA_UPGRADE";
    public static final String EVENT_TYPE_OTA_PROGRESS = "OTA_PROGRESS";

    // 设备影子相关事件类型
    public static final String EVENT_TYPE_SHADOW_DELTA = "SHADOW_DELTA";
    public static final String EVENT_TYPE_SHADOW_GET = "SHADOW_GET";
    public static final String EVENT_TYPE_SHADOW_REPORTED = "SHADOW_REPORTED";

    // ==================== 协议类型常量 ====================

    public static final String PROTOCOL_MQTT = "MQTT";
    public static final String PROTOCOL_TCP = "TCP";
    public static final String PROTOCOL_HTTP = "HTTP";

    /**
     * 获取 Kafka 消息 Key
     * 格式: tenantId:deviceId
     */
    public String getKafkaKey() {
        return tenantId + ":" + deviceId;
    }

    /**
     * 获取设备编码（deviceId 的别名）
     */
    public String getDeviceCode() {
        return deviceId;
    }

    /**
     * 获取产品ID（从 payload 中提取，如果存在）
     */
    @SuppressWarnings("unchecked")
    public String getProductId() {
        // payload 可能是 Map 类型
        if (payload instanceof Map) {
            Object productId = ((Map<?, ?>) payload).get("productId");
            return productId != null ? productId.toString() : null;
        }
        return null;
    }
}
