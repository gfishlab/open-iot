package com.openiot.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openiot.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 物模型服务单元测试
 *
 * <p>测试 ThingModelService 的物模型校验逻辑，包括属性、事件、服务定义的验证。
 * 物模型校验主要是纯函数式验证，只依赖 ObjectMapper，不需要大量 mock。
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("物模型服务测试")
class ThingModelServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    /** 被测服务，手动构造注入以使用真实的 ObjectMapper */
    private ThingModelService thingModelService;

    @BeforeEach
    void setUp() {
        // 手动构造 ThingModelService，注入真实的 ObjectMapper 和 mock 的 ProductService
        thingModelService = new ThingModelService(objectMapper, productService);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建一个包含完整属性、事件、服务定义的有效物模型
     */
    private ObjectNode createValidThingModel() {
        ObjectNode thingModel = objectMapper.createObjectNode();

        // 属性定义
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode tempProperty = objectMapper.createObjectNode();
        tempProperty.put("identifier", "temperature");
        tempProperty.put("name", "温度");
        tempProperty.put("dataType", "double");
        tempProperty.put("min", -40.0);
        tempProperty.put("max", 100.0);
        properties.add(tempProperty);

        ObjectNode humidityProperty = objectMapper.createObjectNode();
        humidityProperty.put("identifier", "humidity");
        humidityProperty.put("name", "湿度");
        humidityProperty.put("dataType", "int");
        properties.add(humidityProperty);

        thingModel.set("properties", properties);

        // 事件定义
        ArrayNode events = objectMapper.createArrayNode();
        ObjectNode alarmEvent = objectMapper.createObjectNode();
        alarmEvent.put("identifier", "highTempAlarm");
        alarmEvent.put("name", "高温告警");
        alarmEvent.put("type", "alert");
        events.add(alarmEvent);
        thingModel.set("events", events);

        // 服务定义
        ArrayNode services = objectMapper.createArrayNode();
        ObjectNode rebootService = objectMapper.createObjectNode();
        rebootService.put("identifier", "reboot");
        rebootService.put("name", "重启设备");
        rebootService.put("callType", "async");
        services.add(rebootService);
        thingModel.set("services", services);

        return thingModel;
    }

    // ==================== 有效物模型校验 ====================

    @Test
    @DisplayName("有效物模型校验通过")
    void validateThingModel_ValidModel() {
        // Given: 构造完整有效的物模型
        ObjectNode thingModel = createValidThingModel();

        // When & Then: 校验通过不抛出异常
        assertThatCode(() -> thingModelService.validateThingModel(thingModel))
                .doesNotThrowAnyException();

        boolean result = thingModelService.validateThingModel(thingModel);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("空物模型校验通过")
    void validateThingModel_NullModel() {
        // Given: null 物模型
        // When & Then: 空物模型是允许的
        assertThat(thingModelService.validateThingModel(null)).isTrue();
    }

    @Test
    @DisplayName("空节点物模型校验通过")
    void validateThingModel_EmptyModel() {
        // Given: 空 JSON 对象
        ObjectNode emptyModel = objectMapper.createObjectNode();

        // When & Then: 没有定义也是允许的
        assertThat(thingModelService.validateThingModel(emptyModel)).isTrue();
    }

    // ==================== 属性定义校验 ====================

    @Test
    @DisplayName("属性定义校验 - 缺少标识符")
    void validateProperty_MissingIdentifier() {
        // Given: 属性缺少 identifier 字段
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("name", "温度");
        property.put("dataType", "double");
        // 缺少 identifier
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then: 应抛出异常
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标识符不能为空");
    }

    @Test
    @DisplayName("属性定义校验 - 缺少名称")
    void validateProperty_MissingName() {
        // Given: 属性缺少 name 字段
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "temperature");
        property.put("dataType", "double");
        // 缺少 name
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称不能为空");
    }

    @Test
    @DisplayName("属性定义校验 - 缺少数据类型")
    void validateProperty_MissingDataType() {
        // Given: 属性缺少 dataType 字段
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "temperature");
        property.put("name", "温度");
        // 缺少 dataType
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据类型不能为空");
    }

    @Test
    @DisplayName("属性定义校验 - 不支持的数据类型")
    void validateProperty_InvalidDataType() {
        // Given: 使用不支持的数据类型
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "temperature");
        property.put("name", "温度");
        property.put("dataType", "unsupported_type");
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的数据类型");
    }

    @Test
    @DisplayName("属性定义校验 - 标识符格式错误（数字开头）")
    void validateProperty_InvalidIdentifierFormat() {
        // Given: 标识符以数字开头
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "123temperature");
        property.put("name", "温度");
        property.put("dataType", "double");
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格式错误");
    }

    @Test
    @DisplayName("属性定义校验 - 标识符重复")
    void validateProperty_DuplicateIdentifier() {
        // Given: 两个属性使用相同的标识符
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();

        ObjectNode prop1 = objectMapper.createObjectNode();
        prop1.put("identifier", "temperature");
        prop1.put("name", "温度1");
        prop1.put("dataType", "double");
        properties.add(prop1);

        ObjectNode prop2 = objectMapper.createObjectNode();
        prop2.put("identifier", "temperature");
        prop2.put("name", "温度2");
        prop2.put("dataType", "int");
        properties.add(prop2);

        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("属性定义校验 - min/max 范围错误")
    void validateProperty_InvalidRange() {
        // Given: min 大于等于 max
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "temperature");
        property.put("name", "温度");
        property.put("dataType", "double");
        property.put("min", 100.0);
        property.put("max", 50.0); // min > max
        properties.add(property);
        thingModel.set("properties", properties);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最小值必须小于最大值");
    }

    // ==================== 事件定义校验 ====================

    @Test
    @DisplayName("事件定义校验 - 有效事件")
    void validateEvent_ValidEvent() {
        // Given: 包含有效事件定义的物模型
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode events = objectMapper.createArrayNode();

        ObjectNode event = objectMapper.createObjectNode();
        event.put("identifier", "highTempAlarm");
        event.put("name", "高温告警");
        event.put("type", "alert");
        events.add(event);

        thingModel.set("events", events);

        // When & Then
        assertThatCode(() -> thingModelService.validateThingModel(thingModel))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("事件定义校验 - 无效事件类型")
    void validateEvent_InvalidType() {
        // Given: 使用无效的事件类型
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode events = objectMapper.createArrayNode();

        ObjectNode event = objectMapper.createObjectNode();
        event.put("identifier", "customEvent");
        event.put("name", "自定义事件");
        event.put("type", "invalid_type"); // 只允许 info、alert、fault
        events.add(event);

        thingModel.set("events", events);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("类型必须是");
    }

    @Test
    @DisplayName("事件定义校验 - 缺少标识符")
    void validateEvent_MissingIdentifier() {
        // Given
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode events = objectMapper.createArrayNode();

        ObjectNode event = objectMapper.createObjectNode();
        event.put("name", "告警事件");
        // 缺少 identifier
        events.add(event);

        thingModel.set("events", events);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标识符不能为空");
    }

    // ==================== 服务定义校验 ====================

    @Test
    @DisplayName("服务定义校验 - 有效服务（含输入输出参数）")
    void validateService_ValidService() {
        // Given: 包含完整输入输出参数的服务定义
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode services = objectMapper.createArrayNode();

        ObjectNode service = objectMapper.createObjectNode();
        service.put("identifier", "setSpeed");
        service.put("name", "设置速度");
        service.put("callType", "sync");

        // 输入参数
        ArrayNode inputParams = objectMapper.createArrayNode();
        ObjectNode speedParam = objectMapper.createObjectNode();
        speedParam.put("identifier", "speed");
        speedParam.put("name", "速度值");
        speedParam.put("dataType", "int");
        inputParams.add(speedParam);
        service.set("inputParams", inputParams);

        // 输出参数
        ArrayNode outputParams = objectMapper.createArrayNode();
        ObjectNode resultParam = objectMapper.createObjectNode();
        resultParam.put("identifier", "result");
        resultParam.put("name", "执行结果");
        resultParam.put("dataType", "boolean");
        outputParams.add(resultParam);
        service.set("outputParams", outputParams);

        services.add(service);
        thingModel.set("services", services);

        // When & Then
        assertThatCode(() -> thingModelService.validateThingModel(thingModel))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("服务定义校验 - 无效调用方式")
    void validateService_InvalidCallType() {
        // Given: 使用无效的 callType
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode services = objectMapper.createArrayNode();

        ObjectNode service = objectMapper.createObjectNode();
        service.put("identifier", "reboot");
        service.put("name", "重启设备");
        service.put("callType", "unknown"); // 只允许 sync 或 async
        services.add(service);

        thingModel.set("services", services);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("调用方式必须是");
    }

    @Test
    @DisplayName("服务定义校验 - 输入参数标识符重复")
    void validateService_DuplicateInputParam() {
        // Given: 输入参数有重复标识符
        ObjectNode thingModel = objectMapper.createObjectNode();
        ArrayNode services = objectMapper.createArrayNode();

        ObjectNode service = objectMapper.createObjectNode();
        service.put("identifier", "setConfig");
        service.put("name", "设置配置");

        ArrayNode inputParams = objectMapper.createArrayNode();
        ObjectNode param1 = objectMapper.createObjectNode();
        param1.put("identifier", "value");
        param1.put("name", "值1");
        param1.put("dataType", "string");
        inputParams.add(param1);

        ObjectNode param2 = objectMapper.createObjectNode();
        param2.put("identifier", "value"); // 重复标识符
        param2.put("name", "值2");
        param2.put("dataType", "int");
        inputParams.add(param2);

        service.set("inputParams", inputParams);
        services.add(service);
        thingModel.set("services", services);

        // When & Then
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    // ==================== 无效物模型校验失败 ====================

    @Test
    @DisplayName("无效物模型校验失败 - 跨定义标识符重复")
    void validateThingModel_CrossDefinitionDuplicate() {
        // Given: 属性和事件使用相同标识符
        ObjectNode thingModel = objectMapper.createObjectNode();

        ArrayNode properties = objectMapper.createArrayNode();
        ObjectNode property = objectMapper.createObjectNode();
        property.put("identifier", "sameId");
        property.put("name", "属性");
        property.put("dataType", "int");
        properties.add(property);
        thingModel.set("properties", properties);

        ArrayNode events = objectMapper.createArrayNode();
        ObjectNode event = objectMapper.createObjectNode();
        event.put("identifier", "sameId"); // 与属性标识符相同
        event.put("name", "事件");
        events.add(event);
        thingModel.set("events", events);

        // When & Then: 跨定义域的标识符也不允许重复
        assertThatThrownBy(() -> thingModelService.validateThingModel(thingModel))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    // ==================== 创建空物模型 ====================

    @Test
    @DisplayName("创建空物模型结构")
    void createEmptyThingModel_Success() {
        // When: 创建空物模型
        ObjectNode result = thingModelService.createEmptyThingModel();

        // Then: 应包含空的 properties、events、services 数组
        assertThat(result).isNotNull();
        assertThat(result.has("properties")).isTrue();
        assertThat(result.has("events")).isTrue();
        assertThat(result.has("services")).isTrue();
        assertThat(result.get("properties").isArray()).isTrue();
        assertThat(result.get("events").isArray()).isTrue();
        assertThat(result.get("services").isArray()).isTrue();
        assertThat(result.get("properties").size()).isZero();
        assertThat(result.get("events").size()).isZero();
        assertThat(result.get("services").size()).isZero();
    }

    // ==================== 添加属性到物模型 ====================

    @Test
    @DisplayName("添加属性到已有物模型")
    void addProperty_ToExistingModel() {
        // Given: 已有物模型和新属性
        ObjectNode thingModel = createValidThingModel();
        int originalSize = thingModel.get("properties").size();

        ObjectNode newProperty = objectMapper.createObjectNode();
        newProperty.put("identifier", "voltage");
        newProperty.put("name", "电压");
        newProperty.put("dataType", "double");

        // When: 添加属性
        JsonNode result = thingModelService.addProperty(thingModel, newProperty);

        // Then: 属性数量增加 1
        assertThat(result).isNotNull();
        assertThat(result.get("properties").size()).isEqualTo(originalSize + 1);
    }

    @Test
    @DisplayName("添加属性到空物模型")
    void addProperty_ToNullModel() {
        // Given: null 物模型和新属性
        ObjectNode newProperty = objectMapper.createObjectNode();
        newProperty.put("identifier", "voltage");
        newProperty.put("name", "电压");
        newProperty.put("dataType", "double");

        // When: 添加属性到 null 物模型
        JsonNode result = thingModelService.addProperty(null, newProperty);

        // Then: 自动创建新物模型并包含该属性
        assertThat(result).isNotNull();
        assertThat(result.get("properties").size()).isEqualTo(1);
    }
}
