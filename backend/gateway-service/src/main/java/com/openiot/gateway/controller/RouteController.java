package com.openiot.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态路由控制器
 * 提供路由查看和刷新接口
 */
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class RouteController {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final ApplicationEventPublisher publisher;

    /**
     * 获取所有路由信息
     */
    @GetMapping("/routes")
    public Flux<RouteDefinition> getRoutes() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    /**
     * 刷新路由配置
     */
    @PostMapping("/routes/refresh")
    public Mono<Map<String, Object>> refreshRoutes() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "路由刷新成功");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }
}
