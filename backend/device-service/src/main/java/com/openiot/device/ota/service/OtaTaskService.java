package com.openiot.device.ota.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.Device;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.ota.entity.FirmwareVersion;
import com.openiot.device.ota.entity.OtaDeviceStatus;
import com.openiot.device.ota.entity.OtaUpgradeTask;
import com.openiot.device.ota.mapper.FirmwareVersionMapper;
import com.openiot.device.ota.mapper.OtaDeviceStatusMapper;
import com.openiot.device.ota.mapper.OtaUpgradeTaskMapper;
import com.openiot.device.ota.vo.OtaTaskCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * OTA 升级任务服务
 * 负责创建升级任务、查询任务设备状态等操作
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaTaskService extends ServiceImpl<OtaUpgradeTaskMapper, OtaUpgradeTask> {

    private final FirmwareVersionMapper firmwareVersionMapper;
    private final OtaDeviceStatusMapper otaDeviceStatusMapper;
    private final DeviceMapper deviceMapper;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Kafka 设备事件主题 */
    private static final String DEVICE_EVENTS_TOPIC = "device-events";

    /**
     * 创建 OTA 升级任务
     * 1. 验证固件版本存在
     * 2. 查询目标设备列表
     * 3. 创建任务记录 + 批量插入设备升级状态记录
     * 4. 为每台设备发送 Kafka OTA_UPGRADE 事件
     *
     * @param vo 任务创建参数
     * @return 创建的升级任务
     */
    @Transactional(rollbackFor = Exception.class)
    public OtaUpgradeTask createTask(OtaTaskCreateVO vo) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw BusinessException.unauthorized("租户信息不存在");
        }
        Long tenantIdLong = Long.valueOf(tenantId);

        // 1. 验证固件版本是否存在且属于当前租户
        FirmwareVersion firmware = firmwareVersionMapper.selectById(vo.getFirmwareVersionId());
        if (firmware == null) {
            throw BusinessException.notFound("固件版本不存在");
        }
        if (!TenantContext.isPlatformAdmin() && !firmware.getTenantId().equals(tenantIdLong)) {
            throw BusinessException.forbidden("无权使用该固件版本");
        }

        // 2. 查询目标设备列表
        List<Device> targetDevices = queryTargetDevices(vo, tenantIdLong);
        if (targetDevices.isEmpty()) {
            throw BusinessException.badRequest("未找到符合条件的目标设备");
        }

        // 3. 构建目标设备ID的JSON数组
        ArrayNode deviceIdArray = objectMapper.createArrayNode();
        for (Device device : targetDevices) {
            deviceIdArray.add(device.getId());
        }

        // 4. 创建升级任务记录
        OtaUpgradeTask task = new OtaUpgradeTask();
        task.setTenantId(tenantIdLong);
        task.setTaskName(vo.getTaskName());
        task.setProductId(vo.getProductId());
        task.setFirmwareVersionId(vo.getFirmwareVersionId());
        task.setUpgradeScope(vo.getUpgradeScope());
        task.setTargetDeviceIds(deviceIdArray);
        task.setTotalCount(targetDevices.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setTaskStatus("created");
        task.setStrategy(vo.getStrategy());
        task.setScheduledTime(vo.getScheduledTime());
        task.setBatchSize(vo.getBatchSize());
        task.setRetryCount(vo.getRetryCount());
        task.setStatus("1");
        task.setDelFlag("0");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        String userId = TenantContext.getUserId();
        if (userId != null) {
            task.setCreateBy(Long.valueOf(userId));
            task.setUpdateBy(Long.valueOf(userId));
        }

        this.save(task);
        log.info("OTA 升级任务创建成功: taskId={}, taskName={}, deviceCount={}", task.getId(), task.getTaskName(), targetDevices.size());

        // 5. 批量插入设备升级状态记录（初始状态为 pending）
        List<OtaDeviceStatus> deviceStatuses = new ArrayList<>();
        for (Device device : targetDevices) {
            OtaDeviceStatus status = new OtaDeviceStatus();
            status.setTenantId(tenantIdLong);
            status.setTaskId(task.getId());
            status.setDeviceId(device.getId());
            status.setFirmwareVersionId(vo.getFirmwareVersionId());
            status.setUpgradeStatus("pending");
            status.setProgress(0);
            status.setTargetVersion(firmware.getVersion());
            status.setRetryCount(0);
            status.setCreateTime(LocalDateTime.now());
            status.setUpdateTime(LocalDateTime.now());
            deviceStatuses.add(status);
        }

        // MyBatis Plus 批量插入
        for (OtaDeviceStatus ds : deviceStatuses) {
            otaDeviceStatusMapper.insert(ds);
        }

        // 6. 为每台设备发送 Kafka OTA_UPGRADE 事件
        for (Device device : targetDevices) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", task.getId());
            payload.put("firmwareId", firmware.getId());
            payload.put("version", firmware.getVersion());
            payload.put("filePath", firmware.getFilePath());
            payload.put("fileMd5", firmware.getFileMd5());
            payload.put("fileSha256", firmware.getFileSha256());
            payload.put("fileSize", firmware.getFileSize());

            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .deviceId(device.getId().toString())
                    .eventType(EventEnvelope.EVENT_TYPE_OTA_UPGRADE)
                    .payload(payload)
                    .timestamp(System.currentTimeMillis())
                    .traceId(UUID.randomUUID().toString().substring(0, 16))
                    .build();

            kafkaTemplate.send(DEVICE_EVENTS_TOPIC, envelope.getKafkaKey(), envelope);
        }

        // 更新任务状态为 running
        task.setTaskStatus("running");
        this.updateById(task);

        log.info("OTA 升级任务已启动: taskId={}, 已发送 {} 条升级指令", task.getId(), targetDevices.size());
        return task;
    }

    /**
     * 分页查询任务下的设备升级状态
     *
     * @param taskId 任务ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    public Page<OtaDeviceStatus> getTaskDeviceStatuses(Long taskId, int page, int size) {
        // 先验证任务权限
        OtaUpgradeTask task = this.getById(taskId);
        if (task == null) {
            throw BusinessException.notFound("升级任务不存在");
        }
        if (!TenantContext.isPlatformAdmin()) {
            String tenantId = TenantContext.getTenantId();
            if (!task.getTenantId().toString().equals(tenantId)) {
                throw BusinessException.forbidden("无权访问该升级任务");
            }
        }

        Page<OtaDeviceStatus> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OtaDeviceStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OtaDeviceStatus::getTaskId, taskId)
               .orderByDesc(OtaDeviceStatus::getUpdateTime);

        return otaDeviceStatusMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 分页查询升级任务列表（租户隔离）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<OtaUpgradeTask> listTasks(int page, int size) {
        Page<OtaUpgradeTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OtaUpgradeTask> wrapper = new LambdaQueryWrapper<>();

        // 租户隔离：平台管理员可查看所有租户任务
        if (!TenantContext.isPlatformAdmin()) {
            String tenantId = TenantContext.getTenantId();
            wrapper.eq(OtaUpgradeTask::getTenantId, Long.valueOf(tenantId));
        }

        wrapper.orderByDesc(OtaUpgradeTask::getCreateTime);

        return this.page(pageParam, wrapper);
    }

    /**
     * 根据升级范围查询目标设备列表
     *
     * @param vo       任务创建参数
     * @param tenantId 租户ID
     * @return 目标设备列表
     */
    private List<Device> queryTargetDevices(OtaTaskCreateVO vo, Long tenantId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getTenantId, tenantId)
               .eq(Device::getProductId, vo.getProductId())
               .eq(Device::getStatus, "1");

        if ("SELECTED".equals(vo.getUpgradeScope())) {
            // 指定设备模式：只查询指定的设备ID列表
            if (vo.getTargetDeviceIds() == null || vo.getTargetDeviceIds().isEmpty()) {
                throw BusinessException.badRequest("指定设备模式下，设备ID列表不能为空");
            }
            wrapper.in(Device::getId, vo.getTargetDeviceIds());
        }

        return deviceMapper.selectList(wrapper);
    }
}
