package com.openiot.gateway.controller;

import lombok.Data;
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
    public Mono<HealthVO> health() {
        HealthVO health = new HealthVO();
        health.setStatus("UP");
        health.setTimestamp(System.currentTimeMillis());
        health.setService("gateway-service");
        return Mono.just(health);
    }

    @GetMapping("/info")
    public Mono<InfoVO> info() {
        InfoVO info = new InfoVO();
        info.setName("Open-IoT Gateway Service");
        info.setVersion("1.0.0");
        info.setDescription("API 网关服务");
        return Mono.just(info);
    }

    /**
     * 健康检查响应 VO
     */
    @Data
    public static class HealthVO {
        private String status;
        private Long timestamp;
        private String service;
    }

    /**
     * 服务信息响应 VO
     */
    @Data
    public static class InfoVO {
        private String name;
        private String version;
        private String description;
    }
}
