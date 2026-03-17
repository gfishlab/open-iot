package com.openiot.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关熔断降级回退控制器
 *
 * <p>当下游服务不可用（熔断器打开）时，Gateway 的 CircuitBreaker 过滤器
 * 会将请求转发到此控制器，返回统一的 503 降级响应。
 *
 * <p>每条路由通过 {@code fallbackUri: forward:/fallback/{serviceName}} 配置
 * 指向对应的回退端点。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@RestController
public class FallbackController {

    /**
     * 通用服务降级回退端点
     *
     * <p>当熔断器处于打开状态时，返回 503 Service Unavailable 响应，
     * 告知客户端目标服务暂时不可用。
     *
     * @param serviceName 不可用的目标服务名称（由路由配置传入）
     * @return 统一格式的降级响应 JSON
     */
    @GetMapping("/fallback/{serviceName}")
    public Mono<Map<String, Object>> fallback(
            @PathVariable(name = "serviceName") String serviceName) {

        log.warn("服务熔断降级触发: serviceName={}", serviceName);

        Map<String, Object> response = new HashMap<>(4);
        response.put("code", 503);
        response.put("msg", serviceName + "暂时不可用，请稍后重试");
        response.put("data", null);
        response.put("timestamp", System.currentTimeMillis());

        return Mono.just(response);
    }
}
