package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.DeviceData;
import com.openiot.device.entity.ProductProperty;
import com.openiot.device.mapper.DeviceDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备数据服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService extends ServiceImpl<DeviceDataMapper, DeviceData> {

    private final DeviceService deviceService;
    private final ThingModelService thingModelService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 接收并保存设备上报数据
     *
     * @param deviceId 设备ID
     * @param data     上报数据（属性标识符 -> 值）
     * @param dataTime 数据时间戳（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportData(Long deviceId, Map<String, Object> data, LocalDateTime dataTime) {
        // 1. 查询设备信息
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        // 2. 校验租户权限
        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }

        // 3. 根据物模型校验数据
        validateDataWithThingModel(device.getProductId(), data);

        // 4. 构造设备数据实体
        DeviceData deviceData = new DeviceData();
        deviceData.setTenantId(device.getTenantId());
        deviceData.setProductId(device.getProductId());
        deviceData.setDeviceId(deviceId);

        // 转换为 JsonNode
        ObjectNode dataNode = objectMapper.valueToTree(data);
        deviceData.setData(dataNode);

        // 设置数据时间
        if (dataTime == null) {
            dataTime = LocalDateTime.now();
        }
        deviceData.setDataTime(dataTime);

        // 5. 保存到数据库
        this.save(deviceData);

        log.info("设备数据上报成功: device={}, dataCount={}", device.getDeviceCode(), data.size());

        // 6. 发送到 Kafka（异步处理）
        sendToKafka(device, data, dataTime);
    }

    /**
     * 查询设备在指定时间范围内的数据
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 数据列表
     */
    public List<DeviceData> queryByTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        return baseMapper.selectByTimeRange(deviceId, startTime, endTime);
    }

    /**
     * 查询设备最新的 N 条数据
     *
     * @param deviceId 设备ID
     * @param limit    条数
     * @return 数据列表
     */
    public List<DeviceData> queryLatest(Long deviceId, int limit) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        return baseMapper.selectLatest(deviceId, limit);
    }

    /**
     * 查询设备最新一条数据
     *
     * @param deviceId 设备ID
     * @return 最新数据
     */
    public DeviceData getLatest(Long deviceId) {
        List<DeviceData> latestList = queryLatest(deviceId, 1);
        return latestList.isEmpty() ? null : latestList.get(0);
    }

    /**
     * 查询产品下所有设备的最新数据
     *
     * @param productId 产品ID
     * @param limit     条数
     * @return 数据列表
     */
    public List<DeviceData> queryLatestByProduct(Long productId, int limit) {
        return baseMapper.selectLatestByProduct(productId, limit);
    }

    /**
     * 根据物模型校验数据
     *
     * @param productId 产品ID
     * @param data      上报数据
     */
    private void validateDataWithThingModel(Long productId, Map<String, Object> data) {
        // 使用 ThingModelService 的物模型校验功能
        // 获取产品的物模型定义（JsonNode 格式）
        JsonNode thingModel = thingModelService.getThingModel(productId);
        if (thingModel == null || thingModel.isNull()) {
            return; // 没有物模型定义，跳过校验
        }

        // 校验每个属性
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String propertyIdentifier = entry.getKey();
            Object value = entry.getValue();

            // 校验属性是否            thingModelService.validateProperty(productId, propertyIdentifier, value);
        }
    }

    /**
     * 校验数据类型
     */
    private void validateDataType(ProductProperty property, Object value) {
        String dataType = property.getDataType();

        try {
            switch (dataType) {
                case "int":
                    if (!(value instanceof Integer || value instanceof Long)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 int，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "float":
                    if (!(value instanceof Number)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 float，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "bool":
                    if (!(value instanceof Boolean)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 bool，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                case "string":
                    if (!(value instanceof String)) {
                        throw BusinessException.badRequest(
                                String.format("属性 %s 类型错误，期望 string，实际 %s",
                                        property.getPropertyIdentifier(), value.getClass().getSimpleName()));
                    }
                    break;
                // JSON, enum, text, date 等类型可以暂时跳过严格校验
                default:
                    log.debug("属性 {} 类型 {} 跳过校验", property.getPropertyIdentifier(), dataType);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("数据类型校验异常: {}", e.getMessage());
        }
    }

    /**
     * 校验数据取值范围
     */
    private void validateDataRange(ProductProperty property, Object value) {
        JsonNode spec = property.getSpec();
        if (spec == null || !(value instanceof Number)) {
            return;
        }

        double numValue = ((Number) value).doubleValue();

        // 校验 min/max
        if (spec.has("min") && numValue < spec.get("min").asDouble()) {
            log.warn("属性 {} 值 {} 小于最小值 {}",
                    property.getPropertyIdentifier(), numValue, spec.get("min").asDouble());
        }

        if (spec.has("max") && numValue > spec.get("max").asDouble()) {
            log.warn("属性 {} 值 {} 大于最大值 {}",
                    property.getPropertyIdentifier(), numValue, spec.get("max").asDouble());
        }
    }

    /**
     * 验证设备访问权限
     */
    private void validateDeviceAccess(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }
    }

    /**
     * 发送数据到 Kafka（异步处理）
     */
    private void sendToKafka(Device device, Map<String, Object> data, LocalDateTime dataTime) {
        try {
            // 构造 Kafka 消息
            Map<String, Object> message = Map.of(
                    "deviceId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "productId", device.getProductId(),
                    "data", data,
                    "dataTime", dataTime.toString()
            );

            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送到 Kafka topic
            kafkaTemplate.send("device-data", jsonMessage);

            log.debug("设备数据已发送到 Kafka: device={}", device.getDeviceCode());

        } catch (Exception e) {
            log.error("发送数据到 Kafka 失败: {}", e.getMessage());
            // Kafka 发送失败不影响主流程
        }
    }

    /**
     * 分页查询设备历史数据
     *
     * @param deviceId  设备ID
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页数据
     */
    public Page<DeviceData> queryDeviceData(Long deviceId, int pageNum, int pageSize,
                                             LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        Page<DeviceData> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPage(page, new LambdaQueryWrapper<DeviceData>()
                .eq(DeviceData::getDeviceId, deviceId)
                .ge(startTime != null, DeviceData::getDataTime, startTime)
                .le(endTime != null, DeviceData::getDataTime, endTime)
                .orderByDesc(DeviceData::getDataTime));
    }

    /**
     * 查询设备属性数据（按属性）
     *
     * @param deviceId           设备ID
     * @param propertyIdentifier 属性标识符
     * @param startTime          开始时间
     * @param endTime            结束时间
     * @return 属性数据列表
     */
    public List<PropertyDataVO> queryPropertyData(Long deviceId, String propertyIdentifier,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        List<DeviceData> dataList = queryByTimeRange(deviceId, startTime, endTime);

        // 提取指定属性的数据
        List<PropertyDataVO> result = new java.util.ArrayList<>();
        for (DeviceData data : dataList) {
            if (data.getData() != null && data.getData().has(propertyIdentifier)) {
                JsonNode valueNode = data.getData().get(propertyIdentifier);
                PropertyDataVO vo = new PropertyDataVO();
                vo.setDeviceId(deviceId);
                vo.setPropertyIdentifier(propertyIdentifier);
                vo.setDataTime(data.getDataTime());

                if (valueNode.isNumber()) {
                    vo.setValue(valueNode.asDouble());
                    vo.setDataType("number");
                } else if (valueNode.isTextual()) {
                    vo.setValue(valueNode.asText());
                    vo.setDataType("string");
                } else if (valueNode.isBoolean()) {
                    vo.setValue(valueNode.asBoolean());
                    vo.setDataType("boolean");
                } else {
                    vo.setValue(valueNode.toString());
                    vo.setDataType("object");
                }

                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 聚合查询设备数据统计
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据
     */
    public DataStatisticsVO getStatistics(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备存在性和权限
        validateDeviceAccess(deviceId);

        List<DeviceData> dataList = queryByTimeRange(deviceId, startTime, endTime);

        DataStatisticsVO statistics = new DataStatisticsVO();
        statistics.setDeviceId(deviceId);
        statistics.setStartTime(startTime);
        statistics.setEndTime(endTime);
        statistics.setTotalCount(dataList.size());

        if (!dataList.isEmpty()) {
            statistics.setFirstDataTime(dataList.get(dataList.size() - 1).getDataTime());
            statistics.setLastDataTime(dataList.get(0).getDataTime());
        }

        return statistics;
    }

    /**
     * 属性数据 VO
     */
    @lombok.Data
    public static class PropertyDataVO {
        private Long deviceId;
        private String propertyIdentifier;
        private Object value;
        private String dataType;
        private LocalDateTime dataTime;
    }

    /**
     * 数据统计 VO
     */
    @lombok.Data
    public static class DataStatisticsVO {
        private Long deviceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer totalCount;
        private LocalDateTime firstDataTime;
        private LocalDateTime lastDataTime;
    }
}
