package com.openiot.data.consumer;

import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.data.service.DeviceStatusService;
import com.openiot.data.service.TrajectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 实时消费者
 * 处理设备状态和轨迹数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeConsumer {

    private final TrajectoryService trajectoryService;
    private final DeviceStatusService deviceStatusService;

    @KafkaListener(
            topics = "device-events",
            groupId = "realtime-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope event = record.value();
        log.debug("实时消费事件: eventId={}, deviceId={}", event.getEventId(), event.getDeviceId());

        try {
            // 更新设备状态
            deviceStatusService.updateOnlineStatus(
                    event.getTenantId(),
                    event.getDeviceId(),
                    true
            );

            // 处理轨迹数据
            if ("TELEMETRY".equals(event.getEventType())) {
                trajectoryService.saveTrajectory(event);
            }

            // 确认消息
            ack.acknowledge();

        } catch (Exception e) {
            log.error("实时消费异常: eventId={}", event.getEventId(), e);
            // 不确认，触发重试
        }
    }
}
