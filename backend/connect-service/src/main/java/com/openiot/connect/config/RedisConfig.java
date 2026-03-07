package com.openiot.connect.config;

import com.openiot.connect.cache.MappingRuleUpdateListener;
import com.openiot.connect.cache.ParseRuleUpdateListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis 配置类
 * 配置 Redis Pub/Sub 消息监听器
 *
 * @author open-iot
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * 配置 Redis 消息监听容器
     * 监听解析规则和映射规则的更新消息
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ParseRuleUpdateListener parseRuleUpdateListener,
            MappingRuleUpdateListener mappingRuleUpdateListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 添加解析规则更新监听器
        container.addMessageListener(parseRuleUpdateListener,
                new PatternTopic(ParseRuleUpdateListener.CHANNEL));

        // 添加映射规则更新监听器
        container.addMessageListener(mappingRuleUpdateListener,
                new PatternTopic(MappingRuleUpdateListener.CHANNEL));

        return container;
    }
}
