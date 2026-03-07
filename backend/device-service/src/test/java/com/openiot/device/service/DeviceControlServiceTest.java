package com.openiot.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.*;
import com.openiot.device.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 设备控制服务单元测试
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("设备控制服务测试")
class DeviceControlServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private ProductService productService;

    @Mock
    private ThingModelService thingModelService;

    @Mock
    private PropertySetRecordMapper propertySetRecordMapper;

    @Mock
    private CommandSendRecordMapper commandSendRecordMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private DeviceControlService deviceControlService;

    private Device testDevice;
    private ServiceCallRecord testServiceCallRecord;
    private PropertySetRecord testPropertySetRecord;
    private CommandSendRecord testCommandSendRecord;

    @BeforeEach
    void setUp() {
        // 初始化测试设备
        testDevice = new Device();
        testDevice.setId(1L);
        testDevice.setTenantId(1L);
        testDevice.setProductId(1L);
        testDevice.setDeviceCode("device001");
        testDevice.setDeviceName("测试设备");
        testDevice.setStatus("online");

        // 初始化服务调用记录
        testServiceCallRecord = new ServiceCallRecord();
        testServiceCallRecord.setId(1L);
        testServiceCallRecord.setTenantId(1L);
        testServiceCallRecord.setDeviceId(1L);
        testServiceCallRecord.setServiceIdentifier("restart");
        testServiceCallRecord.setServiceName("重启服务");
        testServiceCallRecord.setStatus("pending");
        testServiceCallRecord.setCallTime(LocalDateTime.now());

        // 初始化属性设置记录
        testPropertySetRecord = new PropertySetRecord();
        testPropertySetRecord.setId(1L);
        testPropertySetRecord.setTenantId(1L);
        testPropertySetRecord.setDeviceId(1L);
        testPropertySetRecord.setPropertyIdentifier("temperature");
        testPropertySetRecord.setPropertyName("温度");
        testPropertySetRecord.setStatus("pending");
        testPropertySetRecord.setCreateTime(LocalDateTime.now());

        // 初始化命令发送记录
        testCommandSendRecord = new CommandSendRecord();
        testCommandSendRecord.setId(1L);
        testCommandSendRecord.setTenantId(1L);
        testCommandSendRecord.setDeviceId(1L);
        testCommandSendRecord.setCommandIdentifier("reset");
        testCommandSendRecord.setCommandName("重置");
        testCommandSendRecord.setStatus("pending");
        testCommandSendRecord.setSendTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("调用设备服务 - 成功")
    void callService_Success() {
        // Given
        Long deviceId = 1L;
        String serviceIdentifier = "restart";
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("delay", 5000);

        ProductService serviceDef = new ProductService();
        serviceDef.setServiceIdentifier("restart");
        serviceDef.setServiceName("重启服务");
        serviceDef.setCallType("async");

        when(deviceService.getById(deviceId)).thenReturn(testDevice);
        when(thingModelService.getServices(any())).thenReturn(java.util.List.of(serviceDef));
        when(deviceControlService.save(any(ServiceCallRecord.class))).thenReturn(testServiceCallRecord);
        when(deviceControlService.updateById(any(ServiceCallRecord.class))).thenReturn(true);

        // When
        ServiceCallRecord result = deviceControlService.callService(deviceId, serviceIdentifier, inputParams);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceIdentifier()).isEqualTo("restart");
        assertThat(result.getStatus()).isEqualTo("pending");

        verify(deviceControlService, times(1)).save(any(ServiceCallRecord.class));
        verify(kafkaTemplate, times(1)).send(eq("device-control"), any(String.class));
    }

    @Test
    @DisplayName("调用设备服务 - 设备不存在")
    void callService_DeviceNotFound() {
        // Given
        when(deviceService.getById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> deviceControlService.callService(999L, "restart", new HashMap<>()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备不存在");

        verify(deviceControlService, never()).save(any(ServiceCallRecord.class));
    }

    @Test
    @DisplayName("调用设备服务 - 服务不存在")
    void callService_ServiceNotFound() {
        // Given
        when(deviceService.getById(1L)).thenReturn(testDevice);
        when(thingModelService.getServices(any())).thenReturn(java.util.List.of());

        // When & Then
        assertThatThrownBy(() -> deviceControlService.callService(1L, "notexist", new HashMap<>()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("服务不存在");

        verify(deviceControlService, never()).save(any(ServiceCallRecord.class));
    }

    @Test
    @DisplayName("设置设备属性 - 成功")
    void setProperty_Success() {
        // Given
        Long deviceId = 1L;
        String propertyIdentifier = "temperature";
        Object value = 25.5;

        ProductProperty property = new ProductProperty();
        property.setPropertyIdentifier("temperature");
        property.setPropertyName("温度");
        property.setReadWriteFlag("rw");

        when(deviceService.getById(deviceId)).thenReturn(testDevice);
        when(thingModelService.getProperties(any())).thenReturn(java.util.List.of(property));
        when(propertySetRecordMapper.insert(any(PropertySetRecord.class))).thenReturn(1);

        // When
        PropertySetRecord result = deviceControlService.setProperty(deviceId, propertyIdentifier, value);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPropertyIdentifier()).isEqualTo("temperature");
        assertThat(result.getStatus()).isEqualTo("pending");

        verify(propertySetRecordMapper, times(1)).insert(any(PropertySetRecord.class));
        verify(kafkaTemplate, times(1)).send(eq("device-control"), any(String.class));
    }

    @Test
    @DisplayName("设置设备属性 - 属性只读")
    void setProperty_ReadOnly() {
        // Given
        ProductProperty property = new ProductProperty();
        property.setPropertyIdentifier("temperature");
        property.setReadWriteFlag("r");  // 只读

        when(deviceService.getById(1L)).thenReturn(testDevice);
        when(thingModelService.getProperties(any())).thenReturn(java.util.List.of(property));

        // When & Then
        assertThatThrownBy(() -> deviceControlService.setProperty(1L, "temperature", 25.5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("属性只读");

        verify(propertySetRecordMapper, never()).insert(any(PropertySetRecord.class));
    }

    @Test
    @DisplayName("发送设备命令 - 成功")
    void sendCommand_Success() {
        // Given
        Long deviceId = 1L;
        String commandIdentifier = "reset";
        Map<String, Object> commandParams = new HashMap<>();
        commandParams.put("mode", "factory");

        when(deviceService.getById(deviceId)).thenReturn(testDevice);
        when(commandSendRecordMapper.insert(any(CommandSendRecord.class))).thenReturn(1);

        // When
        CommandSendRecord result = deviceControlService.sendCommand(deviceId, commandIdentifier, commandParams);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommandIdentifier()).isEqualTo("reset");
        assertThat(result.getStatus()).isEqualTo("pending");

        verify(commandSendRecordMapper, times(1)).insert(any(CommandSendRecord.class));
        verify(kafkaTemplate, times(1)).send(eq("device-control"), any(String.class));
    }

    @Test
    @DisplayName("发送设备命令 - 设备不存在")
    void sendCommand_DeviceNotFound() {
        // Given
        when(deviceService.getById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> deviceControlService.sendCommand(999L, "reset", new HashMap<>()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备不存在");

        verify(commandSendRecordMapper, never()).insert(any(CommandSendRecord.class));
    }
}
