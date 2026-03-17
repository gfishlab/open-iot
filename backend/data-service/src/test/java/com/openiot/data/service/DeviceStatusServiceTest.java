package com.openiot.data.service;

import com.openiot.common.redis.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 设备状态服务单元测试（data-service 版本）
 * 覆盖设备上线/离线状态更新、状态查询等场景
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("设备状态服务测试（data-service）")
class DeviceStatusServiceTest {

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private DeviceStatusService deviceStatusService;

    // ========== 公共测试数据 ==========
    private static final String TENANT_ID = "1";
    private static final String DEVICE_ID = "device001";
    private static final String STATUS_KEY = "device:status:" + TENANT_ID + ":" + DEVICE_ID;

    // ==========================================================
    // T032-1: 记录设备上线状态
    // ==========================================================

    @Nested
    @DisplayName("设备上线状态测试")
    class DeviceOnlineTests {

        @Test
        @DisplayName("记录设备上线状态 - 成功写入 Redis")
        void updateOnlineStatus_Online_ShouldSetRedisFields() {
            // When: 更新设备为上线状态
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, true);

            // Then: 应写入 online=true 和 lastSeen 时间戳
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("online"), eq("true"));
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("lastSeen"), anyString());
            // 应设置过期时间（5分钟 = 300秒）
            verify(redisUtil).expire(eq(STATUS_KEY), eq(300L));
        }

        @Test
        @DisplayName("记录设备上线状态 - 验证 Redis Key 格式")
        void updateOnlineStatus_Online_ShouldUseCorrectKeyFormat() {
            // Given: 不同租户和设备
            String tenantId = "99";
            String deviceId = "sensor-abc";
            String expectedKey = "device:status:99:sensor-abc";

            // When
            deviceStatusService.updateOnlineStatus(tenantId, deviceId, true);

            // Then: Key 格式应为 device:status:{tenantId}:{deviceId}
            verify(redisUtil).hSet(eq(expectedKey), eq("online"), eq("true"));
            verify(redisUtil).hSet(eq(expectedKey), eq("lastSeen"), anyString());
            verify(redisUtil).expire(eq(expectedKey), eq(300L));
        }
    }

    // ==========================================================
    // T032-2: 记录设备离线状态
    // ==========================================================

    @Nested
    @DisplayName("设备离线状态测试")
    class DeviceOfflineTests {

        @Test
        @DisplayName("记录设备离线状态 - 成功写入 Redis")
        void updateOnlineStatus_Offline_ShouldSetRedisFields() {
            // When: 更新设备为离线状态
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, false);

            // Then: 应写入 online=false 和 lastSeen 时间戳
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("online"), eq("false"));
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("lastSeen"), anyString());
            verify(redisUtil).expire(eq(STATUS_KEY), eq(300L));
        }

        @Test
        @DisplayName("设备离线后再上线 - 状态正确更新")
        void updateOnlineStatus_OfflineThenOnline_ShouldUpdateCorrectly() {
            // When: 先离线，再上线
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, false);
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, true);

            // Then: 应分别调用 hSet 设置 online 为 false 和 true
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("online"), eq("false"));
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("online"), eq("true"));
            // expire 应被调用两次
            verify(redisUtil, times(2)).expire(eq(STATUS_KEY), eq(300L));
        }
    }

    // ==========================================================
    // T032-3: 查询设备在线统计
    // ==========================================================

    @Nested
    @DisplayName("设备状态查询测试")
    class DeviceStatusQueryTests {

        @Test
        @DisplayName("查询设备状态 - 在线设备")
        void getDeviceStatus_OnlineDevice_ShouldReturnOnline() {
            // Given: Redis 中设备状态为在线
            when(redisUtil.hGet(STATUS_KEY, "online")).thenReturn("true");
            when(redisUtil.hGet(STATUS_KEY, "lastSeen")).thenReturn("1710000000000");

            // When
            DeviceStatusService.DeviceStatusVO status =
                    deviceStatusService.getDeviceStatus(TENANT_ID, DEVICE_ID);

            // Then
            assertThat(status).isNotNull();
            assertThat(status.getOnline()).isTrue();
            assertThat(status.getLastSeen()).isEqualTo(1710000000000L);
        }

        @Test
        @DisplayName("查询设备状态 - 离线设备")
        void getDeviceStatus_OfflineDevice_ShouldReturnOffline() {
            // Given: Redis 中设备状态为离线
            when(redisUtil.hGet(STATUS_KEY, "online")).thenReturn("false");
            when(redisUtil.hGet(STATUS_KEY, "lastSeen")).thenReturn("1710000000000");

            // When
            DeviceStatusService.DeviceStatusVO status =
                    deviceStatusService.getDeviceStatus(TENANT_ID, DEVICE_ID);

            // Then
            assertThat(status).isNotNull();
            assertThat(status.getOnline()).isFalse();
            assertThat(status.getLastSeen()).isEqualTo(1710000000000L);
        }

        @Test
        @DisplayName("查询设备状态 - 设备不存在（Redis 无数据）")
        void getDeviceStatus_DeviceNotInRedis_ShouldReturnDefaultStatus() {
            // Given: Redis 中无该设备数据
            when(redisUtil.hGet(STATUS_KEY, "online")).thenReturn(null);
            when(redisUtil.hGet(STATUS_KEY, "lastSeen")).thenReturn(null);

            // When
            DeviceStatusService.DeviceStatusVO status =
                    deviceStatusService.getDeviceStatus(TENANT_ID, DEVICE_ID);

            // Then: 默认离线，lastSeen 为 null
            assertThat(status).isNotNull();
            assertThat(status.getOnline()).isFalse();
            assertThat(status.getLastSeen()).isNull();
        }

        @Test
        @DisplayName("查询设备状态 - lastSeen 为 null 字符串时返回 null")
        void getDeviceStatus_LastSeenIsNullString_ShouldReturnNull() {
            // Given: Redis 中 lastSeen 字段值为 "null" 字符串
            when(redisUtil.hGet(STATUS_KEY, "online")).thenReturn("true");
            when(redisUtil.hGet(STATUS_KEY, "lastSeen")).thenReturn("null");

            // When
            DeviceStatusService.DeviceStatusVO status =
                    deviceStatusService.getDeviceStatus(TENANT_ID, DEVICE_ID);

            // Then: lastSeen 应为 null（而不是解析 "null" 导致异常）
            assertThat(status).isNotNull();
            assertThat(status.getOnline()).isTrue();
            assertThat(status.getLastSeen()).isNull();
        }
    }

    // ==========================================================
    // T032-4: 设备状态变更记录
    // ==========================================================

    @Nested
    @DisplayName("设备状态变更记录测试")
    class DeviceStatusChangeTests {

        @Test
        @DisplayName("状态变更 - 上线时 lastSeen 更新为当前时间戳")
        void updateOnlineStatus_ShouldUpdateLastSeen() {
            // Given: 记录调用前的时间
            long beforeTime = System.currentTimeMillis();

            // When: 更新设备上线状态
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, true);

            // Then: lastSeen 应为当前时间戳
            verify(redisUtil).hSet(eq(STATUS_KEY), eq("lastSeen"), argThat(value -> {
                long timestamp = Long.parseLong((String) value);
                // lastSeen 应在调用前后的合理范围内
                return timestamp >= beforeTime && timestamp <= System.currentTimeMillis();
            }));
        }

        @Test
        @DisplayName("状态变更 - 每次更新都刷新 TTL")
        void updateOnlineStatus_ShouldRefreshTTL() {
            // When: 连续三次更新状态
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, true);
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, true);
            deviceStatusService.updateOnlineStatus(TENANT_ID, DEVICE_ID, false);

            // Then: expire 应被调用三次，每次都刷新 TTL
            verify(redisUtil, times(3)).expire(eq(STATUS_KEY), eq(300L));
        }

        @Test
        @DisplayName("状态变更 - 多设备并行更新互不影响")
        void updateOnlineStatus_MultipleDevices_ShouldBeIndependent() {
            // Given: 两台不同的设备
            String device1Key = "device:status:" + TENANT_ID + ":device001";
            String device2Key = "device:status:" + TENANT_ID + ":device002";

            // When: 分别更新两台设备的状态
            deviceStatusService.updateOnlineStatus(TENANT_ID, "device001", true);
            deviceStatusService.updateOnlineStatus(TENANT_ID, "device002", false);

            // Then: 两台设备的 Redis Key 应不同，互不干扰
            verify(redisUtil).hSet(eq(device1Key), eq("online"), eq("true"));
            verify(redisUtil).hSet(eq(device2Key), eq("online"), eq("false"));
        }

        @Test
        @DisplayName("状态变更 - 不同租户同设备 ID 互不影响")
        void updateOnlineStatus_DifferentTenants_ShouldBeIndependent() {
            // Given: 两个不同租户的同名设备
            String tenant1Key = "device:status:1:device001";
            String tenant2Key = "device:status:2:device001";

            // When: 分别更新不同租户的设备状态
            deviceStatusService.updateOnlineStatus("1", "device001", true);
            deviceStatusService.updateOnlineStatus("2", "device001", false);

            // Then: 不同租户的 Key 应不同
            verify(redisUtil).hSet(eq(tenant1Key), eq("online"), eq("true"));
            verify(redisUtil).hSet(eq(tenant2Key), eq("online"), eq("false"));
        }
    }
}
