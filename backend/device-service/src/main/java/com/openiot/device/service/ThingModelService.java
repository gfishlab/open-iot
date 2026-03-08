package com.openiot.device.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 物模型服务
 *
 * 负责物模型的定义、验证和管理
 * 物模型包含三类定义：
 * - 属性（properties）：设备上报的状态数据
 * - 事件（events）：设备主动上报的事件
 * - 服务（services）：平台可调用设备的服务
 *
 * @author open-iot
 */
@Slf4j
@Service
public class ThingModelService {

    private final ObjectMapper objectMapper;
    private final ProductService productService;

    /**
     * 构造器注入，使用 @Lazy 打破循环依赖
     */
    public ThingModelService(ObjectMapper objectMapper, @Lazy ProductService productService) {
        this.objectMapper = objectMapper;
        this.productService = productService;
    }

    /**
     * 验证物模型定义
     *
     * @param thingModel 物模型 JSON
     * @return 验证通过返回 true
     */
    public boolean validateThingModel(JsonNode thingModel) {
        if (thingModel == null || thingModel.isNull()) {
            return true; // 空物模型是允许的
        }

        // 收集所有标识符，用于检测重复
        Set<String> identifiers = new HashSet<>();

        // 验证属性定义
        JsonNode properties = thingModel.get("properties");
        if (properties != null && properties.isArray()) {
            for (JsonNode property : properties) {
                validatePropertyDefinition(property, identifiers);
            }
        }

        // 验证事件定义
        JsonNode events = thingModel.get("events");
        if (events != null && events.isArray()) {
            for (JsonNode event : events) {
                validateEventDefinition(event, identifiers);
            }
        }

        // 验证服务定义
        JsonNode services = thingModel.get("services");
        if (services != null && services.isArray()) {
            for (JsonNode service : services) {
                validateServiceDefinition(service, identifiers);
            }
        }

        return true;
    }

    /**
     * 验证属性定义
     */
    private void validatePropertyDefinition(JsonNode property, Set<String> identifiers) {
        // 必填字段检查
        String identifier = getRequiredString(property, "identifier", "属性标识符不能为空");
        String name = getRequiredString(property, "name", "属性名称不能为空");
        String dataType = getRequiredString(property, "dataType", "属性数据类型不能为空");

        // 检查标识符格式（只允许字母、数字、下划线，不能以数字开头）
        validateIdentifierFormat(identifier, "属性");

        // 检查标识符唯一性
        checkIdentifierUnique(identifiers, identifier, "属性");

        // 验证数据类型
        validateDataType(dataType);

        // 验证数值范围（如果提供了 min/max）
        if (property.has("min") && property.has("max")) {
            try {
                double min = property.get("min").asDouble();
                double max = property.get("max").asDouble();
                if (min >= max) {
                    throw BusinessException.badRequest(
                        String.format("属性 '%s' 的最小值必须小于最大值", name));
                }
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest(
                    String.format("属性 '%s' 的 min/max 值格式错误", name));
            }
        }
    }

    /**
     * 验证事件定义
     */
    private void validateEventDefinition(JsonNode event, Set<String> identifiers) {
        // 必填字段检查
        String identifier = getRequiredString(event, "identifier", "事件标识符不能为空");
        String name = getRequiredString(event, "name", "事件名称不能为空");

        // 检查标识符格式
        validateIdentifierFormat(identifier, "事件");

        // 检查标识符唯一性
        checkIdentifierUnique(identifiers, identifier, "事件");

        // 验证事件类型
        if (event.has("type")) {
            String type = event.get("type").asText();
            if (!type.equals("info") && !type.equals("alert") && !type.equals("fault")) {
                throw BusinessException.badRequest(
                    String.format("事件 '%s' 的类型必须是 info、alert 或 fault", name));
            }
        }
    }

    /**
     * 验证服务定义
     */
    private void validateServiceDefinition(JsonNode service, Set<String> identifiers) {
        // 必填字段检查
        String identifier = getRequiredString(service, "identifier", "服务标识符不能为空");
        String name = getRequiredString(service, "name", "服务名称不能为空");

        // 检查标识符格式
        validateIdentifierFormat(identifier, "服务");

        // 检查标识符唯一性
        checkIdentifierUnique(identifiers, identifier, "服务");

        // 验证调用方式
        if (service.has("callType")) {
            String callType = service.get("callType").asText();
            if (!callType.equals("sync") && !callType.equals("async")) {
                throw BusinessException.badRequest(
                    String.format("服务 '%s' 的调用方式必须是 sync 或 async", name));
            }
        }

        // 验证输入参数
        JsonNode inputParams = service.get("inputParams");
        if (inputParams != null && inputParams.isArray()) {
            Set<String> paramIdentifiers = new HashSet<>();
            for (JsonNode param : inputParams) {
                validateParameterDefinition(param, paramIdentifiers, "输入参数");
            }
        }

        // 验证输出参数
        JsonNode outputParams = service.get("outputParams");
        if (outputParams != null && outputParams.isArray()) {
            Set<String> paramIdentifiers = new HashSet<>();
            for (JsonNode param : outputParams) {
                validateParameterDefinition(param, paramIdentifiers, "输出参数");
            }
        }
    }

