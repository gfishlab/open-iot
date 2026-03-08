package com.openiot.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.AlertRecordMapper;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.metrics.AlertMetrics;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警服务
 *
 * @author OpenIoT Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService extends ServiceImpl<AlertRecordMapper, AlertRecord> {

    private final DeviceMapper deviceMapper;
    private final com.openiot.device.mapper.RuleMapper ruleMapper;
    private final AlertMetrics alertMetrics;

    /**
     * 分页查询告警记录
     *
     * @param page     页码
     * @param size     每页大小
     * @param deviceId 设备ID（可选）
     * @param level    告警级别（可选）
     * @param status   处理状态（可选）
     * @return 分页结果
     */
    public Page<AlertRecord> queryAlerts(int page, int size, Long deviceId, String level, String status) {
        Page<AlertRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();

        // 租户隔离
        String tenantId = TenantContext.getTenantId();
        wrapper.eq(AlertRecord::getTenantId, Long.valueOf(tenantId));

        // 条件过滤
        if (deviceId != null) {
            wrapper.eq(AlertRecord::getDeviceId, deviceId);
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq(AlertRecord::getAlertLevel, level);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlertRecord::getStatus, status);
        }

        wrapper.orderByDesc(AlertRecord::getCreateTime);

        return this.page(pageParam, wrapper);
    }

    /**
     * 查询告警详情
     *
     * @param alertId 告警ID
     * @return 告警详情
     */
    public AlertRecord getAlertDetail(Long alertId) {
        AlertRecord alert = this.getById(alertId);
        if (alert == null) {
            throw BusinessException.notFound("告警记录不存在");
        }

        // 租户权限检查
        String tenantId = TenantContext.getTenantId();
        if (!alert.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该告警记录");
        }

        return alert;
    }

    /**
     * 处理告警
     *
     * @param alertId 告警ID
     * @param status  新状态
     * @return 更新的告警记录
     */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord handleAlert(Long alertId, String status) {
        AlertRecord alert = getAlertDetail(alertId);

        // 验证状态流转
        if (!isValidStatusTransition(alert.getStatus(), status)) {
            throw BusinessException.badRequest("无效的状态转换: " + alert.getStatus() + " -> " + status);
        }

        // 更新状态
        LambdaUpdateWrapper<AlertRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AlertRecord::getId, alertId)
               .set(AlertRecord::getStatus, status)
               .set(AlertRecord::getHandledTime, LocalDateTime.now());

        if ("resolved".equals(status) || "ignored".equals(status)) {
            String userId = TenantContext.getUserId();
            if (userId != null) {
                wrapper.set(AlertRecord::getHandledBy, Long.valueOf(userId));
            }
        }

        this.update(wrapper);

        // 记录告警处理指标
        String tenantId = TenantContext.getTenantId();
        alertMetrics.recordAlarmHandled(tenantId, alertId, status);

        log.info("处理告警: alertId={}, status={}, handledBy={}", alertId, status,
                TenantContext.getUserId());

        return this.getById(alertId);
    }

    /**
     * 批量处理告警
     *
     * @param alertIds 告警ID列表
     * @param status   新状态
     * @return 处理数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchHandleAlerts(List<Long> alertIds, String status) {
        String tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId() != null ? Long.valueOf(TenantContext.getUserId()) : null;

        LambdaUpdateWrapper<AlertRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(AlertRecord::getId, alertIds)
               .eq(AlertRecord::getTenantId, Long.valueOf(tenantId))
               .set(AlertRecord::getStatus, status)
               .set(AlertRecord::getHandledTime, LocalDateTime.now());

        if (userId != null) {
            wrapper.set(AlertRecord::getHandledBy, userId);
        }

        boolean updated = this.update(wrapper);
        int count = updated ? alertIds.size() : 0;

        // 记录告警批量处理指标
        alertMetrics.recordAlarmBatchHandled(tenantId, count, status);

        log.info("批量处理告警: count={}, status={}, handledBy={}", count, status, userId);

        return count;
    }

    /**
     * 查询告警统计
     *
     * @param deviceId 设备ID（可选）
     * @return 统计数据
     */
    public AlertStatisticsVO getStatistics(Long deviceId) {
        String tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getTenantId, Long.valueOf(tenantId));

        if (deviceId != null) {
            wrapper.eq(AlertRecord::getDeviceId, deviceId);
        }

        List<AlertRecord> allAlerts = this.list(wrapper);

        AlertStatisticsVO statistics = new AlertStatisticsVO();
        statistics.setTotalCount((long) allAlerts.size());

        // 按级别统计
        Map<String, Long> levelCount = new HashMap<>();
        levelCount.put("critical", allAlerts.stream().filter(a -> "critical".equals(a.getAlertLevel())).count());
        levelCount.put("warning", allAlerts.stream().filter(a -> "warning".equals(a.getAlertLevel())).count());
        levelCount.put("info", allAlerts.stream().filter(a -> "info".equals(a.getAlertLevel())).count());
        statistics.setLevelCount(levelCount);

        // 按状态统计
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("pending", allAlerts.stream().filter(a -> "pending".equals(a.getStatus())).count());
        statusCount.put("processing", allAlerts.stream().filter(a -> "processing".equals(a.getStatus())).count());
        statusCount.put("resolved", allAlerts.stream().filter(a -> "resolved".equals(a.getStatus())).count());
        statusCount.put("ignored", allAlerts.stream().filter(a -> "ignored".equals(a.getStatus())).count());
        statistics.setStatusCount(statusCount);

        // 待处理告警
        statistics.setPendingCount(statusCount.getOrDefault("pending", 0L));

        return statistics;
    }

    /**
     * 查询最近的告警
     *
     * @param limit 条数
     * @return 告警列表
     */
    public List<AlertRecord> getRecentAlerts(int limit) {
        String tenantId = TenantContext.getTenantId();

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getTenantId, Long.valueOf(tenantId))
               .in(AlertRecord::getStatus, "pending", "processing")
               .orderByDesc(AlertRecord::getCreateTime)
               .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    /**
     * 查询设备告警历史
     *
     * @param deviceId 设备ID
     * @param limit    条数
     * @return 告警列表
     */
    public List<AlertRecord> getDeviceAlertHistory(Long deviceId, int limit) {
        // 验证设备权限
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw BusinessException.notFound("设备不存在");
        }

        String tenantId = TenantContext.getTenantId();
        if (!device.getTenantId().toString().equals(tenantId)) {
            throw BusinessException.forbidden("无权访问该设备");
        }

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getDeviceId, deviceId)
               .orderByDesc(AlertRecord::getCreateTime)
               .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    /**
     * 验证状态流转是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "pending" -> List.of("processing", "resolved", "ignored").contains(newStatus);
            case "processing" -> List.of("resolved", "ignored").contains(newStatus);
            case "resolved", "ignored" -> false; // 终态，不能再转换
            default -> true;
        };
    }

    /**
     * 告警统计 VO
     */
    @Data
    public static class AlertStatisticsVO {
        private Long totalCount;
        private Map<String, Long> levelCount;    // critical, warning, info
        private Map<String, Long> statusCount;  // pending, processing, resolved, ignored
        private Long pendingCount;
    }
}
