package com.openiot.device.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.*;
import com.openiot.device.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备控制服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceControlService extends ServiceImpl<ServiceCallRecordMapper, ServiceCallRecord> {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final ThingModelService thingModelService;
    private final PropertySetRecordMapper propertySetRecordMapper;
    private final CommandSendRecordMapper commandSendRecordMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 调用设备服务
     *
     * @param deviceId            设备ID
     * @param serviceIdentifier 服务标识符
     * @param inputParams          输入参数
     * @return 调用记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceCallRecord callService(Long deviceId, String serviceIdentifier, Map<String, Object> inputParams) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在");
        }

        // 2. 查询服务定义（从 JsonNode 中查找）
        JsonNode servicesNode = thingModelService.getServices(device.getProductId());
        JsonNode serviceDef = null;
        if (servicesNode != null && servicesNode.isArray()) {
            for (JsonNode node : servicesNode) {
                if (node.has("identifier") && node.get("identifier").asText().equals(serviceIdentifier)) {
                    serviceDef = node;
                    break;
                }
            }
        }

        if (serviceDef == null) {
            throw BusinessException.badRequest("服务不存在: " + serviceIdentifier);
        }

        // 获取服务名称和调用类型
        String serviceName = serviceDef.has("name") ? serviceDef.get("name").asText() : serviceIdentifier;
        String callType = serviceDef.has("callType") ? serviceDef.get("callType").asText() : "async";

        // 3. 创建调用记录
        ServiceCallRecord record = new ServiceCallRecord();
        record.setTenantId(device.getTenantId());
        record.setDeviceId(deviceId);
        record.setProductId(device.getProductId());
        record.setServiceIdentifier(serviceIdentifier);
        record.setServiceName(serviceName);
        record.setInputParams(objectMapper.valueToTree(inputParams));
        record.setCallType(callType);
        record.setStatus("pending");
        record.setCallTime(LocalDateTime.now());

        this.save(record);

        // 4. 发送控制命令到 Kafka（由 MQTT Broker 或其他服务处理）
        sendControlCommand(device, record, inputParams);

        log.info("服务调用已下发: device={}, service={}", device.getDeviceCode(), serviceIdentifier);

        return record;
    }

    /**
     * 设置设备属性
     *
     * @param deviceId            设备ID
     * @param propertyIdentifier 属性标识符
     * @param value                新值
     * @return 设置记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PropertySetRecord setProperty(Long deviceId, String propertyIdentifier, Object value) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在");
        }

        // 2. 查询属性定义（从 JsonNode 中查找，校验可写权限）
        JsonNode propertiesNode = thingModelService.getProperties(device.getProductId());
        JsonNode propertyDef = null;
        if (propertiesNode != null && propertiesNode.isArray()) {
            for (JsonNode node : propertiesNode) {
                if (node.has("identifier") && node.get("identifier").asText().equals(propertyIdentifier)) {
                    propertyDef = node;
                    break;
                }
            }
        }

        if (propertyDef == null) {
            throw BusinessException.badRequest("属性不存在: " + propertyIdentifier);
        }

        // 获取属性名称和读写权限
        String propertyName = propertyDef.has("name") ? propertyDef.get("name").asText() : propertyIdentifier;
        String readWriteFlag = propertyDef.has("accessMode") ? propertyDef.get("accessMode").asText() : "rw";

        // 检查是否可写
        if ("r".equals(readWriteFlag)) {
            throw BusinessException.badRequest("属性只读，无法设置: " + propertyIdentifier);
        }

        // 3. 创建设置记录
        PropertySetRecord record = new PropertySetRecord();
        record.setTenantId(device.getTenantId());
        record.setDeviceId(deviceId);
        record.setProductId(device.getProductId());
        record.setPropertyIdentifier(propertyIdentifier);
        record.setPropertyName(propertyName);
        record.setNewValue(objectMapper.valueToTree(value));
        record.setSetType("user"); // 用户设置
        record.setStatus("pending");
        record.setCreateTime(LocalDateTime.now());

        propertySetRecordMapper.insert(record);

        // 4. 发送设置命令
        sendPropertySetCommand(device, record, value);

        log.info("属性设置已下发: device={}, property={}", device.getDeviceCode(), propertyIdentifier);

        return record;
    }

    /**
     * 发送命令
     *
     * @param deviceId            设备ID
     * @param commandIdentifier 命令标识符
     * @param commandParams       命令参数
     * @return 下发记录
     */
    @Transactional(rollbackFor = Exception.class)
    public CommandSendRecord sendCommand(Long deviceId, String commandIdentifier, Map<String, Object> commandParams) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在");
        }

        // 2. 创建下发记录
        CommandSendRecord record = new CommandSendRecord();
        record.setTenantId(device.getTenantId());
        record.setDeviceId(deviceId);
        record.setProductId(device.getProductId());
        record.setCommandIdentifier(commandIdentifier);
        record.setCommandName(commandIdentifier); // 可以从产品定义中查询
        record.setCommandParams(objectMapper.valueToTree(commandParams));
        record.setStatus("pending");
        record.setSendTime(LocalDateTime.now());

        commandSendRecordMapper.insert(record);

        // 3. 发送命令
        sendCommandToDevice(device, record, commandParams);

        log.info("命令已下发: device={}, command={}", device.getDeviceCode(), commandIdentifier);

        return record;
    }

    /**
     * 发送控制命令到 Kafka
     */
    private void sendControlCommand(Device device, ServiceCallRecord record, Map<String, Object> inputParams) {
        try {
            // 构造控制消息
            Map<String, Object> message = Map.of(
                    "type", "service_call",
                    "deviceId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "recordId", record.getId(),
                    "serviceIdentifier", record.getServiceIdentifier(),
                    "inputParams", inputParams,
                    "timestamp", LocalDateTime.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("device-control", jsonMessage);

            // 更新状态为已发送
            record.setStatus("sent");
            this.updateById(record);

        } catch (Exception e) {
            log.error("发送服务调用命令失败: {}", e.getMessage(), e);

            // 更新状态为失败
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            this.updateById(record);
        }
    }

    /**
     * 发送属性设置命令
     */
    private void sendPropertySetCommand(Device device, PropertySetRecord record, Object value) {
        try {
            // 构造设置消息
            Map<String, Object> message = Map.of(
                    "type", "property_set",
                    "deviceId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "recordId", record.getId(),
                    "propertyIdentifier", record.getPropertyIdentifier(),
                    "value", value,
                    "timestamp", LocalDateTime.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("device-control", jsonMessage);

        } catch (Exception e) {
            log.error("发送属性设置命令失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送命令到设备
     */
    private void sendCommandToDevice(Device device, CommandSendRecord record, Map<String, Object> commandParams) {
        try {
            // 构造命令消息
            Map<String, Object> message = Map.of(
                    "type", "command_send",
                    "deviceId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "recordId", record.getId(),
                    "commandIdentifier", record.getCommandIdentifier(),
                    "commandParams", commandParams,
                    "timestamp", LocalDateTime.now().toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("device-control", jsonMessage);

        } catch (Exception e) {
            log.error("发送命令失败: {}", e.getMessage(), e);
        }
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
}