    /**
     * 验证参数定义
     */
    private void validateParameterDefinition(JsonNode param, Set<String> identifiers, String context) {
        String identifier = getRequiredString(param, "identifier", context + "标识符不能为空");
        String name = getRequiredString(param, "name", context + "名称不能为空");
        String dataType = getRequiredString(param, "dataType", context + "数据类型不能为空");

        validateIdentifierFormat(identifier, context);
        checkIdentifierUnique(identifiers, identifier, context);
        validateDataType(dataType);
    }

    /**
     * 获取必填字符串字段
     */
    private String getRequiredString(JsonNode node, String field, String errorMessage) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().trim().isEmpty()) {
            throw BusinessException.badRequest(errorMessage);
        }
        return fieldNode.asText().trim();
    }

    /**
     * 验证标识符格式
     */
    private void validateIdentifierFormat(String identifier, String context) {
        if (!identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw BusinessException.badRequest(
                String.format("%s标识符 '%s' 格式错误，只允许字母、数字、下划线，且不能以数字开头",
                    context, identifier));
        }
        if (identifier.length() > 50) {
            throw BusinessException.badRequest(
                String.format("%s标识符 '%s' 长度超过50个字符", context, identifier));
        }
    }

    /**
     * 检查标识符唯一性
     */
    private void checkIdentifierUnique(Set<String> identifiers, String identifier, String context) {
        if (identifiers.contains(identifier)) {
            throw BusinessException.badRequest(
                String.format("%s标识符 '%s' 已存在，请使用不同的标识符", context, identifier));
        }
        identifiers.add(identifier);
    }

    /**
     * 验证数据类型
     */
    private void validateDataType(String dataType) {
        Set<String> validTypes = Set.of("int", "float", "double", "string", "boolean", "date", "array", "struct");
        if (!validTypes.contains(dataType.toLowerCase())) {
            throw BusinessException.badRequest(
                String.format("不支持的数据类型 '%s'，支持的类型: %s", dataType, validTypes));
        }
    }

    /**
     * 验证设备上报数据是否符合物模型
     *
     * @param product 产品信息
     * @param propertyIdentifier 属性标识符
     * @param value 属性值
     * @return 验证通过返回 true
     */
    public boolean validatePropertyData(Product product, String propertyIdentifier, Object value) {
        if (product == null || product.getThingModel() == null) {
            return true; // 没有物模型定义，允许任意数据
        }

        JsonNode thingModel = product.getThingModel();
        JsonNode properties = thingModel.get("properties");

        if (properties == null || !properties.isArray()) {
            return true;
        }

        // 查找属性定义
        JsonNode propertyDef = null;
        for (JsonNode prop : properties) {
            if (prop.has("identifier") &&
                prop.get("identifier").asText().equals(propertyIdentifier)) {
                propertyDef = prop;
                break;
            }
        }

        if (propertyDef == null) {
            log.warn("属性 '{}' 未在物模型中定义，产品: {}", propertyIdentifier, product.getProductKey());
            return true; // 未定义的属性，允许通过但记录警告
        }

        // 验证数据类型
        String dataType = propertyDef.get("dataType").asText();
        validateValueByType(propertyIdentifier, value, dataType, propertyDef);

        return true;
    }

    /**
     * 根据数据类型验证值
     */
    private void validateValueByType(String identifier, Object value, String dataType, JsonNode propertyDef) {
        if (value == null) {
            return; // null 值允许
        }

        switch (dataType.toLowerCase()) {
            case "int":
                if (!(value instanceof Number)) {
                    throw BusinessException.badRequest(
                        String.format("属性 '%s' 期望整数类型，实际: %s", identifier, value.getClass().getSimpleName()));
                }
                checkRange(identifier, ((Number) value).doubleValue(), propertyDef);
                break;

            case "float":
            case "double":
                if (!(value instanceof Number)) {
                    throw BusinessException.badRequest(
                        String.format("属性 '%s' 期望浮点类型，实际: %s", identifier, value.getClass().getSimpleName()));
                }
                checkRange(identifier, ((Number) value).doubleValue(), propertyDef);
                break;

            case "string":
                if (!(value instanceof String)) {
                    throw BusinessException.badRequest(
                        String.format("属性 '%s' 期望字符串类型，实际: %s", identifier, value.getClass().getSimpleName()));
                }
                break;

            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw BusinessException.badRequest(
                        String.format("属性 '%s' 期望布尔类型，实际: %s", identifier, value.getClass().getSimpleName()));
                }
                break;

            default:
                // 其他类型不做严格验证
                break;
        }
    }

    /**
     * 检查数值范围
     */
    private void checkRange(String identifier, double value, JsonNode propertyDef) {
        if (propertyDef.has("min")) {
            double min = propertyDef.get("min").asDouble();
            if (value < min) {
                throw BusinessException.badRequest(
                    String.format("属性 '%s' 的值 %.2f 小于最小值 %.2f", identifier, value, min));
            }
        }

        if (propertyDef.has("max")) {
            double max = propertyDef.get("max").asDouble();
            if (value > max) {
                throw BusinessException.badRequest(
                    String.format("属性 '%s' 的值 %.2f 大于最大值 %.2f", identifier, value, max));
            }
        }
    }

    /**
     * 获取物模型的属性定义
     *
     * @param product 产品信息
     * @param identifier 属性标识符
     * @return 属性定义，不存在返回 null
     */
    public JsonNode getPropertyDefinition(Product product, String identifier) {
        if (product == null || product.getThingModel() == null) {
            return null;
        }

        JsonNode properties = product.getThingModel().get("properties");
        if (properties == null || !properties.isArray()) {
            return null;
        }

        for (JsonNode prop : properties) {
            if (prop.has("identifier") && prop.get("identifier").asText().equals(identifier)) {
                return prop;
            }
        }

        return null;
    }

    /**
     * 获取物模型的服务定义
     *
     * @param product 产品信息
     * @param identifier 服务标识符
     * @return 服务定义，不存在返回 null
     */
    public JsonNode getServiceDefinition(Product product, String identifier) {
        if (product == null || product.getThingModel() == null) {
            return null;
        }

        JsonNode services = product.getThingModel().get("services");
        if (services == null || !services.isArray()) {
            return null;
        }

        for (JsonNode service : services) {
            if (service.has("identifier") && service.get("identifier").asText().equals(identifier)) {
                return service;
            }
        }

        return null;
    }

    /**
     * 创建空的物模型结构
     */
    public ObjectNode createEmptyThingModel() {
        ObjectNode thingModel = objectMapper.createObjectNode();
        thingModel.set("properties", objectMapper.createArrayNode());
        thingModel.set("events", objectMapper.createArrayNode());
        thingModel.set("services", objectMapper.createArrayNode());
        return thingModel;
    }

    /**
     * 添加属性到物模型
     */
    public JsonNode addProperty(JsonNode thingModel, JsonNode property) {
        ObjectNode model = thingModel != null && !thingModel.isNull()
            ? thingModel.deepCopy()
            : createEmptyThingModel();

        ArrayNode properties = (ArrayNode) model.get("properties");
        if (properties == null) {
            properties = objectMapper.createArrayNode();
            model.set("properties", properties);
        }

        properties.add(property);
        return model;
    }

    // ==================== 新增方法（供 Controller 调用）====================

    /**
     * 保存物模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveThingModel(Long productId, String thingModelJson) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw BusinessException.badRequest("产品不存在");
        }
        try {
            JsonNode thingModel = objectMapper.readTree(thingModelJson);
            validateThingModel(thingModel);
            product.setThingModel(thingModel);
            productService.updateById(product);
        } catch (Exception e) {
            throw BusinessException.badRequest("物模型格式错误: " + e.getMessage());
        }
    }

    /**
     * 删除物模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteThingModel(Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw BusinessException.badRequest("产品不存在");
        }
        product.setThingModel(null);
        productService.updateById(product);
    }

    /**
     * 获取物模型的属性列表
     */
    public JsonNode getProperties(Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null || product.getThingModel() == null) {
            return objectMapper.createArrayNode();
        }
        JsonNode properties = product.getThingModel().get("properties");
        return properties != null ? properties : objectMapper.createArrayNode();
    }

    /**
     * 获取物模型的事件列表
     */
    public JsonNode getEvents(Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null || product.getThingModel() == null) {
            return objectMapper.createArrayNode();
        }
        JsonNode events = product.getThingModel().get("events");
        return events != null ? events : objectMapper.createArrayNode();
    }

    /**
     * 获取物模型的服务列表
     */
    public JsonNode getServices(Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null || product.getThingModel() == null) {
            return objectMapper.createArrayNode();
        }
        JsonNode services = product.getThingModel().get("services");
        return services != null ? services : objectMapper.createArrayNode();
    }

    /**
     * 获取产品的物模型定义
     */
    public JsonNode getThingModel(Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return null;
        }
        return product.getThingModel();
    }

    /**
     * 校验单个属性值
     */
    public void validateProperty(Long productId, String propertyIdentifier, Object value) {
        Product product = productService.getProductById(productId);
        if (product == null || product.getThingModel() == null) {
            return;
        }
        validatePropertyData(product, propertyIdentifier, value);
    }
}
