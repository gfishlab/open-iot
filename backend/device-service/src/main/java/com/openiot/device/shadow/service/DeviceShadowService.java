package com.openiot.device.shadow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.kafka.model.EventEnvelope;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.shadow.entity.DeviceShadow;
import com.openiot.device.shadow.mapper.DeviceShadowMapper;
import com.openiot.device.shadow.vo.DesiredUpdateVO;
import com.openiot.device.shadow.vo.DeviceShadowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 设备影子服务
 * <p>
 * 管理设备影子的 reported（设备上报）和 desired（期望）属性，
 * 支持 Redis 缓存加速读取，Kafka 推送 delta 变更事件。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceShadowService {

    private final DeviceShadowMapper deviceShadowMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Redis 缓存 Key 前缀 */
    private static final String SHADOW_CACHE_PREFIX = "device:shadow:";

    /** 缓存过期时间：1 小时 */
    private static final long SHADOW_CACHE_TTL = 1;

    /** Kafka 主题：设备事件 */
    private static final String TOPIC_DEVICE_EVENTS = "device-events";

    /**
     * 获取设备影子
     * <p>
     * 优先从 Redis 缓存读取，缓存未命中时查询 PostgreSQL，
     * 若数据库中也不存在则自动初始化一条默认影子记录。
     * </p>
     *
     * @param deviceId 设备ID
     * @return 设备影子 VO
     */
    public DeviceShadowVO getShadow(Long deviceId) {
        // 1. 尝试从 Redis 缓存读取
        String cacheKey = SHADOW_CACHE_PREFIX + deviceId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从 Redis 缓存获取设备影子: deviceId={}", deviceId);
            return convertToVO(cached);
        }

        // 2. 缓存未命中，查询 PostgreSQL
        DeviceShadow shadow = queryShadowByDeviceId(deviceId);
        if (shadow == null) {
            // 数据库中不存在，初始化默认影子记录
            shadow = initShadow(deviceId);
        }

        // 3. 转换为 VO 并写入缓存
        DeviceShadowVO vo = toVO(shadow);
        redisTemplate.opsForValue().set(cacheKey, vo, SHADOW_CACHE_TTL, TimeUnit.HOURS);
        log.debug("从数据库加载设备影子并缓存: deviceId={}", deviceId);

        return vo;
    }

    /**
     * 更新期望属性（desired）
     * <p>
     * 使用乐观锁防止并发冲突，更新后刷新 Redis 缓存，
     * 若 delta 非空则发送 Kafka SHADOW_DELTA 事件通知设备。
     * </p>
     *
     * @param deviceId 设备ID
     * @param vo       期望属性更新请求
     * @return 更新后的设备影子 VO
     */
    public DeviceShadowVO updateDesired(Long deviceId, DesiredUpdateVO vo) {
        // 1. 获取当前影子
        DeviceShadow shadow = queryShadowByDeviceId(deviceId);
        if (shadow == null) {
            shadow = initShadow(deviceId);
        }

        // 2. 计算 delta = diff(desired, reported)
        String deltaJson = computeDelta(vo.getDesired(), shadow.getReported());

        // 3. 乐观锁更新 desired 和 delta
        int rows = deviceShadowMapper.updateDesiredWithVersion(
                deviceId, vo.getDesired(), deltaJson, vo.getVersion());
        if (rows == 0) {
            // 版本冲突，返回 409
            throw new BusinessException(409, "设备影子版本冲突，请刷新后重试");
        }

        // 4. 查询更新后的记录
        DeviceShadow updated = queryShadowByDeviceId(deviceId);
        DeviceShadowVO result = toVO(updated);

        // 5. 刷新 Redis 缓存
        String cacheKey = SHADOW_CACHE_PREFIX + deviceId;
        redisTemplate.opsForValue().set(cacheKey, result, SHADOW_CACHE_TTL, TimeUnit.HOURS);

        // 6. 如果 delta 非空，发送 Kafka SHADOW_DELTA 事件
        if (deltaJson != null && !"{}".equals(deltaJson)) {
            sendShadowDeltaEvent(deviceId, deltaJson);
        }

        log.info("更新设备期望属性: deviceId={}, version={}", deviceId, vo.getVersion());
        return result;
    }

    /**
     * 更新上报属性（reported）
     * <p>
     * 通常由设备数据上报链路调用，使用乐观锁更新 reported，
     * 同时重新计算 delta 并刷新 Redis 缓存。
     * </p>
     *
     * @param deviceId     设备ID
     * @param reportedJson 新的 reported JSON 字符串
     */
    public void updateReported(Long deviceId, String reportedJson) {
        // 1. 获取当前影子
        DeviceShadow shadow = queryShadowByDeviceId(deviceId);
        if (shadow == null) {
            shadow = initShadow(deviceId);
        }

        // 2. 计算 delta = diff(desired, newReported)
        String deltaJson = computeDelta(shadow.getDesired(), reportedJson);

        // 3. 乐观锁更新 reported 和 delta
        int rows = deviceShadowMapper.updateReportedWithVersion(
                deviceId, reportedJson, deltaJson, shadow.getVersion());
        if (rows == 0) {
            // 版本冲突，记录日志但不抛异常（设备上报可重试）
            log.warn("更新设备上报属性版本冲突，稍后重试: deviceId={}, version={}", deviceId, shadow.getVersion());
            return;
        }

        // 4. 查询更新后的记录并刷新 Redis 缓存
        DeviceShadow updated = queryShadowByDeviceId(deviceId);
        DeviceShadowVO result = toVO(updated);
        String cacheKey = SHADOW_CACHE_PREFIX + deviceId;
        redisTemplate.opsForValue().set(cacheKey, result, SHADOW_CACHE_TTL, TimeUnit.HOURS);

        log.info("更新设备上报属性: deviceId={}", deviceId);
    }

    /**
     * 计算 delta（desired 与 reported 的差异）
     * <p>
     * 遍历 desired 中的所有字段，如果 reported 中不存在或值不同，
     * 则将该字段加入 delta。如果 desired 为空，delta 也为空。
     * </p>
     *
     * @param desired  期望属性 JSON 字符串
     * @param reported 上报属性 JSON 字符串
     * @return delta JSON 字符串
     */
    private String computeDelta(String desired, String reported) {
        try {
            // 如果 desired 为空或为空对象，delta 为空
            if (desired == null || desired.isBlank() || "{}".equals(desired.trim())) {
                return "{}";
            }

            JsonNode desiredNode = objectMapper.readTree(desired);
            JsonNode reportedNode = (reported != null && !reported.isBlank())
                    ? objectMapper.readTree(reported)
                    : objectMapper.createObjectNode();

            ObjectNode deltaNode = objectMapper.createObjectNode();

            // 遍历 desired 的所有字段，与 reported 比较
            Iterator<String> fieldNames = desiredNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode desiredValue = desiredNode.get(field);
                JsonNode reportedValue = reportedNode.get(field);

                // reported 中不存在该字段，或值不同，则加入 delta
                if (reportedValue == null || !desiredValue.equals(reportedValue)) {
                    deltaNode.set(field, desiredValue);
                }
            }

            return objectMapper.writeValueAsString(deltaNode);
        } catch (JsonProcessingException e) {
            log.error("计算 delta 失败: desired={}, reported={}", desired, reported, e);
            return "{}";
        }
    }

    /**
     * 初始化设备影子
     * <p>
     * 当设备首次获取影子时，自动创建一条默认记录。
     * </p>
     *
     * @param deviceId 设备ID
     * @return 新创建的设备影子
     */
    private DeviceShadow initShadow(Long deviceId) {
        String tenantId = TenantContext.getTenantId();

        DeviceShadow shadow = new DeviceShadow();
        shadow.setDeviceId(deviceId);
        shadow.setTenantId(tenantId != null ? Long.valueOf(tenantId) : null);
        shadow.setReported("{}");
        shadow.setDesired("{}");
        shadow.setDelta("{}");
        shadow.setVersion(0L);
        shadow.setStatus("1");
        shadow.setDelFlag("0");
        shadow.setReportedTime(LocalDateTime.now());
        shadow.setDesiredTime(LocalDateTime.now());
        shadow.setMetadata("{}");
        shadow.setCreateTime(LocalDateTime.now());
        shadow.setUpdateTime(LocalDateTime.now());

        // 设置创建人
        String userId = TenantContext.getUserId();
        if (userId != null) {
            shadow.setCreateBy(Long.valueOf(userId));
            shadow.setUpdateBy(Long.valueOf(userId));
        }

        deviceShadowMapper.insert(shadow);
        log.info("初始化设备影子: deviceId={}, tenantId={}", deviceId, tenantId);

        return shadow;
    }

    /**
     * 根据设备ID查询影子记录
     *
     * @param deviceId 设备ID
     * @return 设备影子实体，不存在返回 null
     */
    private DeviceShadow queryShadowByDeviceId(Long deviceId) {
        LambdaQueryWrapper<DeviceShadow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceShadow::getDeviceId, deviceId)
               .eq(DeviceShadow::getDelFlag, "0");

        // 租户隔离：非平台管理员只能查询本租户的影子
        if (!TenantContext.isPlatformAdmin()) {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                wrapper.eq(DeviceShadow::getTenantId, Long.valueOf(tenantId));
            }
        }

        return deviceShadowMapper.selectOne(wrapper);
    }

    /**
     * 发送 Kafka SHADOW_DELTA 事件
     * <p>
     * 当 desired 与 reported 存在差异时，通知设备进行属性同步。
     * </p>
     *
     * @param deviceId  设备ID
     * @param deltaJson delta JSON 字符串
     */
    private void sendShadowDeltaEvent(Long deviceId, String deltaJson) {
        try {
            String tenantId = TenantContext.getTenantId();
            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .deviceId(String.valueOf(deviceId))
                    .eventType(EventEnvelope.EVENT_TYPE_SHADOW_DELTA)
                    .payload(deltaJson)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(TOPIC_DEVICE_EVENTS, envelope.getKafkaKey(), envelope);
            log.info("发送 SHADOW_DELTA 事件: deviceId={}, delta={}", deviceId, deltaJson);
        } catch (Exception e) {
            log.error("发送 SHADOW_DELTA 事件失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 实体转 VO
     *
     * @param shadow 设备影子实体
     * @return 设备影子 VO
     */
    private DeviceShadowVO toVO(DeviceShadow shadow) {
        DeviceShadowVO vo = new DeviceShadowVO();
        vo.setDeviceId(shadow.getDeviceId());
        vo.setReported(shadow.getReported());
        vo.setDesired(shadow.getDesired());
        vo.setDelta(shadow.getDelta());
        vo.setVersion(shadow.getVersion());
        vo.setReportedTime(shadow.getReportedTime());
        vo.setDesiredTime(shadow.getDesiredTime());
        vo.setMetadata(shadow.getMetadata());
        return vo;
    }

    /**
     * 将缓存中的对象转换为 DeviceShadowVO
     * <p>
     * Redis 反序列化后可能是 LinkedHashMap，需要通过 ObjectMapper 转换。
     * </p>
     *
     * @param cached 缓存对象
     * @return 设备影子 VO
     */
    private DeviceShadowVO convertToVO(Object cached) {
        if (cached instanceof DeviceShadowVO) {
            return (DeviceShadowVO) cached;
        }
        // Redis Jackson 反序列化可能返回 LinkedHashMap，需要转换
        return objectMapper.convertValue(cached, DeviceShadowVO.class);
    }
}
