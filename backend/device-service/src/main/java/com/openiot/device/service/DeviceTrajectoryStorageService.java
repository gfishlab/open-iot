package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.DeviceTrajectory;
import com.openiot.device.mapper.DeviceTrajectoryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 设备轨迹存储服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTrajectoryStorageService {

    private final DeviceTrajectoryMapper trajectoryMapper;
    private final DeviceService deviceService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 保存单个轨迹点
     *
     * @param trajectory 轨迹点
     * @return 保存后的轨迹点
     */
    @Transactional(rollbackFor = Exception.class)
    public DeviceTrajectory save(DeviceTrajectory trajectory) {
        // 验证设备权限
        Device device = validateDeviceAccess(trajectory.getDeviceId());

        // 设置租户ID
        trajectory.setTenantId(device.getTenantId());

        // 保存到数据库
        trajectoryMapper.insert(trajectory);

        // 发布轨迹事件
        publishTrajectoryEvent(trajectory);

        log.debug("保存设备轨迹点: deviceId={}, lat={}, lng={}",
                trajectory.getDeviceId(), trajectory.getLatitude(), trajectory.getLongitude());

        return trajectory;
    }

    /**
     * 批量保存轨迹点
     *
     * @param trajectories 轨迹点列表
     * @return 保存成功的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchSave(List<DeviceTrajectory> trajectories) {
        if (trajectories == null || trajectories.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DeviceTrajectory trajectory : trajectories) {
            try {
                save(trajectory);
                count++;
            } catch (Exception e) {
                log.error("保存轨迹点失败: deviceId={}, error={}",
                        trajectory.getDeviceId(), e.getMessage());
            }
        }

        log.info("批量保存轨迹点: total={}, success={}", trajectories.size(), count);
        return count;
    }

    /**
     * 查询设备轨迹
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 轨迹点列表
     */
    public List<DeviceTrajectory> query(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证设备权限
        validateDeviceAccess(deviceId);

        LambdaQueryWrapper<DeviceTrajectory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTrajectory::getDeviceId, deviceId)
                .between(DeviceTrajectory::getEventTime, startTime, endTime)
                .orderByAsc(DeviceTrajectory::getEventTime);

        return trajectoryMapper.selectList(wrapper);
    }

    /**
     * 分页查询设备轨迹
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    public Page<DeviceTrajectory> queryPage(Long deviceId, LocalDateTime startTime, LocalDateTime endTime,
                                            int pageNum, int pageSize) {
        // 验证设备权限
        validateDeviceAccess(deviceId);

        Page<DeviceTrajectory> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceTrajectory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTrajectory::getDeviceId, deviceId)
                .between(DeviceTrajectory::getEventTime, startTime, endTime)
                .orderByAsc(DeviceTrajectory::getEventTime);

        return trajectoryMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询最新轨迹点
     *
     * @param deviceId 设备ID
     * @return 最新轨迹点
     */
    public DeviceTrajectory getLatest(Long deviceId) {
        // 验证设备权限
        validateDeviceAccess(deviceId);

        LambdaQueryWrapper<DeviceTrajectory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTrajectory::getDeviceId, deviceId)
                .orderByDesc(DeviceTrajectory::getEventTime)
                .last("LIMIT 1");

        return trajectoryMapper.selectOne(wrapper);
    }

    /**
     * 删除过期轨迹数据
     *
     * @param beforeTime 删除此时间之前的数据
     * @return 删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpired(LocalDateTime beforeTime) {
        LambdaQueryWrapper<DeviceTrajectory> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(DeviceTrajectory::getEventTime, beforeTime);

        int count = trajectoryMapper.delete(wrapper);
        log.info("删除过期轨迹数据: beforeTime={}, count={}", beforeTime, count);

        return count;
    }

    /**
     * 计算轨迹距离
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 总距离（米）
     */
    public double calculateDistance(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<DeviceTrajectory> trajectories = query(deviceId, startTime, endTime);

        if (trajectories.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 1; i < trajectories.size(); i++) {
            DeviceTrajectory prev = trajectories.get(i - 1);
            DeviceTrajectory curr = trajectories.get(i);
            totalDistance += haversineDistance(
                    prev.getLatitude().doubleValue(), prev.getLongitude().doubleValue(),
                    curr.getLatitude().doubleValue(), curr.getLongitude().doubleValue()
            );
        }

        return totalDistance;
    }

    /**
     * 发布轨迹事件到 Kafka
     */
    private void publishTrajectoryEvent(DeviceTrajectory trajectory) {
        try {
            var message = Map.of(
                    "type", "trajectory",
                    "deviceId", trajectory.getDeviceId(),
                    "latitude", trajectory.getLatitude(),
                    "longitude", trajectory.getLongitude(),
                    "speed", trajectory.getSpeed() != null ? trajectory.getSpeed() : 0,
                    "heading", trajectory.getHeading() != null ? trajectory.getHeading() : 0,
                    "eventTime", trajectory.getEventTime().toString(),
                    "timestamp", LocalDateTime.now().toString()
            );

            kafkaTemplate.send("device-trajectory", objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("发布轨迹事件失败: {}", e.getMessage());
        }
    }

    /**
     * 计算两点之间的 Haversine 距离（米）
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 验证设备访问权限
     */
    private Device validateDeviceAccess(Long deviceId) {
        Device device = deviceService.getById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }

        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }

        return device;
    }

    /**
     * 轨迹统计 VO
     */
    @Data
    public static class TrajectoryStatisticsVO {
        private Long deviceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer pointCount;
        private Double totalDistance; // 米
        private Double avgSpeed;       // km/h
        private Double maxSpeed;       // km/h
        private LocalDateTime firstPointTime;
        private LocalDateTime lastPointTime;
    }
}
