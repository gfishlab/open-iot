package com.openiot.device.shadow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.shadow.entity.DeviceShadow;
import com.openiot.device.shadow.mapper.DeviceShadowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 设备上线影子消费者
 * <p>
 * 监听 device-status-change 主题，当设备上线时，
 * 检查该设备的影子是否存在未同步的 delta（desired 与 reported 的差异）。
 * 如果 delta 非空，则发送 SHADOW_DELTA 事件到 Kafka，
 * 由 connect-service 的 ShadowDeltaConsumer 通过 MQTT 下发给设备。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOnlineShadowConsumer {

    private final DeviceShadowMapper deviceShadowMapper;
    private final DeviceMapper deviceMapper;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** 设备事件 Kafka 主题 */
    private static final String TOPIC_DEVICE_EVENTS = "device-events";

    /**
     * 消费设备状态变更事件
     * <p>
     * 过滤 payload 中 status == "online" 的事件。
     * 设备上线后，查询其影子 delta 是否非空，
     * 若存在未同步的 delta，则发送 SHADOW_DELTA 事件触发下发。
     * </p>
     *
     * @param record Kafka 消息记录（EventEnvelope 格式）
     * @param ack    手动确认句柄
     */
    @KafkaListener(topics = "device-status-change", groupId = "device-shadow-online")
    public void consumeDeviceStatusChange(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();

        // 从 payload 中提取设备状态
        String status = extractStatus(event);

        // 仅处理设备上线事件
        if (!"online".equals(status)) {
            ack.acknowledge();
            return;
        }

        // EventEnvelope 中的 deviceId 实际为 deviceKey（MQTT clientId）
        String deviceKey = event.getDeviceId();
        log.info("[Shadow] 设备上线，检查影子 delta: deviceKey={}", deviceKey);

        try {
            // 通过 deviceKey 查询设备记录，获取设备 ID
            Device device = findDeviceByKey(deviceKey);
            if (device == null) {
                log.warn("[Shadow] 设备不存在，跳过影子检查: deviceKey={}", deviceKey);
                ack.acknowledge();
                return;
            }

            // 查询设备影子
            DeviceShadow shadow = findShadowByDeviceId(device.getId());
            if (shadow == null) {
                log.debug("[Shadow] 设备影子不存在，跳过: deviceId={}", device.getId());
                ack.acknowledge();
                return;
            }

            // 检查 delta 是否非空（存在未同步的期望属性）
            if (isDeltaNotEmpty(shadow.getDelta())) {
                log.info("[Shadow] 检测到未同步 delta，发送 SHADOW_DELTA 事件: deviceKey={}, delta={}",
                        deviceKey, shadow.getDelta());

                // 构建 SHADOW_DELTA 事件并发送到 Kafka
                sendShadowDeltaEvent(event, device, shadow);
            } else {
                log.debug("[Shadow] 设备影子 delta 为空，无需下发: deviceKey={}", deviceKey);
            }

            // 确认消息消费
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[Shadow] 处理设备上线影子检查异常: deviceKey={}", deviceKey, e);
            // 不确认，触发 Kafka 重试
        }
    }

    /**
     * 从 EventEnvelope 的 payload 中提取设备状态
     *
     * @param event 事件信封
     * @return 设备状态（online/offline），提取失败返回 null
     */
    @SuppressWarnings("unchecked")
    private String extractStatus(EventEnvelope event) {
        Object payload = event.getPayload();
        if (payload instanceof Map) {
            Object status = ((Map<String, Object>) payload).get("status");
            return status != null ? status.toString() : null;
        }
        return null;
    }

    /**
     * 通过 deviceKey 查询设备记录
     *
     * @param deviceKey 设备唯一密钥
     * @return 设备实体，不存在返回 null
     */
    private Device findDeviceByKey(String deviceKey) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceKey, deviceKey)
               .last("LIMIT 1");
        return deviceMapper.selectOne(wrapper);
    }

    /**
     * 通过设备 ID 查询设备影子
     *
     * @param deviceId 设备 ID
     * @return 设备影子实体，不存在返回 null
     */
    private DeviceShadow findShadowByDeviceId(Long deviceId) {
        LambdaQueryWrapper<DeviceShadow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceShadow::getDeviceId, deviceId)
               .last("LIMIT 1");
        return deviceShadowMapper.selectOne(wrapper);
    }

    /**
     * 判断 delta 是否非空
     * <p>
     * delta 为 null、空字符串、"{}" 或 "null" 均视为空。
     * </p>
     *
     * @param delta delta JSON 字符串
     * @return true-非空，false-为空
     */
    private boolean isDeltaNotEmpty(String delta) {
        if (delta == null || delta.isBlank()) {
            return false;
        }
        String trimmed = delta.trim();
        return !trimmed.equals("{}") && !trimmed.equals("null");
    }

    /**
     * 构建并发送 SHADOW_DELTA 事件到 Kafka
     *
     * @param originalEvent 原始状态变更事件
     * @param device        设备实体
     * @param shadow        设备影子实体
     */
    private void sendShadowDeltaEvent(EventEnvelope originalEvent, Device device, DeviceShadow shadow) {
        try {
            // 构建 delta payload
            Map<String, Object> deltaPayload = new LinkedHashMap<>();
            // 将 delta JSON 字符串解析为 Map 放入 payload
            deltaPayload.put("delta", objectMapper.readValue(shadow.getDelta(), Map.class));
            deltaPayload.put("version", shadow.getVersion());
            deltaPayload.put("deviceId", device.getId());

            // 构建 SHADOW_DELTA 事件信封
            EventEnvelope deltaEvent = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .tenantId(originalEvent.getTenantId())
                    .deviceId(originalEvent.getDeviceId())  // deviceKey
                    .eventType(EventEnvelope.EVENT_TYPE_SHADOW_DELTA)
                    .protocol(EventEnvelope.PROTOCOL_MQTT)
                    .payload(deltaPayload)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发送到 device-events 主题
            kafkaTemplate.send(TOPIC_DEVICE_EVENTS, deltaEvent.getKafkaKey(), deltaEvent);
            log.info("[Shadow] SHADOW_DELTA 事件已发送: deviceKey={}, eventId={}",
                    originalEvent.getDeviceId(), deltaEvent.getEventId());

        } catch (Exception e) {
            log.error("[Shadow] 发送 SHADOW_DELTA 事件失败: deviceKey={}", originalEvent.getDeviceId(), e);
        }
    }
}
