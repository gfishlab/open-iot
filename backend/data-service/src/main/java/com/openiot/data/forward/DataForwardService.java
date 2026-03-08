package com.openiot.data.forward;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.data.entity.DataForwardConfig;
import com.openiot.data.entity.DataForwardLog;
import com.openiot.data.mapper.DataForwardConfigMapper;
import com.openiot.data.mapper.DataForwardLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 数据转发服务
 * 支持多目标数据转发：HTTP、Kafka、MQTT
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataForwardService {

    private final DataForwardConfigMapper configMapper;
    private final DataForwardLogMapper logMapper;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 异步转发线程池
     */
    private final Executor forwardExecutor = Executors.newFixedThreadPool(10);

    /**
     * 转发设备数据
     *
     * @param tenantId  租户ID
     * @param deviceId  设备ID
     * @param deviceCode 设备编码
     * @param dataType  数据类型
     * @param data      数据内容
     */
    public void forwardData(Long tenantId, Long deviceId, String deviceCode,
                            String dataType, Map<String, Object> data) {
        // 查询启用的转发配置
        LambdaQueryWrapper<DataForwardConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataForwardConfig::getTenantId, tenantId)
               .eq(DataForwardConfig::getStatus, "1")
               .eq(DataForwardConfig::getDelFlag, "0");
        List<DataForwardConfig> configs = configMapper.selectList(wrapper);

        if (configs.isEmpty()) {
            return;
        }

        // 异步转发到多个目标
        for (DataForwardConfig config : configs) {
            CompletableFuture.runAsync(() -> {
                try {
                    forwardToTarget(config, deviceId, deviceCode, dataType, data);
                } catch (Exception e) {
                    log.error("数据转发失败: configId={}, deviceId={}", config.getId(), deviceId, e);
                }
            }, forwardExecutor);
        }
    }

    /**
     * 转发数据到指定目标
     */
    private void forwardToTarget(DataForwardConfig config, Long deviceId, String deviceCode,
                                 String dataType, Map<String, Object> data) {
        long startTime = System.currentTimeMillis();
        DataForwardLog forwardLog = new DataForwardLog();
        forwardLog.setTenantId(config.getTenantId());
        forwardLog.setConfigId(config.getId());
        forwardLog.setDeviceId(deviceId);
        forwardLog.setDeviceCode(deviceCode);
        forwardLog.setDataType(dataType);
        forwardLog.setTarget(config.getTargetType());
        forwardLog.setForwardTime(LocalDateTime.now());
        forwardLog.setCreateTime(LocalDateTime.now());

        try {
            // 检查过滤条件
            if (!matchFilter(config, deviceId, data)) {
                return; // 不匹配过滤条件，跳过
            }

            // 数据转换
            Map<String, Object> transformedData = transformData(config, data);

            // 根据目标类型转发
            switch (config.getTargetType().toLowerCase()) {
                case "http":
                    forwardToHttp(config, transformedData, forwardLog);
                    break;
                case "kafka":
                    forwardToKafka(config, transformedData, forwardLog);
                    break;
                case "mqtt":
                    forwardToMqtt(config, transformedData, forwardLog);
                    break;
                default:
                    forwardLog.setForwardStatus("fail");
                    forwardLog.setErrorMessage("不支持的目标类型: " + config.getTargetType());
            }
        } catch (Exception e) {
            forwardLog.setForwardStatus("fail");
            forwardLog.setErrorMessage(e.getMessage());
            log.error("转发异常: configId={}, deviceId={}", config.getId(), deviceId, e);
        } finally {
            forwardLog.setDuration(System.currentTimeMillis() - startTime);
            logMapper.insert(forwardLog);
        }
    }

    /**
     * 检查是否匹配过滤条件
     */
    private boolean matchFilter(DataForwardConfig config, Long deviceId, Map<String, Object> data) {
        if (config.getFilterCondition() == null || config.getFilterCondition().isEmpty()) {
            return true; // 无过滤条件，全部通过
        }

        try {
            JsonNode filter = objectMapper.readTree(config.getFilterCondition());

            // 检查设备ID过滤
            if (filter.has("deviceIds")) {
                JsonNode deviceIds = filter.get("deviceIds");
                boolean matched = false;
                for (JsonNode id : deviceIds) {
                    if (id.asLong() == deviceId) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }

            // 检查属性过滤
            if (filter.has("propertyIdentifiers")) {
                JsonNode properties = filter.get("propertyIdentifiers");
                for (JsonNode prop : properties) {
                    if (data.containsKey(prop.asText())) {
                        return true;
                    }
                }
                return false;
            }

            return true;
        } catch (JsonProcessingException e) {
            log.error("解析过滤条件失败", e);
            return true;
        }
    }

    /**
     * 数据转换
     */
    private Map<String, Object> transformData(DataForwardConfig config, Map<String, Object> data) {
        if (config.getTransformRule() == null || config.getTransformRule().isEmpty()) {
            return data;
        }

        try {
            JsonNode rule = objectMapper.readTree(config.getTransformRule());

            // 简单模板替换
            if (rule.has("template")) {
                String template = rule.get("template").asText();
                String result = template;

                // 替换占位符
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                }

                Map<String, Object> transformed = new HashMap<>();
                transformed.put("data", result);
                return transformed;
            }

            return data;
        } catch (JsonProcessingException e) {
            log.error("解析转换规则失败", e);
            return data;
        }
    }

    /**
     * 转发到HTTP端点
     */
    private void forwardToHttp(DataForwardConfig config, Map<String, Object> data, DataForwardLog forwardLog) {
        try {
            JsonNode endpoint = objectMapper.readTree(config.getEndpointConfig());
            String url = endpoint.get("url").asText();
            String method = endpoint.has("method") ? endpoint.get("method").asText() : "POST";

            // 发送HTTP请求
            Map<String, Object> response = restTemplate.postForObject(url, data, Map.class);

            forwardLog.setForwardStatus("success");
            forwardLog.setResponse(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            forwardLog.setForwardStatus("fail");
            forwardLog.setErrorMessage(e.getMessage());

            // 重试
            if (config.getRetryTimes() != null && config.getRetryTimes() > 0) {
                retryForward(config, data, forwardLog);
            }
        }
    }

    /**
     * 转发到Kafka
     */
    private void forwardToKafka(DataForwardConfig config, Map<String, Object> data, DataForwardLog forwardLog) {
        try {
            JsonNode endpoint = objectMapper.readTree(config.getEndpointConfig());
            String topic = endpoint.get("topic").asText();

            kafkaTemplate.send(topic, objectMapper.writeValueAsString(data))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            forwardLog.setForwardStatus("fail");
                            forwardLog.setErrorMessage(ex.getMessage());
                        } else {
                            forwardLog.setForwardStatus("success");
                        }
                    });
        } catch (Exception e) {
            forwardLog.setForwardStatus("fail");
            forwardLog.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 转发到MQTT
     */
    private void forwardToMqtt(DataForwardConfig config, Map<String, Object> data, DataForwardLog forwardLog) {
        // MQTT转发需要集成MQTT客户端
        // 这里先记录日志，后续实现
        forwardLog.setForwardStatus("fail");
        forwardLog.setErrorMessage("MQTT转发功能待实现");
        log.warn("MQTT转发功能待实现: configId={}", config.getId());
    }

    /**
     * 重试转发
     */
    private void retryForward(DataForwardConfig config, Map<String, Object> data, DataForwardLog forwardLog) {
        int retryTimes = config.getRetryTimes() != null ? config.getRetryTimes() : 0;
        long retryInterval = config.getRetryIntervalMs() != null ? config.getRetryIntervalMs() : 1000;

        for (int i = 0; i < retryTimes; i++) {
            try {
                Thread.sleep(retryInterval);

                // 根据目标类型重试
                if ("http".equalsIgnoreCase(config.getTargetType())) {
                    forwardToHttp(config, data, forwardLog);
                }

                if ("success".equals(forwardLog.getForwardStatus())) {
                    forwardLog.setRetryCount(i + 1);
                    break;
                }
            } catch (Exception e) {
                log.error("重试失败: retryCount={}", i + 1, e);
            }
        }
    }
}
