package com.openiot.connect.auth;

import com.openiot.common.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAuthService {

    private final RedisUtil redisUtil;

    private static final String DEVICE_TOKEN_KEY_PREFIX = "device:token:";

    /**
     * 验证设备 Token
     */
    public boolean authenticate(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String key = DEVICE_TOKEN_KEY_PREFIX + token;
        Object deviceInfo = redisUtil.get(key);

        if (deviceInfo != null) {
            log.debug("设备Token验证成功: {}", token);
            return true;
        }

        // TODO: 如果 Redis 中没有，从数据库查询并缓存
        log.warn("设备Token验证失败: {}", token);
        return false;
    }

    /**
     * 获取设备信息
     */
    public DeviceInfo getDeviceInfo(String token) {
        String key = DEVICE_TOKEN_KEY_PREFIX + token;
        Object deviceInfo = redisUtil.hGet(key, "info");

        if (deviceInfo == null) {
            return null;
        }

        // TODO: 解析设备信息
        DeviceInfo info = new DeviceInfo();
        info.setTenantId((String) redisUtil.hGet(key, "tenantId"));
        info.setDeviceId((String) redisUtil.hGet(key, "deviceId"));
        return info;
    }

    /**
     * 设备信息
     */
    public static class DeviceInfo {
        private String tenantId;
        private String deviceId;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }
}
