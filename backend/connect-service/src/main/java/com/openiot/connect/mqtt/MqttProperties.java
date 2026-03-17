package com.openiot.connect.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT 客户端配置属性
 * <p>
 * 配置 connect-service 内嵌 MQTT Client 连接 EMQX Broker 的参数。
 * 用于向设备下发指令（属性设置、服务调用、OTA 通知、影子同步等）。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "openiot.mqtt")
public class MqttProperties {

    /** EMQX Broker 地址 */
    private String host = "127.0.0.1";

    /** EMQX Broker 端口 */
    private int port = 1883;

    /** 客户端ID前缀（实际ID = 前缀 + 随机后缀，避免多实例冲突） */
    private String clientIdPrefix = "connect-service-";

    /** 连接用户名（平台内部连接，非设备认证） */
    private String username = "";

    /** 连接密码 */
    private String password = "";

    /** 自动重连 */
    private boolean autoReconnect = true;

    /** 初始重连延迟（秒） */
    private long reconnectDelaySeconds = 1;

    /** 最大重连延迟（秒） */
    private long maxReconnectDelaySeconds = 30;

    /** 保持连接间隔（秒） */
    private int keepAliveSeconds = 60;

    /** 连接超时时间（秒） */
    private int connectTimeoutSeconds = 10;

    /** 是否启用 MQTT Client */
    private boolean enabled = true;
}
