package com.openiot.connect.parser;

import com.openiot.connect.parser.impl.BinaryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 二进制解析器单元测试
 * 测试 BinaryParser 对十六进制字符串的解析能力
 *
 * @author open-iot
 */
@DisplayName("二进制解析器测试")
class BinaryParserTest {

    private BinaryParser parser;

    @BeforeEach
    void setUp() {
        parser = new BinaryParser();
    }

    // ==================== getType 测试 ====================

    @Test
    @DisplayName("解析器类型应为 BINARY")
    void getType_shouldReturnBinary() {
        assertEquals("BINARY", parser.getType());
    }

    // ==================== 正常解析测试 ====================

    @Test
    @DisplayName("解析十六进制字符串 - 提取 hex 类型字段")
    void parse_hexField_success() throws ParseException {
        // 原始数据：AA BB CC DD
        String rawData = "AABBCCDD";
        // 规则配置：从偏移0提取2字节十六进制字符串
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "header", "type": "hex" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("AABB", result.get("header"));
    }

    @Test
    @DisplayName("解析十六进制字符串 - 提取 int16 类型字段（大端序）")
    void parse_int16BigEndian_success() throws ParseException {
        // 0x00 0x64 = 100
        String rawData = "0064";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "temperature", "type": "int16" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals((short) 100, result.get("temperature"));
    }

    @Test
    @DisplayName("解析十六进制字符串 - 提取 int16 类型字段（小端序）")
    void parse_int16LittleEndian_success() throws ParseException {
        // 小端序: 0x64 0x00 = 100
        String rawData = "6400";
        String ruleConfig = """
                {
                  "byteOrder": "LITTLE_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "temperature", "type": "int16" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals((short) 100, result.get("temperature"));
    }

    @Test
    @DisplayName("解析十六进制字符串 - 提取多个字段")
    void parse_multipleFields_success() throws ParseException {
        // header(2) + temp_int8(1) + humidity_int8(1) = AABB 19 32
        // 0x19 = 25, 0x32 = 50
        String rawData = "AABB1932";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "header", "type": "hex" },
                    { "offset": 2, "length": 1, "target": "temperature", "type": "int8" },
                    { "offset": 3, "length": 1, "target": "humidity", "type": "int8" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("AABB", result.get("header"));
        assertEquals(25, result.get("temperature"));  // 0x19 = 25
        assertEquals(50, result.get("humidity"));      // 0x32 = 50
    }

    @Test
    @DisplayName("解析十六进制字符串 - 提取 float 类型字段")
    void parse_floatField_success() throws ParseException {
        // 42280000 = 42.0f（IEEE 754 大端序）
        String rawData = "42280000";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 4, "target": "value", "type": "float" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(42.0f, (float) result.get("value"), 0.001f);
    }

    @Test
    @DisplayName("解析十六进制字符串 - 提取 UTF-8 字符串")
    void parse_utf8Field_success() throws ParseException {
        // "Hi" = 0x48 0x69
        String rawData = "4869";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "text", "type": "utf8" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("Hi", result.get("text"));
    }

    @Test
    @DisplayName("解析十六进制字符串 - 含空格和冒号分隔符")
    void parse_hexWithSeparators_success() throws ParseException {
        // 带分隔符的十六进制字符串
        String rawData = "AA BB:CC-DD";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 4, "target": "data", "type": "hex" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("AABBCCDD", result.get("data"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("字段超出数据范围 - 应跳过该字段")
    void parse_fieldExceedsDataLength_shouldSkip() throws ParseException {
        // 只有 2 字节数据，但配置要求从偏移 0 读取 4 字节
        String rawData = "AABB";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 4, "target": "value", "type": "int32" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        // 字段超出范围，应被跳过，结果为空
        assertFalse(result.containsKey("value"));
    }

    @Test
    @DisplayName("字段缺少 target - 应跳过该字段")
    void parse_missingTarget_shouldSkip() throws ParseException {
        String rawData = "AABB";
        // target 为空字符串
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "", "type": "hex" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("负偏移量 - 应跳过该字段")
    void parse_negativeOffset_shouldSkip() throws ParseException {
        String rawData = "AABB";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": -1, "length": 2, "target": "data", "type": "hex" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertFalse(result.containsKey("data"));
    }

    // ==================== 错误输入测试 ====================

    @Test
    @DisplayName("非法十六进制字符串 - 应抛出 ParseException")
    void parse_invalidHexData_shouldThrowParseException() {
        String rawData = "GGHHII";  // 非十六进制字符
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 1, "target": "data", "type": "hex" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("DATA_FORMAT_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("奇数长度十六进制字符串 - 应抛出 ParseException")
    void parse_oddLengthHex_shouldThrowParseException() {
        String rawData = "AAB";  // 长度为奇数
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 1, "target": "data", "type": "hex" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("DATA_FORMAT_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("规则配置缺少 fields 数组 - 应抛出 ParseException")
    void parse_missingFieldsArray_shouldThrowParseException() {
        String rawData = "AABB";
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN"
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("无效的规则配置 JSON - 应抛出 ParseException")
    void parse_invalidJsonConfig_shouldThrowParseException() {
        String rawData = "AABB";
        String ruleConfig = "not-a-json";

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    // ==================== validateConfig 测试 ====================

    @Test
    @DisplayName("合法配置 - 验证通过")
    void validateConfig_validConfig_shouldReturnTrue() {
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN",
                  "fields": [
                    { "offset": 0, "length": 2, "target": "header", "type": "hex" }
                  ]
                }
                """;

        assertTrue(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("缺少 fields 数组 - 验证不通过")
    void validateConfig_missingFields_shouldReturnFalse() {
        String ruleConfig = """
                {
                  "byteOrder": "BIG_ENDIAN"
                }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("空 fields 数组 - 验证不通过")
    void validateConfig_emptyFields_shouldReturnFalse() {
        String ruleConfig = """
                {
                  "fields": []
                }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("无效 JSON - 验证不通过")
    void validateConfig_invalidJson_shouldReturnFalse() {
        assertFalse(parser.validateConfig("not-json"));
    }
}
