package com.openiot.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置
 *
 * <p>基于 Redis 令牌桶算法的请求限流，按客户端 IP 维度进行速率控制。
 * 与 Gateway 路由中的 {@code RequestRateLimiter} 过滤器配合使用。
 *
 * <p>限流参数在 application.yml 的路由 filters 中配置：
 * <ul>
 *   <li>{@code redis-rate-limiter.replenishRate} - 每秒令牌填充速率</li>
 *   <li>{@code redis-rate-limiter.burstCapacity} - 令牌桶容量（突发上限）</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    /**
     * 基于客户端 IP 的限流 Key 解析器
     *
     * <p>从请求的远程地址中提取客户端 IP 作为限流维度的 Key，
     * 每个 IP 独立计算令牌桶，互不影响。
     *
     * @return KeyResolver 实例
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String hostAddress = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            log.debug("限流 Key 解析: ip={}", hostAddress);
            return Mono.just(hostAddress);
        };
    }
}
