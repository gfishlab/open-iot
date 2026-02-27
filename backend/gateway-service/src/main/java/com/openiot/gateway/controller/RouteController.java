package com.openiot.gateway.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<RefreshResponse> refreshRoutes() {
        publisher.publishEvent(new RefreshRoutesEvent(this));

        RefreshResponse response = new RefreshResponse();
        response.setCode(200);
        response.setMsg("路由刷新成功");
        response.setTimestamp(System.currentTimeMillis());
        return Mono.just(response);
    }

    /**
     * 路由刷新响应 VO
     */
    @Data
    public static class RefreshResponse {
        private Integer code;
        private String msg;
        private Long timestamp;
    }
}
