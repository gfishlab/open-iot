package com.openiot.data.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.common.mongodb.document.RawEventDocument;
import com.openiot.data.entity.DeviceTrajectory;
import com.openiot.data.mapper.DeviceTrajectoryMapper;
import com.openiot.data.service.TrajectoryService.TrajectoryPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轨迹解析器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrajectoryParser {

    private final DeviceTrajectoryMapper trajectoryMapper;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 解析并保存轨迹数据
     */
    public void parseAndSave(RawEventDocument rawEvent) {
        if (!"TELEMETRY".equals(rawEvent.getEventType())) {
            markAsProcessed(rawEvent, "SKIPPED");
            return;
        }

        try {
            Object payload = rawEvent.getParsedPayload();
            if (payload == null) {
                markAsProcessed(rawEvent, "NO_PAYLOAD");
                return;
            }

            // 转换为轨迹载荷对象
            TrajectoryPayload trajectoryPayload;
            if (payload instanceof TrajectoryPayload) {
                trajectoryPayload = (TrajectoryPayload) payload;
            } else {
                trajectoryPayload = objectMapper.convertValue(payload, TrajectoryPayload.class);
            }

            // 构建轨迹实体
            DeviceTrajectory trajectory = new DeviceTrajectory();
            trajectory.setTenantId(Long.valueOf(rawEvent.getTenantId()));
            trajectory.setDeviceId(Long.valueOf(rawEvent.getDeviceId()));
            trajectory.setEventTime(rawEvent.getTimestamp());
            trajectory.setCreateTime(LocalDateTime.now());

            // 解析轨迹字段
            if (trajectoryPayload.getLatitude() != null) {
                trajectory.setLatitude(trajectoryPayload.getLatitude());
            }
            if (trajectoryPayload.getLongitude() != null) {
                trajectory.setLongitude(trajectoryPayload.getLongitude());
            }
            if (trajectoryPayload.getSpeed() != null) {
                trajectory.setSpeed(trajectoryPayload.getSpeed());
            }
            if (trajectoryPayload.getHeading() != null) {
                trajectory.setHeading(trajectoryPayload.getHeading());
            }

            // 保存到 PostgreSQL
            trajectoryMapper.insert(trajectory);

            // 标记为已处理
            markAsProcessed(rawEvent, "SUCCESS");

            log.debug("轨迹解析成功: eventId={}, deviceId={}", rawEvent.getEventId(), rawEvent.getDeviceId());

        } catch (Exception e) {
            log.error("轨迹解析失败: eventId={}", rawEvent.getEventId(), e);
            markAsProcessed(rawEvent, "FAILED:" + e.getMessage());
        }
    }

    private void markAsProcessed(RawEventDocument event, String result) {
        event.setProcessed(true);
        event.setProcessResult(result);
        event.setUpdateTime(LocalDateTime.now());
        mongoTemplate.save(event);
    }
}
