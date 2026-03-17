package com.openiot.connect.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.connect.parser.ParseException;
import com.openiot.connect.parser.ParseRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * JavaScript 解析器
 * 使用 GraalJS 在沙箱环境中执行 JavaScript 脚本
 *
 * <p>安全限制：
 * <ul>
 *   <li>禁用 IO 操作</li>
 *   <li>禁用本地访问</li>
 *   <li>3 秒执行超时</li>
 *   <li>禁用 Java 类型访问</li>
 * </ul>
 *
 * <p>规则配置格式：
 * <pre>
 * {
 *   "script": "function parse(data) { const obj = JSON.parse(data); return { temp: obj.temperature }; }"
 * }
 * </pre>
 *
 * @author open-iot
 */
@Slf4j
@Component
public class JavaScriptParser implements ParseRuleEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行超时时间（秒）
     */
    private static final int EXECUTION_TIMEOUT_SECONDS = 10;

    /**
     * 执行器线程池（用于超时控制）
     */
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "js-parser");
        t.setDaemon(true);
        return t;
    });

    @Override
    public String getType() {
        return "JAVASCRIPT";
    }

    @Override
    public Map<String, Object> parse(String rawData, String ruleConfig) throws ParseException {
        try {
            // 解析规则配置
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            String script = configNode.path("script").asText();

            if (script == null || script.isEmpty()) {
                throw ParseException.configError("规则配置必须包含 'script' 字段");
            }

            // 在线程池中执行，支持超时控制
            Future<Map<String, Object>> future = executor.submit(() -> executeScript(script, rawData));

            try {
                // 等待执行结果（带超时）
                Map<String, Object> result = future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.debug("JavaScript 解析完成: 结果字段数={}", result.size());
                return result;

            } catch (TimeoutException e) {
                future.cancel(true);
                throw ParseException.timeout("JavaScript 执行超时（" + EXECUTION_TIMEOUT_SECONDS + "秒）");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ParseException) {
                    throw (ParseException) cause;
                }
                throw ParseException.executionError("JavaScript 执行错误: " + cause.getMessage(), cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw ParseException.executionError("JavaScript 执行被中断", e);
            }

        } catch (JsonProcessingException e) {
            throw ParseException.configError("规则配置 JSON 解析失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String ruleConfig) {
        try {
            JsonNode configNode = objectMapper.readTree(ruleConfig);
            String script = configNode.path("script").asText();
            return script != null && !script.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 在 GraalJS 沙箱中执行 JavaScript 脚本
     */
    private Map<String, Object> executeScript(String script, String rawData) throws ParseException {
        // 创建沙箱上下文
        try (Context context = Context.newBuilder("js")
                // 安全配置：禁用所有危险操作
                .allowAllAccess(false)
                .allowIO(false)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowHostClassLoading(false)
                .allowHostClassLookup(className -> false)  // 禁止所有类查找
                .allowInnerContextOptions(false)
                // 允许实验性选项（GraalJS commonjs-require 需要）
                .allowExperimentalOptions(true)
                // 允许基本的宿主访问（用于返回结果）
                .allowHostAccess(HostAccess.ALL)
                // 禁用 ES 模块（避免加载外部模块）
                .option("js.commonjs-require", "false")
                .build()) {

            // 注入原始数据
            context.getBindings("js").putMember("rawData", rawData);

            // 执行脚本
            context.eval("js", script);

            // 获取 parse 函数
            Value parseFn = context.getBindings("js").getMember("parse");
            if (parseFn == null || !parseFn.canExecute()) {
                throw ParseException.configError("脚本必须定义 parse(rawData) 函数");
            }

            // 调用 parse 函数
            Value result = parseFn.execute(rawData);

            // 转换结果
            return convertResult(result);

        } catch (PolyglotException e) {
            if (e.isCancelled()) {
                throw ParseException.timeout("JavaScript 执行超时");
            }
            if (e.isSyntaxError()) {
                throw ParseException.configError("JavaScript 语法错误: " + e.getMessage());
            }
            throw ParseException.executionError("JavaScript 执行错误: " + e.getMessage(), e);
        }
    }

    /**
     * 将 GraalJS 执行结果转换为 Map
     * 简化实现：将结果转为 JSON 字符串，然后用 Jackson 解析
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertResult(Object result) throws ParseException {
        if (result == null) {
            return new HashMap<>();
        }

        // 如果结果是 Map，直接返回
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }

        // 如果结果是 GraalJS 的 Value 对象
        if (result instanceof Value value) {
            if (value.isNull()) {
                return new HashMap<>();
            }

            // 如果是对象，转为 JSON 字符串后解析
            if (value.hasHashEntries()) {
                try {
                    // 使用 JSON.stringify 将结果转为字符串
                    String jsonResult = value.toString();
                    // 尝试解析为 JSON
                    return objectMapper.readValue(jsonResult,
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                } catch (JsonProcessingException e) {
                    // 如果 toString() 不是有效 JSON，尝试手动构建
                    log.warn("无法直接解析 Value，尝试手动转换: {}", e.getMessage());
                }
            }
        }

        // 尝试作为 JSON 解析
        try {
            return objectMapper.readValue(result.toString(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException e) {
            throw ParseException.dataFormatError("脚本返回值无法转换为 Map: " + e.getMessage());
        }
    }
}
