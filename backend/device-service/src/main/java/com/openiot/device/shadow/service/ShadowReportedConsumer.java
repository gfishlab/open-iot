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
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备影子 Reported 消费者
 * <p>
 * 监听 device-events 主题中的 SHADOW_REPORTED 事件，
 * 将设备上报的 reported 数据更新到设备影子表，
 * 并重新计算 delta（desired - reported）。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowReportedConsumer {

    private final DeviceShadowMapper deviceShadowMapper;
    private final DeviceMapper deviceMapper;
    private final ObjectMapper objectMapper;

    /** 乐观锁重试最大次数 */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 消费设备影子 Reported 事件
     * <p>
     * 过滤 eventType == SHADOW_REPORTED 的事件，
     * 提取设备上报的 reported 数据，更新到设备影子表。
     * 使用乐观锁（version）防止并发冲突，失败时自动重试。
     * </p>
     *
     * @param record Kafka 消息记录（EventEnvelope 格式）
     * @param ack    手动确认句柄
     */
    @KafkaListener(topics = "device-events", groupId = "device-shadow-reported")
    public void consumeShadowReported(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();

        // 仅处理 SHADOW_REPORTED 类型事件，其余跳过
        if (!EventEnvelope.EVENT_TYPE_SHADOW_REPORTED.equals(event.getEventType())) {
            ack.acknowledge();
            return;
        }

        // EventEnvelope 中的 deviceId 实际为 deviceKey（MQTT clientId）
        String deviceKey = event.getDeviceId();
        log.info("[Shadow] 收到设备 Reported 上报: deviceKey={}, eventId={}", deviceKey, event.getEventId());

        try {
            // 提取上报的 reported 数据
            Map<String, Object> reported = extractReportedData(event);
            if (reported == null || reported.isEmpty()) {
                log.warn("[Shadow] Reported 数据为空，跳过: deviceKey={}", deviceKey);
                ack.acknowledge();
                return;
            }

            // 通过 deviceKey 查询设备记录
            Device device = findDeviceByKey(deviceKey);
            if (device == null) {
                log.warn("[Shadow] 设备不存在，跳过 reported 更新: deviceKey={}", deviceKey);
                ack.acknowledge();
                return;
            }

            // 使用乐观锁更新 reported 和 delta
            boolean success = updateReportedWithRetry(device.getId(), reported);
            if (success) {
                log.info("[Shadow] 设备 Reported 更新成功: deviceKey={}, deviceId={}", deviceKey, device.getId());
            } else {
                log.error("[Shadow] 设备 Reported 更新失败（乐观锁冲突超过重试次数）: deviceKey={}", deviceKey);
            }

            // 确认消息消费
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[Shadow] 处理设备 Reported 事件异常: deviceKey={}, eventId={}", deviceKey, event.getEventId(), e);
            // 不确认，触发 Kafka 重试
        }
    }

    /**
     * 从 EventEnvelope 的 payload 中提取 reported 数据
     *
     * @param event 事件信封
     * @return reported 属性 Map，提取失败返回 null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractReportedData(EventEnvelope event) {
        Object payload = event.getPayload();
        if (payload instanceof Map) {
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            // 优先从 "reported" 字段提取
            Object reported = payloadMap.get("reported");
            if (reported instanceof Map) {
                return (Map<String, Object>) reported;
            }
            // 如果没有 "reported" 字段，整个 payload 视为 reported 数据
            return payloadMap;
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
     * 使用乐观锁更新 reported 并重新计算 delta，支持自动重试
     *
     * @param deviceId 设备 ID
     * @param reported 新上报的 reported 数据
     * @return true-更新成功，false-重试次数用尽仍失败
     */
    private boolean updateReportedWithRetry(Long deviceId, Map<String, Object> reported) {
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                // 查询当前影子记录
                DeviceShadow shadow = findShadowByDeviceId(deviceId);
                if (shadow == null) {
                    log.warn("[Shadow] 设备影子记录不存在，跳过更新: deviceId={}", deviceId);
                    return false;
                }

                // 合并 reported：将新上报数据合并到已有 reported 中
                Map<String, Object> mergedReported = mergeReported(shadow.getReported(), reported);
                String reportedJson = objectMapper.writeValueAsString(mergedReported);

                // 重新计算 delta（desired - mergedReported）
                String deltaJson = computeDelta(shadow.getDesired(), mergedReported);

                // 使用乐观锁更新（version 校验）
                int rows = deviceShadowMapper.updateReportedWithVersion(
                        deviceId, reportedJson, deltaJson, shadow.getVersion());

                if (rows > 0) {
                    return true;
                }

                // 版本冲突，重试
                log.warn("[Shadow] 乐观锁冲突，重试第 {} 次: deviceId={}", retry + 1, deviceId);

            } catch (Exception e) {
                log.error("[Shadow] 更新 reported 异常，重试第 {} 次: deviceId={}", retry + 1, deviceId, e);
            }
        }
        return false;
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
     * 合并 reported 数据
     * <p>
     * 将新上报的属性合并到已有 reported 中，新值覆盖旧值。
     * </p>
     *
     * @param existingReportedJson 已有的 reported JSON 字符串
     * @param newReported          新上报的 reported 数据
     * @return 合并后的 reported Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeReported(String existingReportedJson, Map<String, Object> newReported) {
        Map<String, Object> merged = new LinkedHashMap<>();

        // 解析已有 reported
        if (existingReportedJson != null && !existingReportedJson.isBlank()) {
            try {
                Map<String, Object> existing = objectMapper.readValue(existingReportedJson, Map.class);
                merged.putAll(existing);
            } catch (Exception e) {
                log.warn("[Shadow] 解析已有 reported 失败: {}", existingReportedJson, e);
            }
        }

        // 新数据覆盖旧数据
        merged.putAll(newReported);
        return merged;
    }

    /**
     * 计算 delta（desired - reported）
     * <p>
     * 遍历 desired 中的属性，与 reported 对比，
     * 值不同的属性加入 delta。如果 desired 为空则 delta 为空。
     * </p>
     *
     * @param desiredJson 期望属性 JSON 字符串
     * @param reported    当前 reported 数据
     * @return delta JSON 字符串
     */
    @SuppressWarnings("unchecked")
    private String computeDelta(String desiredJson, Map<String, Object> reported) {
        try {
            // 如果 desired 为空，delta 也为空
            if (desiredJson == null || desiredJson.isBlank()
                    || desiredJson.trim().equals("{}") || desiredJson.trim().equals("null")) {
                return "{}";
            }

            Map<String, Object> desired = objectMapper.readValue(desiredJson, Map.class);
            Map<String, Object> delta = new LinkedHashMap<>();

            // 遍历 desired 属性，找出与 reported 不同的属性
            for (Map.Entry<String, Object> entry : desired.entrySet()) {
                String key = entry.getKey();
                Object desiredValue = entry.getValue();
                Object reportedValue = reported.get(key);

                // 如果 reported 中没有该属性，或值不同，加入 delta
                if (reportedValue == null || !desiredValue.equals(reportedValue)) {
                    delta.put(key, desiredValue);
                }
            }

            return objectMapper.writeValueAsString(delta);

        } catch (Exception e) {
            log.error("[Shadow] 计算 delta 失败: desired={}", desiredJson, e);
            return "{}";
        }
    }
}
