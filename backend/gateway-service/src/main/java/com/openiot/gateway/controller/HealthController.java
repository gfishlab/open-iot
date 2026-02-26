package com.openiot.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "gateway-service");
        return Mono.just(health);
    }

    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Open-IoT Gateway Service");
        info.put("version", "1.0.0");
        info.put("description", "API 网关服务");
        return Mono.just(info);
    }
}
