package com.openiot.data.alarm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.data.entity.AlarmRecord;
import com.openiot.data.entity.AlarmRule;
import com.openiot.data.mapper.AlarmRecordMapper;
import com.openiot.data.mapper.AlarmRuleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 告警处理服务
 * 负责告警规则的评估、触发和通知
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRuleMapper ruleMapper;
    private final AlarmRecordMapper recordMapper;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 设备状态缓存，用于持续告警检测
     * Key: deviceId_propertyIdentifier
     * Value: 状态变更时间+值
     */
    private final ConcurrentHashMap<String, DeviceState> deviceStateCache = new ConcurrentHashMap<>();

    /**
     * 处理设备数据，检查是否触发告警
     *
     * @param tenantId    租户ID
     * @param deviceId    设备ID
     * @param deviceCode  设备编码
     * @param productId   产品ID
     * @param propertyData 属性数据
     */
    public void processDeviceData(Long tenantId, Long deviceId, String deviceCode,
                                  Long productId, Map<String, Object> propertyData) {
        // 查询启用的告警规则
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRule::getTenantId, tenantId)
               .eq(AlarmRule::getStatus, "1")
               .eq(AlarmRule::getDelFlag, "0")
               .and(w -> w.isNull(AlarmRule::getDeviceId).or().eq(AlarmRule::getDeviceId, deviceId))
               .and(w -> w.isNull(AlarmRule::getProductId).or().eq(AlarmRule::getProductId, productId));
        List<AlarmRule> rules = ruleMapper.selectList(wrapper);

        for (AlarmRule rule : rules) {
            try {
                evaluateRule(rule, deviceId, deviceCode, productId, propertyData);
            } catch (Exception e) {
                log.error("告警规则评估失败: ruleId={}, deviceId={}", rule.getId(), deviceId, e);
            }
        }
    }

    /**
     * 评估告警规则
     */
    private void evaluateRule(AlarmRule rule, Long deviceId, String deviceCode,
                              Long productId, Map<String, Object> propertyData) {
        try {
            boolean triggered = false;
            String triggerValue = null;

            JsonNode condition = objectMapper.readTree(rule.getTriggerCondition());

            switch (rule.getTriggerType()) {
                case "threshold":
                    triggered = evaluateThreshold(condition, propertyData);
                    triggerValue = extractTriggerValue(condition, propertyData);
                    break;

                case "expression":
                    triggered = evaluateExpression(condition, propertyData);
                    triggerValue = propertyData.toString();
                    break;

                case "rate":
                    triggered = evaluateRate(rule, condition, propertyData, deviceId);
                    triggerValue = "Rate change detected";
                    break;
            }

            if (triggered) {
                handleAlarmTriggered(rule, deviceId, deviceCode, productId, triggerValue);
            } else {
                handleAlarmRecovered(rule, deviceId, deviceCode, productId, propertyData);
            }
        } catch (JsonProcessingException e) {
            log.error("解析告警条件失败", e);
        }
    }

    /**
     * 评估阈值条件
     */
    private boolean evaluateThreshold(JsonNode condition, Map<String, Object> propertyData) {
        String property = condition.get("property").asText();
        String operator = condition.get("operator").asText();
        double threshold = condition.get("value").asDouble();

        Object value = propertyData.get(property);
        if (value == null) {
            return false;
        }

        double actualValue = ((Number) value).doubleValue();

        return switch (operator) {
            case ">" -> actualValue > threshold;
            case ">=" -> actualValue >= threshold;
            case "<" -> actualValue < threshold;
            case "<=" -> actualValue <= threshold;
            case "==" -> actualValue == threshold;
            case "!=" -> actualValue != threshold;
            default -> false;
        };
    }

    /**
     * 评估表达式条件（简化版，实际可使用 Aviator 或 GraalJS）
     */
    private boolean evaluateExpression(JsonNode condition, Map<String, Object> propertyData) {
        String expression = condition.get("expression").asText();

        // 简单实现：替换变量并评估
        // 实际生产环境建议使用 Aviator 或 GraalJS
        String evalExpr = expression;
        for (Map.Entry<String, Object> entry : propertyData.entrySet()) {
            evalExpr = evalExpr.replace("${" + entry.getKey() + "}",
                    String.valueOf(entry.getValue()));
        }

        // 检查是否还有未替换的变量（${...}），如有则表示缺少数据，不触发
        if (evalExpr.contains("${")) {
            return false;
        }

        // 这里简化处理，实际应该使用表达式引擎
        return !evalExpr.contains("null") && !evalExpr.contains("undefined");
    }

    /**
     * 评估变化率条件
     */
    private boolean evaluateRate(AlarmRule rule, JsonNode condition,
                                 Map<String, Object> propertyData, Long deviceId) {
        String property = condition.get("property").asText();
        double changeThreshold = condition.get("change").asDouble();

        Object value = propertyData.get(property);
        if (value == null) {
            return false;
        }

        double currentValue = ((Number) value).doubleValue();
        String key = deviceId + "_" + property;

        DeviceState prevState = deviceStateCache.get(key);
        if (prevState == null) {
            // 首次记录，不触发告警
            deviceStateCache.put(key, new DeviceState(currentValue, System.currentTimeMillis()));
            return false;
        }

        // 计算变化率
        double rate = Math.abs(currentValue - prevState.value);
        boolean triggered = rate >= changeThreshold;

        // 更新状态
        deviceStateCache.put(key, new DeviceState(currentValue, System.currentTimeMillis()));

        return triggered;
    }

    /**
     * 提取触发值
     */
    private String extractTriggerValue(JsonNode condition, Map<String, Object> propertyData) {
        String property = condition.get("property").asText();
        Object value = propertyData.get(property);
        return property + "=" + (value != null ? value.toString() : "null");
    }

    /**
     * 处理告警触发
     */
    private void handleAlarmTriggered(AlarmRule rule, Long deviceId, String deviceCode,
                                      Long productId, String triggerValue) {
        String cacheKey = rule.getId() + "_" + deviceId;

        // 检查是否已存在活动告警
        LambdaQueryWrapper<AlarmRecord> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(AlarmRecord::getRuleId, rule.getId())
                    .eq(AlarmRecord::getDeviceId, deviceId)
                    .in(AlarmRecord::getAlarmStatus, Arrays.asList("active", "pending"));
        AlarmRecord activeAlarm = recordMapper.selectOne(activeWrapper);

        if (activeAlarm != null) {
            // 已有活动告警，更新触发值
            activeAlarm.setTriggerValue(triggerValue);
            activeAlarm.setUpdateTime(LocalDateTime.now());
            recordMapper.updateById(activeAlarm);
            return;
        }

        // 检查静默期
        if (rule.getSilenceEnabled() != null && rule.getSilenceEnabled()) {
            LambdaQueryWrapper<AlarmRecord> silenceWrapper = new LambdaQueryWrapper<>();
            silenceWrapper.eq(AlarmRecord::getRuleId, rule.getId())
                         .eq(AlarmRecord::getDeviceId, deviceId)
                         .orderByDesc(AlarmRecord::getAlarmTime)
                         .last("LIMIT 1");
            AlarmRecord lastAlarm = recordMapper.selectOne(silenceWrapper);

            LocalDateTime lastAlarmTime = lastAlarm != null ? lastAlarm.getAlarmTime() : null;

            if (lastAlarmTime != null) {
                long silenceSeconds = rule.getSilenceSeconds() != null ? rule.getSilenceSeconds() : 300;
                if (lastAlarmTime.plusSeconds(silenceSeconds).isAfter(LocalDateTime.now())) {
                    log.debug("告警处于静默期内: ruleId={}, deviceId={}", rule.getId(), deviceId);
                    return;
                }
            }
        }

        // 检查持续时间
        if (rule.getDurationSeconds() != null && rule.getDurationSeconds() > 0) {
            DeviceState state = deviceStateCache.computeIfAbsent(cacheKey,
                    k -> new DeviceState(0, System.currentTimeMillis()));

            long duration = (System.currentTimeMillis() - state.timestamp) / 1000;
            if (duration < rule.getDurationSeconds()) {
                log.debug("未达到持续时间要求: ruleId={}, required={}s, actual={}s",
                        rule.getId(), rule.getDurationSeconds(), duration);
                return;
            }
        }

        // 创建新告警
        AlarmRecord record = new AlarmRecord();
        record.setTenantId(rule.getTenantId());
        record.setRuleId(rule.getId());
        record.setProductId(productId);
        record.setDeviceId(deviceId);
        record.setDeviceCode(deviceCode);
        record.setAlarmLevel(rule.getAlarmLevel());
        record.setAlarmTitle(rule.getRuleName());
        record.setAlarmContent(rule.getContentTemplate());
        record.setTriggerValue(triggerValue);
        record.setAlarmStatus("active");
        record.setAlarmTime(LocalDateTime.now());
        record.setNotifyStatus("none");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        recordMapper.insert(record);

        log.warn("告警触发: ruleId={}, deviceId={}, level={}, content={}",
                rule.getId(), deviceId, rule.getAlarmLevel(), rule.getRuleName());

        // 发送通知
        sendNotification(rule, record);
    }

    /**
     * 处理告警恢复
     */
    private void handleAlarmRecovered(AlarmRule rule, Long deviceId, String deviceCode,
                                      Long productId, Map<String, Object> propertyData) {
        // 查询活动告警
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRecord::getRuleId, rule.getId())
               .eq(AlarmRecord::getDeviceId, deviceId)
               .in(AlarmRecord::getAlarmStatus, Arrays.asList("active", "pending"));
        List<AlarmRecord> activeAlarms = recordMapper.selectList(wrapper);

        if (activeAlarms.isEmpty()) {
            return;
        }

        // 更新告警状态为已恢复
        for (AlarmRecord alarm : activeAlarms) {
            alarm.setAlarmStatus("resolved");
            alarm.setRecoverTime(LocalDateTime.now());
            alarm.setUpdateTime(LocalDateTime.now());
            recordMapper.updateById(alarm);

            log.info("告警恢复: alarmId={}, deviceId={}", alarm.getId(), deviceId);
        }
    }

    /**
     * 发送告警通知
     */
    private void sendNotification(AlarmRule rule, AlarmRecord record) {
        if ("none".equals(rule.getNotifyType())) {
            return;
        }

        try {
            switch (rule.getNotifyType()) {
                case "email":
                    sendEmailNotification(rule, record);
                    break;
                case "sms":
                    sendSmsNotification(rule, record);
                    break;
                case "webhook":
                    sendWebhookNotification(rule, record);
                    break;
            }

            record.setNotifyStatus("sent");
        } catch (Exception e) {
            log.error("告警通知发送失败: alarmId={}", record.getId(), e);
            record.setNotifyStatus("failed");
        } finally {
            recordMapper.updateById(record);
        }
    }

    /**
     * 发送邮件通知（占位实现）
     */
    private void sendEmailNotification(AlarmRule rule, AlarmRecord record) {
        log.info("发送邮件通知: alarmId={}, recipients=[{}]",
                record.getId(), rule.getNotifyConfig());
        // 实际实现需要集成邮件服务
    }

    /**
     * 发送短信通知（占位实现）
     */
    private void sendSmsNotification(AlarmRule rule, AlarmRecord record) {
        log.info("发送短信通知: alarmId={}", record.getId());
        // 实际实现需要集成短信服务
    }

    /**
     * 发送 Webhook 通知
     */
    private void sendWebhookNotification(AlarmRule rule, AlarmRecord record) {
        try {
            JsonNode config = objectMapper.readTree(rule.getNotifyConfig());
            String url = config.get("url").asText();

            Map<String, Object> payload = new HashMap<>();
            payload.put("alarmId", record.getId());
            payload.put("ruleId", rule.getId());
            payload.put("deviceId", record.getDeviceId());
            payload.put("deviceCode", record.getDeviceCode());
            payload.put("alarmLevel", record.getAlarmLevel());
            payload.put("alarmTitle", record.getAlarmTitle());
            payload.put("alarmContent", record.getAlarmContent());
            payload.put("triggerValue", record.getTriggerValue());
            payload.put("alarmTime", record.getAlarmTime().toString());

            restTemplate.postForObject(url, payload, String.class);
        } catch (JsonProcessingException e) {
            log.error("解析通知配置失败", e);
        }
    }

    /**
     * 清理过期的状态缓存
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cleanExpiredCache() {
        long expireTime = System.currentTimeMillis() - 3600000; // 1小时前
        deviceStateCache.entrySet().removeIf(entry ->
                entry.getValue().timestamp < expireTime);
    }

    /**
     * 设备状态记录
     */
    @Data
    private static class DeviceState {
        private final double value;
        private final long timestamp;
    }
}
