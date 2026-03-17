package com.openiot.connect.parser;

import com.openiot.connect.parser.impl.JavaScriptParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaScript 解析器单元测试
 * 测试 JavaScriptParser 使用 GraalJS 沙箱执行脚本的解析能力
 *
 * @author open-iot
 */
@DisplayName("JavaScript 解析器测试")
class JavaScriptParserTest {

    private JavaScriptParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaScriptParser();
    }

    // ==================== getType 测试 ====================

    @Test
    @DisplayName("解析器类型应为 JAVASCRIPT")
    void getType_shouldReturnJavaScript() {
        assertEquals("JAVASCRIPT", parser.getType());
    }

    // ==================== 正常解析测试 ====================

    @Test
    @DisplayName("执行基本脚本 - 直接返回常量对象")
    void parse_basicScript_returnConstant() throws ParseException {
        String rawData = "any-data";
        // 脚本：返回固定对象
        String ruleConfig = """
                {
                  "script": "function parse(data) { return JSON.stringify({ status: 'ok', code: 200 }); }"
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("ok", result.get("status"));
        assertEquals(200, result.get("code"));
    }

    @Test
    @DisplayName("执行脚本 - 解析 JSON 原始数据并转换")
    void parse_jsonTransform_success() throws ParseException {
        String rawData = "{\"temperature\":250,\"humidity\":600}";
        // 脚本：解析 JSON 并做除法转换
        String ruleConfig = """
                {
                  "script": "function parse(data) { var obj = JSON.parse(data); return JSON.stringify({ temp: obj.temperature / 10.0, hum: obj.humidity / 10.0 }); }"
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(25.0, ((Number) result.get("temp")).doubleValue(), 0.001);
        assertEquals(60.0, ((Number) result.get("hum")).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("执行脚本 - 处理字符串拼接")
    void parse_stringProcessing_success() throws ParseException {
        String rawData = "DEVICE-001";
        String ruleConfig = """
                {
                  "script": "function parse(data) { return JSON.stringify({ deviceId: data.toLowerCase(), prefix: data.substring(0, 6) }); }"
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("device-001", result.get("deviceId"));
        assertEquals("DEVICE", result.get("prefix"));
    }

    @Test
    @DisplayName("执行脚本 - 使用条件逻辑")
    void parse_conditionalLogic_success() throws ParseException {
        String rawData = "{\"value\":85}";
        // 根据值大小返回不同的级别
        String ruleConfig = """
                {
                  "script": "function parse(data) { var obj = JSON.parse(data); var level = obj.value > 80 ? 'high' : 'normal'; return JSON.stringify({ value: obj.value, level: level }); }"
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(85, result.get("value"));
        assertEquals("high", result.get("level"));
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("脚本缺少 parse 函数 - 应抛出 ParseException")
    void parse_missingParseFunction_shouldThrowParseException() {
        String rawData = "test";
        // 脚本没有定义 parse 函数
        String ruleConfig = """
                {
                  "script": "function notParse(data) { return data; }"
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("脚本语法错误 - 应抛出 ParseException")
    void parse_syntaxError_shouldThrowParseException() {
        String rawData = "test";
        // JavaScript 语法错误
        String ruleConfig = """
                {
                  "script": "function parse(data { return data; }"
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        // 语法错误由 GraalJS 抛出
        assertNotNull(exception.getErrorCode());
    }

    @Test
    @DisplayName("规则配置缺少 script - 应抛出 ParseException")
    void parse_missingScript_shouldThrowParseException() {
        String rawData = "test";
        String ruleConfig = """
                {
                  "notScript": "something"
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("无效的规则配置 JSON - 应抛出 ParseException")
    void parse_invalidJsonConfig_shouldThrowParseException() {
        String rawData = "test";
        String ruleConfig = "not-a-json";

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("脚本执行运行时错误 - 应抛出 ParseException")
    void parse_runtimeError_shouldThrowParseException() {
        String rawData = "not-json";
        // 脚本尝试 JSON.parse 一个非 JSON 字符串
        String ruleConfig = """
                {
                  "script": "function parse(data) { var obj = JSON.parse(data); return JSON.stringify({ value: obj.temp }); }"
                }
                """;

        // 脚本运行时错误（JSON.parse 失败）
        assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
    }

    // ==================== validateConfig 测试 ====================

    @Test
    @DisplayName("合法配置 - 验证通过")
    void validateConfig_validConfig_shouldReturnTrue() {
        String ruleConfig = """
                {
                  "script": "function parse(data) { return data; }"
                }
                """;

        assertTrue(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("缺少 script 字段 - 验证不通过")
    void validateConfig_missingScript_shouldReturnFalse() {
        String ruleConfig = """
                { "notScript": "something" }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("空 script 字段 - 验证不通过")
    void validateConfig_emptyScript_shouldReturnFalse() {
        String ruleConfig = """
                { "script": "" }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("无效 JSON - 验证不通过")
    void validateConfig_invalidJson_shouldReturnFalse() {
        assertFalse(parser.validateConfig("not-json"));
    }
}
