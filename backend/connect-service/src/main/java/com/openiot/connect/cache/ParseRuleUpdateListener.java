package com.openiot.connect.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 解析规则更新消息监听器
 * 监听 rule-service 发布的解析规则变更消息，更新本地缓存
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseRuleUpdateListener implements MessageListener {

    private final ParseRuleCache parseRuleCache;

    /**
     * 解析规则更新通道名称
     */
    public static final String CHANNEL = "openiot:parse_rule:update";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String msg = new String(message.getBody());
        log.info("收到解析规则更新消息: {}", msg);

        try {
            // 委托给 ParseRuleCache 处理
            parseRuleCache.onMessage(message, pattern);
        } catch (Exception e) {
            log.error("处理解析规则更新消息失败: {}", msg, e);
        }
    }
}
