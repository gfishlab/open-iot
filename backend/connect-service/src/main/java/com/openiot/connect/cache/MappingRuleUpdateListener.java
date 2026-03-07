package com.openiot.connect.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 映射规则更新消息监听器
 * 监听 rule-service 发布的映射规则变更消息，更新本地缓存
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingRuleUpdateListener implements MessageListener {

    private final MappingRuleCache mappingRuleCache;

    /**
     * 映射规则更新通道名称
     */
    public static final String CHANNEL = "openiot:mapping_rule:update";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String msg = new String(message.getBody());
        log.info("收到映射规则更新消息: {}", msg);

        try {
            mappingRuleCache.handleRuleUpdate(msg);
        } catch (Exception e) {
            log.error("处理映射规则更新消息失败: {}", msg, e);
        }
    }
}
