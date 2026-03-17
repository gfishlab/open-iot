package com.openiot.connect.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HiveMQ MQTT Client 管理器
 * <p>
 * 管理 connect-service 与 EMQX Broker 的 MQTT 连接生命周期。
 * 用于向设备下发指令（发布消息到设备订阅的 Topic）。
 * 支持自动重连、健康检查、优雅关闭。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttClientManager implements HealthIndicator {

    private final MqttProperties mqttProperties;

    /** HiveMQ MQTT 3.1.1 异步客户端 */
    private Mqtt3AsyncClient mqttClient;

    /** 连接状态标记 */
    private volatile boolean connected = false;

    /**
     * 初始化 MQTT 客户端并连接 Broker
     */
    @PostConstruct
    public void init() {
        if (!mqttProperties.isEnabled()) {
            log.info("[MQTT] MQTT Client 已禁用，跳过初始化");
            return;
        }

        String clientId = mqttProperties.getClientIdPrefix() + UUID.randomUUID().toString().substring(0, 8);
        log.info("[MQTT] 初始化 MQTT Client, broker={}:{}, clientId={}",
                mqttProperties.getHost(), mqttProperties.getPort(), clientId);

        // 构建 HiveMQ MQTT 客户端
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(mqttProperties.getHost())
                .serverPort(mqttProperties.getPort())
                // 自动重连配置（指数退避）
                .automaticReconnectWithDefaultConfig()
                .buildAsync();

        // 异步连接
        connect();
    }

    /**
     * 连接到 EMQX Broker
     */
    private void connect() {
        try {
            CompletableFuture<Mqtt3ConnAck> future = mqttClient.connectWith()
                    .keepAlive(mqttProperties.getKeepAliveSeconds())
                    .cleanSession(true)
                    .send();

            future.thenAccept(connAck -> {
                connected = true;
                log.info("[MQTT] 成功连接到 EMQX Broker, returnCode={}", connAck.getReturnCode());
            }).exceptionally(throwable -> {
                connected = false;
                log.error("[MQTT] 连接 EMQX Broker 失败: {}", throwable.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("[MQTT] 连接 EMQX 异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 向指定 Topic 发布消息
     *
     * @param topic   MQTT Topic
     * @param payload 消息内容（JSON 字符串）
     * @param qos     QoS 等级（0/1/2）
     * @return 发布结果的 CompletableFuture
     */
    public CompletableFuture<Void> publish(String topic, String payload, int qos) {
        if (mqttClient == null || !connected) {
            log.warn("[MQTT] 客户端未连接，无法发布消息到 topic={}", topic);
            return CompletableFuture.failedFuture(new IllegalStateException("MQTT 客户端未连接"));
        }

        MqttQos mqttQos = qos == 0 ? MqttQos.AT_MOST_ONCE :
                           qos == 1 ? MqttQos.AT_LEAST_ONCE : MqttQos.EXACTLY_ONCE;

        return mqttClient.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .qos(mqttQos)
                .send()
                .thenAccept(publish -> {
                    log.debug("[MQTT] 消息已发布, topic={}, qos={}, payloadSize={}",
                            topic, qos, payload.length());
                })
                .exceptionally(throwable -> {
                    log.error("[MQTT] 消息发布失败, topic={}: {}", topic, throwable.getMessage());
                    return null;
                });
    }

    /**
     * 向设备下发指令
     * <p>
     * 便捷方法：自动构造设备 Topic 并发布消息。
     * Topic 格式: /devices/{deviceKey}/{domain}/{action}
     * </p>
     *
     * @param deviceKey 设备唯一标识
     * @param domain    功能域（properties/services/shadow/ota）
     * @param action    操作方向（set/invoke/update/notify）
     * @param payload   消息内容（JSON 字符串）
     * @return 发布结果
     */
    public CompletableFuture<Void> publishCommand(String deviceKey, String domain, String action, String payload) {
        String topic = String.format("/devices/%s/%s/%s", deviceKey, domain, action);
        return publish(topic, payload, 1);
    }

    /**
     * 优雅关闭 MQTT 连接
     */
    @PreDestroy
    public void destroy() {
        if (mqttClient != null) {
            log.info("[MQTT] 正在断开 EMQX 连接...");
            try {
                mqttClient.disconnect().get(5, TimeUnit.SECONDS);
                connected = false;
                log.info("[MQTT] EMQX 连接已断开");
            } catch (Exception e) {
                log.warn("[MQTT] 断开连接异常: {}", e.getMessage());
            }
        }
    }

    /**
     * Spring Boot Actuator 健康检查
     */
    @Override
    public Health health() {
        if (!mqttProperties.isEnabled()) {
            return Health.up().withDetail("mqtt", "disabled").build();
        }
        if (connected) {
            return Health.up()
                    .withDetail("broker", mqttProperties.getHost() + ":" + mqttProperties.getPort())
                    .withDetail("status", "connected")
                    .build();
        }
        return Health.down()
                .withDetail("broker", mqttProperties.getHost() + ":" + mqttProperties.getPort())
                .withDetail("status", "disconnected")
                .build();
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return connected;
    }
}
