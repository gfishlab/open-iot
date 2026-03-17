package com.openiot.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性，支持 Nacos 动态刷新
 *
 * <p>通过 Nacos 配置中心可动态调整限流参数，无需重启服务。
 * 配置前缀为 {@code openiot.rate-limit}。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "openiot.rate-limit")
public class RateLimitProperties {

    /** 每秒令牌填充速率（每秒允许的请求数） */
    private int replenishRate = 50;

    /** 令牌桶突发容量（最大突发请求数） */
    private int burstCapacity = 100;
}
