package com.openiot.data.controller;

import com.openiot.common.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Set;

/**
 * SSE 控制器
 * 用于实时推送设备数据
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final RedisUtil redisUtil;

    private static final String TRAJECTORY_KEY_PREFIX = "device:trajectory:";

    /**
     * 实时轨迹推送
     */
    @GetMapping(value = "/devices/{deviceId}/trajectory/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Object> streamTrajectory(
            @PathVariable(value = "deviceId") String deviceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {

        if (tenantId == null) {
            tenantId = "1"; // 默认租户
        }

        String key = TRAJECTORY_KEY_PREFIX + tenantId + ":" + deviceId;
        final String finalTenantId = tenantId;

        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> {
                    try {
                        // 获取最新轨迹点
                        Set<Object> points = redisUtil.zReverseRange(key, 0, 0);
                        if (points != null && !points.isEmpty()) {
                            return points.iterator().next();
                        }
                        return "{}";
                    } catch (Exception e) {
                        log.error("获取轨迹失败", e);
                        return "{}";
                    }
                })
                .doOnSubscribe(s -> log.info("SSE连接: deviceId={}", deviceId))
                .doOnCancel(() -> log.info("SSE断开: deviceId={}", deviceId));
    }
}
