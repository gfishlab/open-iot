package com.openiot.data.service;

import com.openiot.common.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

        Map<String, Object> status = new HashMap<>();
        status.put("online", online);
        status.put("lastSeen", System.currentTimeMillis());

        redisUtil.hSet(key, "online", String.valueOf(online));
        redisUtil.hSet(key, "lastSeen", String.valueOf(System.currentTimeMillis()));
        redisUtil.expire(key, STATUS_TTL);

        log.debug("更新设备状态: deviceId={}, online={}", deviceId, online);
    }

    /**
     * 获取设备在线状态
     */
    public Map<String, Object> getDeviceStatus(String tenantId, String deviceId) {
        String key = DEVICE_STATUS_KEY_PREFIX + tenantId + ":" + deviceId;

        Map<String, Object> status = new HashMap<>();
        status.put("online", Boolean.parseBoolean(String.valueOf(redisUtil.hGet(key, "online"))));
        status.put("lastSeen", Long.parseLong(String.valueOf(redisUtil.hGet(key, "lastSeen")));

        return status;
    }
}
