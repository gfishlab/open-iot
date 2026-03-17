package com.openiot.device.service;

import com.openiot.device.entity.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 设备状态服务单元测试
 *
 * @author OpenIoT Team
 */
@ExtendWith(SpringExtension.class)
@DisplayName("设备状态服务测试")
class DeviceStatusServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private DeviceStatusService deviceStatusService;

    private Device testDevice;

    @BeforeEach
    void setUp() {
        // 初始化测试设备
        testDevice = new Device();
        testDevice.setId(1L);
        testDevice.setTenantId(1L);
        testDevice.setDeviceCode("device001");
        testDevice.setDeviceName("测试设备");
        testDevice.setStatus("offline");

        // Mock Redis 操作
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("设备上线 - 成功")
    void setOnline_Success() {
        // Given
        when(deviceService.getByCode("device001")).thenReturn(testDevice);
        when(setOperations.add(anyString(), any())).thenReturn(1L);

        // When
        deviceStatusService.setOnline("device001", "client001");

        // Then
        verify(valueOperations, times(1)).set(anyString(), eq("online"), anyLong(), any());
        verify(kafkaTemplate, times(1)).send(eq("device-status-change"), anyString());
    }

    @Test
    @DisplayName("设备上线 - 设备不存在")
    void setOnline_DeviceNotFound() {
        // Given
        when(deviceService.getByCode("notexist")).thenReturn(null);

        // When
        deviceStatusService.setOnline("notexist", "client001");

        // Then
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("设备离线 - 成功")
    void setOffline_Success() {
        // Given
        when(deviceService.getByCode("device001")).thenReturn(testDevice);
        when(deviceService.getById(1L)).thenReturn(testDevice);

        // When
        deviceStatusService.setOffline("device001");

        // Then
        verify(redisTemplate, times(1)).delete(contains("device:online:1"));
        verify(kafkaTemplate, times(1)).send(eq("device-status-change"), anyString());
    }

    @Test
    @DisplayName("检查设备在线状态 - 在线")
    void isOnline_Online() {
        // Given
        when(redisTemplate.hasKey("device:online:1")).thenReturn(true);

        // When
        boolean online = deviceStatusService.isOnline(1L);

        // Then
        assertThat(online).isTrue();
    }

    @Test
    @DisplayName("检查设备在线状态 - 离线")
    void isOnline_Offline() {
        // Given
        when(redisTemplate.hasKey("device:online:1")).thenReturn(false);

        // When
        boolean online = deviceStatusService.isOnline(1L);

        // Then
        assertThat(online).isFalse();
    }

    @Test
    @DisplayName("查询设备状态 - 成功")
    void getStatus_Success() {
        // Given
        when(deviceService.getById(1L)).thenReturn(testDevice);
        when(redisTemplate.hasKey("device:online:1")).thenReturn(true);

        // When
        DeviceStatusService.DeviceStatusVO status = deviceStatusService.getStatus(1L);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getDeviceId()).isEqualTo(1L);
        assertThat(status.getDeviceCode()).isEqualTo("device001");
        assertThat(status.getOnline()).isTrue();
    }

    @Test
    @DisplayName("批量查询设备状态 - 成功")
    void batchGetStatus_Success() {
        // Given
        when(deviceService.getById(1L)).thenReturn(testDevice);
        when(deviceService.getById(2L)).thenReturn(testDevice);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        List<DeviceStatusService.DeviceStatusVO> statuses = deviceStatusService.batchGetStatus(List.of(1L, 2L));

        // Then
        assertThat(statuses).hasSize(2);
    }

    @Test
    @DisplayName("查询在线设备数量 - 成功")
    void getOnlineCount_Success() {
        // Given
        when(setOperations.size("tenant:online:1")).thenReturn(5L);

        // When
        long count = deviceStatusService.getOnlineCount("1");

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("刷新设备心跳 - 成功")
    void refreshHeartbeat_Success() {
        // Given
        when(redisTemplate.hasKey("device:online:1")).thenReturn(true);

        // When
        deviceStatusService.refreshHeartbeat(1L);

        // Then
        verify(redisTemplate, times(1)).expire(anyString(), any());
    }
}
