package com.openiot.device.ota.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.ota.entity.OtaDeviceStatus;
import com.openiot.device.ota.entity.OtaUpgradeTask;
import com.openiot.device.ota.mapper.OtaDeviceStatusMapper;
import com.openiot.device.ota.mapper.OtaUpgradeTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OTA 升级进度服务
 * 负责处理设备上报的升级进度，维护状态机流转，更新任务统计计数
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaProgressService {

    private final OtaDeviceStatusMapper otaDeviceStatusMapper;
    private final OtaUpgradeTaskMapper otaUpgradeTaskMapper;

    /**
     * 状态机定义：每个状态允许流转的下一个状态集合
     * pending → pushing → downloading → installing → success/failed
     * 任意非终态都可以直接流转到 failed
     */
    private static final Map<String, Set<String>> STATE_TRANSITIONS = Map.of(
            "pending", Set.of("pushing", "failed"),
            "pushing", Set.of("downloading", "failed"),
            "downloading", Set.of("installing", "failed"),
            "installing", Set.of("success", "failed")
    );

    /**
     * 更新设备升级进度
     * 校验状态机合法性，更新设备状态记录，同步更新任务统计计数
     *
     * @param deviceId  设备ID
     * @param taskId    任务ID
     * @param status    新状态（pending/pushing/downloading/installing/success/failed）
     * @param progress  进度百分比 0-100
     * @param errorCode 错误码（失败时传入）
     * @param errorMsg  错误详情（失败时传入）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long deviceId, Long taskId, String status, Integer progress,
                               String errorCode, String errorMsg) {
        // 1. 查询当前设备升级状态记录
        LambdaQueryWrapper<OtaDeviceStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OtaDeviceStatus::getDeviceId, deviceId)
                    .eq(OtaDeviceStatus::getTaskId, taskId);
        OtaDeviceStatus deviceStatus = otaDeviceStatusMapper.selectOne(queryWrapper);

        if (deviceStatus == null) {
            throw BusinessException.notFound("设备升级状态记录不存在: deviceId=" + deviceId + ", taskId=" + taskId);
        }

        // 2. 校验状态机流转是否合法
        String currentStatus = deviceStatus.getUpgradeStatus();
        if (!isValidTransition(currentStatus, status)) {
            throw BusinessException.badRequest(
                    "无效的状态流转: " + currentStatus + " -> " + status);
        }

        // 3. 更新设备升级状态记录
        LambdaUpdateWrapper<OtaDeviceStatus> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OtaDeviceStatus::getId, deviceStatus.getId())
                     .set(OtaDeviceStatus::getUpgradeStatus, status)
                     .set(OtaDeviceStatus::getUpdateTime, LocalDateTime.now());

        // 更新进度百分比
        if (progress != null) {
            updateWrapper.set(OtaDeviceStatus::getProgress, progress);
        }

        // 首次从 pending 流转时，记录开始时间
        if ("pending".equals(currentStatus)) {
            updateWrapper.set(OtaDeviceStatus::getStartTime, LocalDateTime.now());
        }

        // 终态处理：success 或 failed
        if ("success".equals(status) || "failed".equals(status)) {
            updateWrapper.set(OtaDeviceStatus::getFinishTime, LocalDateTime.now());

            // success 时进度设为 100
            if ("success".equals(status)) {
                updateWrapper.set(OtaDeviceStatus::getProgress, 100);
            }

            // failed 时记录错误信息
            if ("failed".equals(status)) {
                if (errorCode != null) {
                    updateWrapper.set(OtaDeviceStatus::getErrorCode, errorCode);
                }
                if (errorMsg != null) {
                    updateWrapper.set(OtaDeviceStatus::getErrorMessage, errorMsg);
                }
            }
        }

        otaDeviceStatusMapper.update(null, updateWrapper);
        log.info("设备升级进度更新: deviceId={}, taskId={}, {} -> {}, progress={}",
                deviceId, taskId, currentStatus, status, progress);

        // 4. 更新任务统计计数
        if ("success".equals(status)) {
            otaDeviceStatusMapper.incrementSuccessCount(taskId);
            log.info("任务成功计数+1: taskId={}", taskId);
            // 检查任务是否全部完成
            checkTaskCompletion(taskId);
        } else if ("failed".equals(status)) {
            otaDeviceStatusMapper.incrementFailedCount(taskId);
            log.info("任务失败计数+1: taskId={}", taskId);
            // 检查任务是否全部完成
            checkTaskCompletion(taskId);
        }
    }

    /**
     * 检查任务是否已全部完成（所有设备都到达终态）
     * 如果 success_count + failed_count == total_count，则标记任务为 completed
     *
     * @param taskId 任务ID
     */
    private void checkTaskCompletion(Long taskId) {
        OtaUpgradeTask task = otaUpgradeTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        int finishedCount = task.getSuccessCount() + task.getFailedCount();
        if (finishedCount >= task.getTotalCount()) {
            // 所有设备已完成（成功或失败），标记任务为 completed
            LambdaUpdateWrapper<OtaUpgradeTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(OtaUpgradeTask::getId, taskId)
                   .set(OtaUpgradeTask::getTaskStatus, "completed")
                   .set(OtaUpgradeTask::getUpdateTime, LocalDateTime.now());
            otaUpgradeTaskMapper.update(null, wrapper);
            log.info("OTA 升级任务已完成: taskId={}, successCount={}, failedCount={}",
                    taskId, task.getSuccessCount(), task.getFailedCount());
        }
    }

    /**
     * 校验状态流转是否合法
     * 状态机：pending → pushing → downloading → installing → success/failed
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 是否允许流转
     */
    private boolean isValidTransition(String from, String to) {
        // 终态不允许再流转
        if ("success".equals(from) || "failed".equals(from)) {
            return false;
        }

        Set<String> allowedTargets = STATE_TRANSITIONS.get(from);
        if (allowedTargets == null) {
            return false;
        }

        return allowedTargets.contains(to);
    }
}
