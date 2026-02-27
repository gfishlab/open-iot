package com.openiot.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.common.redis.util.RedisUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 轨迹服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrajectoryService {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    private static final String TRAJECTORY_KEY_PREFIX = "device:trajectory:";
    private static final long MAX_POINTS = 100;
    private static final long TRAJECTORY_TTL = 3600; // 1小时

    /**
     * 保存轨迹点
     */
    public void saveTrajectory(EventEnvelope event) {
        try {
            String key = TRAJECTORY_KEY_PREFIX + event.getTenantId() + ":" + event.getDeviceId();

            // 解析轨迹数据
            TrajectoryPointVO point = parseTrajectoryPoint(event);
            if (point == null) {
                return;
            }

            // 存入 ZSET（按时间戳排序）
            String pointJson = objectMapper.writeValueAsString(point);
            redisUtil.zAdd(key, pointJson, event.getTimestamp());

            // 限制轨迹点数量
            Long size = redisUtil.zSize(key);
            if (size > MAX_POINTS) {
                redisUtil.zRemoveRange(key, 0, size - MAX_POINTS - 1);
            }

            // 设置过期时间
            redisUtil.expire(key, TRAJECTORY_TTL);

            log.debug("保存轨迹点: deviceId={}", event.getDeviceId());

        } catch (Exception e) {
            log.error("保存轨迹失败: deviceId={}", event.getDeviceId(), e);
        }
    }

    /**
     * 解析轨迹点
     */
    private TrajectoryPointVO parseTrajectoryPoint(EventEnvelope event) {
        Object payload = event.getPayload();
        if (payload == null) {
            return null;
        }

        TrajectoryPointVO point = new TrajectoryPointVO();
        point.setTimestamp(event.getTimestamp());

        // 尝试从 payload 中提取字段
        if (payload instanceof TrajectoryPayload trajectoryPayload) {
            point.setLatitude(trajectoryPayload.getLatitude());
            point.setLongitude(trajectoryPayload.getLongitude());
            point.setSpeed(trajectoryPayload.getSpeed());
            point.setHeading(trajectoryPayload.getHeading());
        } else {
            log.warn("无法解析轨迹数据: {}", payload.getClass().getName());
            return null;
        }

        return point;
    }

    /**
     * 轨迹点 VO（用于 Redis 存储和返回）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrajectoryPointVO {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal speed;
        private BigDecimal heading;
        private Long timestamp;
    }

    /**
     * 轨迹载荷结构（Kafka 消息中的 payload 结构）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrajectoryPayload {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal speed;
        private BigDecimal heading;
    }
}
