package com.openiot.connect.parser;

import com.openiot.connect.parser.impl.JsonPathParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON 路径解析器单元测试
 * 测试 JsonPathParser 对 JSON 数据的路径提取和类型转换能力
 *
 * @author open-iot
 */
@DisplayName("JSON 路径解析器测试")
class JsonPathParserTest {

    private JsonPathParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsonPathParser();
    }

    // ==================== getType 测试 ====================

    @Test
    @DisplayName("解析器类型应为 JSON")
    void getType_shouldReturnJson() {
        assertEquals("JSON", parser.getType());
    }

    // ==================== 正常解析测试 ====================

    @Test
    @DisplayName("解析简单 JSON - 提取顶层字段")
    void parse_simpleJson_success() throws ParseException {
        String rawData = """
                { "temperature": 25.5, "humidity": 60 }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.temperature", "target": "temp", "type": "double" },
                    { "source": "$.humidity", "target": "hum", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(25.5, result.get("temp"));
        assertEquals(60, result.get("hum"));
    }

    @Test
    @DisplayName("解析嵌套 JSON - 提取嵌套对象字段")
    void parse_nestedJson_success() throws ParseException {
        String rawData = """
                {
                  "device": {
                    "sensor": {
                      "temperature": 30.2,
                      "unit": "celsius"
                    }
                  }
                }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.device.sensor.temperature", "target": "temp", "type": "double" },
                    { "source": "$.device.sensor.unit", "target": "unit", "type": "string" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(30.2, result.get("temp"));
        assertEquals("celsius", result.get("unit"));
    }

    @Test
    @DisplayName("解析 JSON 数组 - 通过索引提取元素")
    void parse_arrayIndex_success() throws ParseException {
        String rawData = """
                {
                  "sensors": [
                    { "name": "temp", "value": 25 },
                    { "name": "hum", "value": 60 }
                  ]
                }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.sensors[0].value", "target": "firstSensor", "type": "int" },
                    { "source": "$.sensors[1].value", "target": "secondSensor", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(25, result.get("firstSensor"));
        assertEquals(60, result.get("secondSensor"));
    }

    @Test
    @DisplayName("解析 JSON - 布尔类型转换")
    void parse_booleanType_success() throws ParseException {
        String rawData = """
                { "online": true, "alarm": false }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.online", "target": "isOnline", "type": "boolean" },
                    { "source": "$.alarm", "target": "hasAlarm", "type": "boolean" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(true, result.get("isOnline"));
        assertEquals(false, result.get("hasAlarm"));
    }

    @Test
    @DisplayName("解析 JSON - long 类型转换")
    void parse_longType_success() throws ParseException {
        String rawData = """
                { "timestamp": 1700000000000 }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.timestamp", "target": "ts", "type": "long" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(1700000000000L, result.get("ts"));
    }

    @Test
    @DisplayName("解析 JSON - 默认类型为 string")
    void parse_defaultTypeIsString_success() throws ParseException {
        String rawData = """
                { "name": "sensor-01" }
                """;
        // 不指定 type，默认为 string
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.name", "target": "deviceName" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("sensor-01", result.get("deviceName"));
    }

    // ==================== 路径不存在测试 ====================

    @Test
    @DisplayName("路径不存在 - 对应字段不应出现在结果中")
    void parse_nonExistentPath_shouldNotIncludeField() throws ParseException {
        String rawData = """
                { "temperature": 25.5 }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.nonExistent", "target": "missing", "type": "string" },
                    { "source": "$.temperature", "target": "temp", "type": "double" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        // 不存在的路径不应出现在结果中
        assertFalse(result.containsKey("missing"));
        // 存在的路径正常提取
        assertEquals(25.5, result.get("temp"));
    }

    @Test
    @DisplayName("数组索引越界 - 对应字段不应出现在结果中")
    void parse_arrayIndexOutOfBounds_shouldNotIncludeField() throws ParseException {
        String rawData = """
                { "items": [1, 2, 3] }
                """;
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.items[99]", "target": "item", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertFalse(result.containsKey("item"));
    }

    // ==================== 错误输入测试 ====================

    @Test
    @DisplayName("无效 JSON 原始数据 - 应抛出 ParseException")
    void parse_invalidJsonData_shouldThrowParseException() {
        String rawData = "not-a-json";
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.field", "target": "f", "type": "string" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("DATA_FORMAT_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("规则配置缺少 mappings 数组 - 应抛出 ParseException")
    void parse_missingMappingsArray_shouldThrowParseException() {
        String rawData = """
                { "temperature": 25 }
                """;
        String ruleConfig = """
                { "noMappings": true }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("无效的规则配置 JSON - 应抛出 ParseException")
    void parse_invalidJsonConfig_shouldThrowParseException() {
        String rawData = """
                { "temperature": 25 }
                """;
        String ruleConfig = "broken-json{{{";

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    // ==================== validateConfig 测试 ====================

    @Test
    @DisplayName("合法配置 - 验证通过")
    void validateConfig_validConfig_shouldReturnTrue() {
        String ruleConfig = """
                {
                  "mappings": [
                    { "source": "$.temperature", "target": "temp", "type": "double" }
                  ]
                }
                """;

        assertTrue(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("缺少 mappings 数组 - 验证不通过")
    void validateConfig_missingMappings_shouldReturnFalse() {
        String ruleConfig = """
                { "noMappings": true }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("无效 JSON - 验证不通过")
    void validateConfig_invalidJson_shouldReturnFalse() {
        assertFalse(parser.validateConfig("not-json"));
    }

    @Test
    @DisplayName("空 mappings 数组 - 验证通过（空数组是合法的）")
    void validateConfig_emptyMappingsArray_shouldReturnTrue() {
        String ruleConfig = """
                { "mappings": [] }
                """;

        // 空数组也是一个合法的 isArray()，所以通过验证
        assertTrue(parser.validateConfig(ruleConfig));
    }
}
