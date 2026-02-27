package com.openiot.data.service;

import com.openiot.common.redis.util.RedisUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备状态服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusService {

    private final RedisUtil redisUtil;

    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";
    private static final long STATUS_TTL = 300; // 5分钟

    /**
     * 更新设备在线状态
     */
    public void updateOnlineStatus(String tenantId, String deviceId, boolean online) {
        String key = DEVICE_STATUS_KEY_PREFIX + tenantId + ":" + deviceId;

        redisUtil.hSet(key, "online", String.valueOf(online));
        redisUtil.hSet(key, "lastSeen", String.valueOf(System.currentTimeMillis()));
        redisUtil.expire(key, STATUS_TTL);

        log.debug("更新设备状态: deviceId={}, online={}", deviceId, online);
    }

    /**
     * 获取设备在线状态
     */
    public DeviceStatusVO getDeviceStatus(String tenantId, String deviceId) {
        String key = DEVICE_STATUS_KEY_PREFIX + tenantId + ":" + deviceId;

        DeviceStatusVO status = new DeviceStatusVO();
        status.setOnline(Boolean.parseBoolean(String.valueOf(redisUtil.hGet(key, "online"))));

        String lastSeen = String.valueOf(redisUtil.hGet(key, "lastSeen"));
        status.setLastSeen(lastSeen != null && !"null".equals(lastSeen) ? Long.parseLong(lastSeen) : null);

        return status;
    }

    /**
     * 设备状态响应 VO
     */
    @Data
    public static class DeviceStatusVO {
        private Boolean online;
        private Long lastSeen;
    }
}
