package com.openiot.common.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 连接池配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.lettuce.pool")
public class RedisPoolProperties {

    /**
     * 连接池最大连接数（负值表示无限制）
     */
    private int maxActive = 16;

    /**
     * 连接池最大空闲连接数
     */
    private int maxIdle = 8;

    /**
     * 连接池最小空闲连接数
     */
    private int minIdle = 2;

    /**
     * 连接池最大阻塞等待时间（毫秒，负值表示无限制）
     */
    private long maxWait = 5000;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectTimeout = 3000;

    /**
     * 读取超时时间（毫秒）
     */
    private long readTimeout = 3000;

    /**
     * 空闲连接检测间隔（毫秒）
     */
    private long timeBetweenEvictionRuns = 60000;

    /**
     * 最小空闲时间（毫秒），超过此时间的空闲连接将被回收
     */
    private long minEvictableIdleTime = 300000;
}
