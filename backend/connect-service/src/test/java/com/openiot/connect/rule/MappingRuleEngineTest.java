package com.openiot.connect.rule;

import com.openiot.connect.mapper.MappingRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 映射规则引擎单元测试
 * 测试 MappingRuleEngine 的字段映射、单位转换和公式计算能力
 *
 * @author open-iot
 */
@DisplayName("映射规则引擎测试")
class MappingRuleEngineTest {

    private MappingRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MappingRuleEngine();
    }

    // ==================== applyMapping（Map 输入）测试 ====================

    @Test
    @DisplayName("简单字段重命名映射 - NONE 类型")
    void applyMapping_simpleRename_success() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25.5);
        parsedData.put("hum", 60);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "NONE" },
                    { "sourceField": "hum", "targetProperty": "humidity", "transformType": "NONE" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(25.5, result.get("temperature"));
        assertEquals(60, result.get("humidity"));
    }

    @Test
    @DisplayName("默认 transformType 为 NONE - 直接赋值")
    void applyMapping_defaultTransformTypeIsNone() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);

        // 不指定 transformType，默认使用 NONE
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        assertEquals(25, result.get("temperature"));
    }

    @Test
    @DisplayName("源字段不存在 - 应跳过")
    void applyMapping_sourceFieldNotExist_shouldSkip() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "nonExistent", "targetProperty": "value", "transformType": "NONE" },
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "NONE" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.containsKey("value"));
        assertEquals(25, result.get("temperature"));
    }

    @Test
    @DisplayName("未映射的源字段不保留")
    void applyMapping_unmappedFieldsNotRetained() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);
        parsedData.put("extra", "unmapped-value");

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "NONE" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.containsKey("extra"));
    }

    // ==================== 单位转换测试 ====================

    @Test
    @DisplayName("单位转换 - 摄氏度转华氏度")
    void applyMapping_unitConvert_celsiusToFahrenheit() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 100.0);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "UNIT_CONVERT", "transformConfig": "C_TO_F" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 100 * 1.8 + 32 = 212
        assertEquals(212.0, (double) result.get("temperature"), 0.001);
    }

    @Test
    @DisplayName("单位转换 - 华氏度转摄氏度")
    void applyMapping_unitConvert_fahrenheitToCelsius() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 212.0);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "UNIT_CONVERT", "transformConfig": "F_TO_C" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // (212 - 32) / 1.8 = 100
        assertEquals(100.0, (double) result.get("temperature"), 0.001);
    }

    @Test
    @DisplayName("单位转换 - 百分比转小数")
    void applyMapping_unitConvert_percentToDecimal() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("hum", 65);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "hum", "targetProperty": "humidity", "transformType": "UNIT_CONVERT", "transformConfig": "PERCENT_TO_DECIMAL" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 65 / 100 = 0.65
        assertEquals(0.65, (double) result.get("humidity"), 0.001);
    }

    @Test
    @DisplayName("单位转换 - 公里转英里")
    void applyMapping_unitConvert_kmToMile() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("distance", 10.0);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "distance", "targetProperty": "distance_mi", "transformType": "UNIT_CONVERT", "transformConfig": "KM_TO_MILE" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 10 * 0.621371 = 6.21371
        assertEquals(6.21371, (double) result.get("distance_mi"), 0.001);
    }

    @Test
    @DisplayName("单位转换 - 未知转换类型返回原值")
    void applyMapping_unitConvert_unknownType_returnOriginal() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("value", 42.0);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "value", "targetProperty": "result", "transformType": "UNIT_CONVERT", "transformConfig": "UNKNOWN_UNIT" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 未知转换类型，返回原值
        assertEquals(42.0, result.get("result"));
    }

    @Test
    @DisplayName("单位转换 - 非数值输入返回原值")
    void applyMapping_unitConvert_nonNumericInput_returnOriginal() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("value", "not-a-number");

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "value", "targetProperty": "result", "transformType": "UNIT_CONVERT", "transformConfig": "C_TO_F" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 无法转换为数字，返回原值
        assertEquals("not-a-number", result.get("result"));
    }

    // ==================== 公式计算测试 ====================

    @Test
    @DisplayName("公式计算 - 简单乘法")
    void applyMapping_formula_multiplication() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("raw", 250);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "raw", "targetProperty": "temperature", "transformType": "FORMULA", "transformConfig": "value / 10" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 250 / 10 = 25
        assertEquals(25L, result.get("temperature"));
    }

    @Test
    @DisplayName("公式计算 - 摄氏度转华氏度公式")
    void applyMapping_formula_celsiusToFahrenheit() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("celsius", 100);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "celsius", "targetProperty": "fahrenheit", "transformType": "FORMULA", "transformConfig": "value * 1.8 + 32" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 100 * 1.8 + 32 = 212.0
        assertEquals(212.0, ((Number) result.get("fahrenheit")).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("公式计算 - 条件表达式（三元运算符）")
    void applyMapping_formula_ternaryExpression() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("raw", 150);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "raw", "targetProperty": "capped", "transformType": "FORMULA", "transformConfig": "value > 100 ? 100 : value" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 150 > 100 ? 100 : 150 = 100
        assertEquals(100L, result.get("capped"));
    }

    @Test
    @DisplayName("公式计算 - 空 transformConfig 返回原值")
    void applyMapping_formula_emptyConfig_returnOriginal() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("value", 42);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "value", "targetProperty": "result", "transformType": "FORMULA", "transformConfig": "" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 空公式，返回原值
        assertEquals(42, result.get("result"));
    }

    // ==================== applyMapping（JSON 字符串输入）测试 ====================

    @Test
    @DisplayName("JSON 字符串输入 - 正常映射")
    void applyMappingJson_success() {
        String parsedDataJson = """
                { "temp": 25, "hum": 60 }
                """;

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "NONE" },
                    { "sourceField": "hum", "targetProperty": "humidity", "transformType": "NONE" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedDataJson, fieldMappings);

        assertNotNull(result);
        assertEquals(25, result.get("temperature"));
        assertEquals(60, result.get("humidity"));
    }

    @Test
    @DisplayName("JSON 字符串输入 - 无效 JSON 返回空 Map")
    void applyMappingJson_invalidJson_returnEmpty() {
        String parsedDataJson = "not-json";
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedDataJson, fieldMappings);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 空输入/边界条件测试 ====================

    @Test
    @DisplayName("输入数据为 null - 返回空 Map")
    void applyMapping_nullInput_returnEmpty() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping((Map<String, Object>) null, fieldMappings);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("输入数据为空 Map - 返回空 Map")
    void applyMapping_emptyInput_returnEmpty() {
        Map<String, Object> parsedData = new HashMap<>();
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature" }
                  ]
                }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("映射配置为 null - 返回原始数据")
    void applyMapping_nullMappings_returnOriginalData() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);

        Map<String, Object> result = engine.applyMapping(parsedData, null);

        assertNotNull(result);
        assertEquals(25, result.get("temp"));
    }

    @Test
    @DisplayName("映射配置为空字符串 - 返回原始数据")
    void applyMapping_emptyMappings_returnOriginalData() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);

        Map<String, Object> result = engine.applyMapping(parsedData, "");

        assertNotNull(result);
        assertEquals(25, result.get("temp"));
    }

    @Test
    @DisplayName("映射配置格式错误（缺少 mappings 数组）- 返回原始数据")
    void applyMapping_invalidMappingFormat_returnOriginalData() {
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("temp", 25);

        String fieldMappings = """
                { "noMappings": true }
                """;

        Map<String, Object> result = engine.applyMapping(parsedData, fieldMappings);

        assertNotNull(result);
        // 格式错误回退为返回原始数据
        assertEquals(25, result.get("temp"));
    }

    // ==================== validateConfig 测试 ====================

    @Test
    @DisplayName("合法配置（NONE）- 验证通过")
    void validateConfig_validNoneConfig_shouldReturnTrue() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "NONE" }
                  ]
                }
                """;

        assertTrue(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("合法配置（FORMULA）- 验证通过")
    void validateConfig_validFormulaConfig_shouldReturnTrue() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "raw", "targetProperty": "value", "transformType": "FORMULA", "transformConfig": "value * 10" }
                  ]
                }
                """;

        assertTrue(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("合法配置（UNIT_CONVERT）- 验证通过")
    void validateConfig_validUnitConvertConfig_shouldReturnTrue() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "UNIT_CONVERT", "transformConfig": "C_TO_F" }
                  ]
                }
                """;

        assertTrue(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("缺少 sourceField - 验证不通过")
    void validateConfig_missingSourceField_shouldReturnFalse() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "", "targetProperty": "temperature", "transformType": "NONE" }
                  ]
                }
                """;

        assertFalse(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("缺少 targetProperty - 验证不通过")
    void validateConfig_missingTargetProperty_shouldReturnFalse() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "", "transformType": "NONE" }
                  ]
                }
                """;

        assertFalse(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("非法 transformType - 验证不通过")
    void validateConfig_invalidTransformType_shouldReturnFalse() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "INVALID_TYPE" }
                  ]
                }
                """;

        assertFalse(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("FORMULA 缺少 transformConfig - 验证不通过")
    void validateConfig_formulaMissingConfig_shouldReturnFalse() {
        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "temp", "targetProperty": "temperature", "transformType": "FORMULA" }
                  ]
                }
                """;

        assertFalse(engine.validateConfig(fieldMappings));
    }

    @Test
    @DisplayName("无效 JSON - 验证不通过")
    void validateConfig_invalidJson_shouldReturnFalse() {
        assertFalse(engine.validateConfig("not-json"));
    }

    // ==================== clearCache 测试 ====================

    @Test
    @DisplayName("清理缓存 - 不应抛出异常")
    void clearCache_shouldNotThrow() {
        // 先执行一次公式计算触发缓存
        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("raw", 10);

        String fieldMappings = """
                {
                  "mappings": [
                    { "sourceField": "raw", "targetProperty": "value", "transformType": "FORMULA", "transformConfig": "value * 2" }
                  ]
                }
                """;
        engine.applyMapping(parsedData, fieldMappings);

        // 清理缓存不应抛出异常
        assertDoesNotThrow(() -> engine.clearCache());
    }
}
