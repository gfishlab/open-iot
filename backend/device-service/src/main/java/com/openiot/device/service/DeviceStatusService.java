package com.openiot.device.service;

import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备状态服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusService {

    private final DeviceService deviceService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String DEVICE_ONLINE_KEY_PREFIX = "device:online:";
    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";
    private static final String TENANT_ONLINE_KEY_PREFIX = "tenant:online:";
    private static final long ONLINE_TIMEOUT_SECONDS = 300; // 5分钟超时

    /**
     * 设备上线
     *
     * @param deviceCode 设备编码
     * @param clientId   MQTT 客户端ID
     */
    public void setOnline(String deviceCode, String clientId) {
        log.info("设备上线: deviceCode={}, clientId={}", deviceCode, clientId);

        // 查询设备
        Device device = deviceService.getByCode(deviceCode);
        if (device == null) {
            log.warn("设备不存在: deviceCode={}", deviceCode);
            return;
        }

        // 设置在线状态（带过期时间）
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + device.getId();
        String statusKey = DEVICE_STATUS_KEY_PREFIX + device.getId();
        String tenantKey = TENANT_ONLINE_KEY_PREFIX + device.getTenantId();

        // 保存设备在线状态
        Map<String, Object> status = new HashMap<>();
        status.put("deviceId", device.getId());
        status.put("deviceCode", deviceCode);
        status.put("clientId", clientId);
        status.put("onlineTime", System.currentTimeMillis());
        status.put("status", "online");

        redisTemplate.opsForValue().set(onlineKey, "online", ONLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForHash().putAll(statusKey, status);
        redisTemplate.expire(statusKey, Duration.ofSeconds(ONLINE_TIMEOUT_SECONDS));

        // 添加到租户在线设备集合
        redisTemplate.opsForSet().add(tenantKey, device.getId().toString());
        redisTemplate.expire(tenantKey, Duration.ofHours(1));

        // 发送状态变化消息
        publishStatusChange(device.getId(), "online");

        // 更新数据库设备状态
        updateDeviceStatus(device.getId(), "online");

        log.info("设备上线成功: deviceId={}, deviceCode={}", device.getId(), deviceCode);
    }

    /**
     * 设备离线
     *
     * @param deviceCode 设备编码
     */
    public void setOffline(String deviceCode) {
        log.info("设备离线: deviceCode={}", deviceCode);

        Device device = deviceService.getByCode(deviceCode);
        if (device == null) {
            log.warn("设备不存在: deviceCode={}", deviceCode);
            return;
        }

        setOfflineByDeviceId(device.getId());
    }

    /**
     * 设备离线（通过设备ID）
     *
     * @param deviceId 设备ID
     */
    public void setOfflineByDeviceId(Long deviceId) {
        log.info("设备离线: deviceId={}", deviceId);

        Device device = deviceService.getById(deviceId);
        if (device == null) {
            return;
        }

        // 删除在线状态
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        String statusKey = DEVICE_STATUS_KEY_PREFIX + deviceId;
        String tenantKey = TENANT_ONLINE_KEY_PREFIX + device.getTenantId();

        redisTemplate.delete(onlineKey);
        redisTemplate.delete(statusKey);
        redisTemplate.opsForSet().remove(tenantKey, deviceId.toString());

        // 发送状态变化消息
        publishStatusChange(deviceId, "offline");

        // 更新数据库设备状态
        updateDeviceStatus(deviceId, "offline");

        log.info("设备离线成功: deviceId={}", deviceId);
    }

    /**
     * 查询设备是否在线
     *
     * @param deviceId 设备ID
     * @return 是否在线
     */
    public boolean isOnline(Long deviceId) {
        String key = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 查询设备状态
     *
     * @param deviceId 设备ID
     * @return 设备状态信息
     */
    public DeviceStatusVO getStatus(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在");
        }

        DeviceStatusVO status = new DeviceStatusVO();
        status.setDeviceId(deviceId);
        status.setDeviceCode(device.getDeviceCode());
        status.setOnline(isOnline(deviceId));

        // 从 Redis 获取详细状态
        String statusKey = DEVICE_STATUS_KEY_PREFIX + deviceId;
        Map<Object, Object> statusData = redisTemplate.opsForHash().entries(statusKey);
        if (!statusData.isEmpty()) {
            Object onlineTime = statusData.get("onlineTime");
            if (onlineTime != null) {
                status.setLastSeen(((Number) onlineTime).longValue());
            }
            status.setClientId((String) statusData.get("clientId"));
        }

        return status;
    }

    /**
     * 批量查询设备状态
     *
     * @param deviceIds 设备ID列表
     * @return 设备状态列表
     */
    public List<DeviceStatusVO> batchGetStatus(List<Long> deviceIds) {
        return deviceIds.stream()
                .map(this::getStatus)
                .collect(Collectors.toList());
    }

    /**
     * 查询租户在线设备
     *
     * @param tenantId 租户ID
     * @return 在线设备ID列表
     */
    public Set<Long> getOnlineDevices(String tenantId) {
        String key = TENANT_ONLINE_KEY_PREFIX + tenantId;
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toSet());
    }

    /**
     * 查询在线设备数量
     *
     * @param tenantId 租户ID
     * @return 在线设备数量
     */
    public long getOnlineCount(String tenantId) {
        String key = TENANT_ONLINE_KEY_PREFIX + tenantId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * 查询设备状态历史（最近N条）
     *
     * @param deviceId 设备ID
     * @param limit    数量限制
     * @return 状态历史列表
     */
    public List<StatusHistoryVO> getStatusHistory(Long deviceId, int limit) {
        // TODO: 从数据库或 InfluxDB 查询历史状态
        return Collections.emptyList();
    }

    /**
     * 刷新设备心跳（延长在线时间）
     *
     * @param deviceId 设备ID
     */
    public void refreshHeartbeat(Long deviceId) {
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        Boolean exists = redisTemplate.hasKey(onlineKey);

        if (Boolean.TRUE.equals(exists)) {
            // 延长过期时间
            redisTemplate.expire(onlineKey, Duration.ofSeconds(ONLINE_TIMEOUT_SECONDS));
            log.debug("刷新设备心跳: deviceId={}", deviceId);
        }
    }

    /**
     * 发送状态变化消息到 Kafka
     */
    private void publishStatusChange(Long deviceId, String status) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "status_change",
                    "deviceId", deviceId,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
            );

            // TODO: 使用 ObjectMapper 序列化
            kafkaTemplate.send("device-status-change", message.toString());
        } catch (Exception e) {
            log.error("发送状态变化消息失败: {}", e.getMessage());
        }
    }

    /**
     * 更新数据库中的设备状态
     */
    private void updateDeviceStatus(Long deviceId, String status) {
        // 使用异步更新，避免阻塞
        CompletableFuture.runAsync(() -> {
            try {
                Device device = new Device();
                device.setId(deviceId);
                device.setStatus(status);
                device.setLastActiveTime(LocalDateTime.now());
                deviceService.updateById(device);
            } catch (Exception e) {
                log.error("更新设备状态失败: deviceId={}, error={}", deviceId, e.getMessage());
            }
        });
    }

    /**
     * 设备状态 VO
     */
    @lombok.Data
    public static class DeviceStatusVO {
        private Long deviceId;
        private String deviceCode;
        private Boolean online;
        private Long lastSeen;
        private String clientId;
    }

    /**
     * 状态历史 VO
     */
    @lombok.Data
    public static class StatusHistoryVO {
        private Long deviceId;
        private String status;
        private LocalDateTime statusTime;
        private Integer duration; // 持续时长（秒）
    }
}
