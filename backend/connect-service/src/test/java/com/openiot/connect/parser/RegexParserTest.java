package com.openiot.connect.parser;

import com.openiot.connect.parser.impl.RegexParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 正则表达式解析器单元测试
 * 测试 RegexParser 的正则匹配、分组提取和类型转换能力
 *
 * @author open-iot
 */
@DisplayName("正则表达式解析器测试")
class RegexParserTest {

    private RegexParser parser;

    @BeforeEach
    void setUp() {
        parser = new RegexParser();
    }

    // ==================== getType 测试 ====================

    @Test
    @DisplayName("解析器类型应为 REGEX")
    void getType_shouldReturnRegex() {
        assertEquals("REGEX", parser.getType());
    }

    // ==================== 正常解析测试 ====================

    @Test
    @DisplayName("正则匹配 - 提取数字分组")
    void parse_numericGroups_success() throws ParseException {
        // 模拟设备上报格式: temp=25&hum=60
        String rawData = "temp=25&hum=60";
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)&hum=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "temperature", "type": "int" },
                    { "index": 2, "target": "humidity", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(25, result.get("temperature"));
        assertEquals(60, result.get("humidity"));
    }

    @Test
    @DisplayName("正则匹配 - 提取浮点数分组")
    void parse_doubleGroups_success() throws ParseException {
        String rawData = "voltage:3.72V current:1.5A";
        String ruleConfig = """
                {
                  "pattern": "voltage:([\\\\d.]+)V current:([\\\\d.]+)A",
                  "groups": [
                    { "index": 1, "target": "voltage", "type": "double" },
                    { "index": 2, "target": "current", "type": "double" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(3.72, result.get("voltage"));
        assertEquals(1.5, result.get("current"));
    }

    @Test
    @DisplayName("正则匹配 - 提取字符串分组（默认类型）")
    void parse_stringGroups_success() throws ParseException {
        String rawData = "device=sensor-01 status=online";
        String ruleConfig = """
                {
                  "pattern": "device=(\\\\S+) status=(\\\\S+)",
                  "groups": [
                    { "index": 1, "target": "deviceName", "type": "string" },
                    { "index": 2, "target": "status" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("sensor-01", result.get("deviceName"));
        assertEquals("online", result.get("status"));
    }

    @Test
    @DisplayName("正则匹配 - 提取整个匹配（index=0）")
    void parse_fullMatchGroup_success() throws ParseException {
        String rawData = "2026-03-16 ALARM device001";
        String ruleConfig = """
                {
                  "pattern": "(\\\\d{4}-\\\\d{2}-\\\\d{2}) (\\\\w+) (\\\\w+)",
                  "groups": [
                    { "index": 0, "target": "fullMatch", "type": "string" },
                    { "index": 1, "target": "date", "type": "string" },
                    { "index": 3, "target": "deviceId", "type": "string" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals("2026-03-16 ALARM device001", result.get("fullMatch"));
        assertEquals("2026-03-16", result.get("date"));
        assertEquals("device001", result.get("deviceId"));
    }

    @Test
    @DisplayName("正则匹配 - boolean 类型转换")
    void parse_booleanType_success() throws ParseException {
        String rawData = "alarm=true level=3";
        String ruleConfig = """
                {
                  "pattern": "alarm=(\\\\w+) level=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "hasAlarm", "type": "boolean" },
                    { "index": 2, "target": "level", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(true, result.get("hasAlarm"));
        assertEquals(3, result.get("level"));
    }

    // ==================== 不匹配场景测试 ====================

    @Test
    @DisplayName("正则不匹配 - 应抛出 ParseException")
    void parse_noMatch_shouldThrowParseException() {
        String rawData = "completely-different-format";
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)&hum=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "temperature", "type": "int" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("DATA_FORMAT_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("分组索引超出范围 - 应跳过该分组")
    void parse_groupIndexOutOfRange_shouldSkip() throws ParseException {
        String rawData = "temp=25";
        // 正则只有 1 个分组，但配置引用 index=5
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "temperature", "type": "int" },
                    { "index": 5, "target": "phantom", "type": "string" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertEquals(25, result.get("temperature"));
        // 超出范围的分组不应出现
        assertFalse(result.containsKey("phantom"));
    }

    @Test
    @DisplayName("分组缺少 target - 应跳过该分组")
    void parse_missingTarget_shouldSkip() throws ParseException {
        String rawData = "temp=25";
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "", "type": "int" }
                  ]
                }
                """;

        Map<String, Object> result = parser.parse(rawData, ruleConfig);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 错误输入测试 ====================

    @Test
    @DisplayName("非法正则表达式 - 应抛出 ParseException")
    void parse_invalidRegexPattern_shouldThrowParseException() {
        String rawData = "test data";
        // 非法正则：未闭合的括号
        String ruleConfig = """
                {
                  "pattern": "temp=((\\\\d+",
                  "groups": [
                    { "index": 1, "target": "temp", "type": "int" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("规则配置缺少 pattern - 应抛出 ParseException")
    void parse_missingPattern_shouldThrowParseException() {
        String rawData = "test data";
        String ruleConfig = """
                {
                  "groups": [
                    { "index": 1, "target": "temp", "type": "int" }
                  ]
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("规则配置缺少 groups - 应抛出 ParseException")
    void parse_missingGroups_shouldThrowParseException() {
        String rawData = "temp=25";
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)"
                }
                """;

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("无效的规则配置 JSON - 应抛出 ParseException")
    void parse_invalidJsonConfig_shouldThrowParseException() {
        String rawData = "test data";
        String ruleConfig = "broken-json";

        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(rawData, ruleConfig));
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
    }

    // ==================== validateConfig 测试 ====================

    @Test
    @DisplayName("合法配置 - 验证通过")
    void validateConfig_validConfig_shouldReturnTrue() {
        String ruleConfig = """
                {
                  "pattern": "temp=(\\\\d+)",
                  "groups": [
                    { "index": 1, "target": "temperature", "type": "int" }
                  ]
                }
                """;

        assertTrue(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("缺少 pattern - 验证不通过")
    void validateConfig_missingPattern_shouldReturnFalse() {
        String ruleConfig = """
                {
                  "groups": [
                    { "index": 1, "target": "temperature", "type": "int" }
                  ]
                }
                """;

        assertFalse(parser.validateConfig(ruleConfig));
    }

    @Test
    @DisplayName("非法正则 - 验证不通过")
    void validateConfig_invalidRegex_shouldReturnFalse() {
        String ruleConfig = """
                {
                  "pattern": "((unclosed",
                  "groups": []
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
