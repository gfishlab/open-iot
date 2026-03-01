package com.openiot.common.redis.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 健康检查配置
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisHealthConfig {

    /**
     * Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                redisConnectionFactory.getConnection().ping();
                return Health.up()
                        .withDetail("redis", "available")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("redis", "unavailable")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
