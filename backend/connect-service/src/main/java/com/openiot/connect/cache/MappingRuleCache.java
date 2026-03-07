package com.openiot.connect.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射规则缓存
 * 用于 connect-service 缓存产品的映射规则配置
 *
 * <p>缓存更新机制：
 * <ol>
 *   <li>通过 Redis Pub/Sub 接收 rule-service 的规则更新通知</li>
 *   <li>收到更新通知后，清除对应的缓存</li>
 *   <li>下次使用时重新从 Redis 加载</li>
 * </ol>
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingRuleCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 本地缓存：productId -> fieldMappings
     * 用于减少 Redis 访问频率
     */
    private final ConcurrentHashMap<Long, String> localCache = new ConcurrentHashMap<>();

    /**
     * Redis 缓存 Key 前缀
     */
    private static final String CACHE_KEY_PREFIX = "openiot:mapping_rule:product:";

    /**
     * 获取产品的映射规则配置
     *
     * @param productId 产品ID
     * @return 映射配置（JSON格式），如果不存在返回 null
     */
    public String getMappingRule(Long productId) {
        // 1. 先查本地缓存
        String cached = localCache.get(productId);
        if (cached != null) {
            log.debug("从本地缓存获取映射规则: productId={}", productId);
            return cached;
        }

        // 2. 查 Redis 缓存
        String redisKey = CACHE_KEY_PREFIX + productId;
        Object value = redisTemplate.opsForValue().get(redisKey);

        if (value != null) {
            String fieldMappings = value.toString();
            // 更新本地缓存
            localCache.put(productId, fieldMappings);
            log.debug("从 Redis 缓存获取映射规则: productId={}", productId);
            return fieldMappings;
        }

        log.debug("映射规则缓存未命中: productId={}", productId);
        return null;
    }

    /**
     * 缓存产品的映射规则配置
     *
     * @param productId     产品ID
     * @param fieldMappings 映射配置（JSON格式）
     */
    public void putMappingRule(Long productId, String fieldMappings) {
        // 更新本地缓存
        localCache.put(productId, fieldMappings);

        // 更新 Redis 缓存
        String redisKey = CACHE_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(redisKey, fieldMappings);

        log.debug("缓存映射规则: productId={}", productId);
    }

    /**
     * 清除产品的映射规则缓存
     *
     * @param productId 产品ID
     */
    public void evict(Long productId) {
        // 清除本地缓存
        localCache.remove(productId);

        // 清除 Redis 缓存
        String redisKey = CACHE_KEY_PREFIX + productId;
        redisTemplate.delete(redisKey);

        log.info("清除映射规则缓存: productId={}", productId);
    }

    /**
     * 清除所有缓存
     */
    public void evictAll() {
        // 清除本地缓存
        localCache.clear();

        // 清除 Redis 缓存（使用 pattern 删除）
        var keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        log.info("清除所有映射规则缓存");
    }

    /**
     * 处理规则更新消息
     *
     * @param message Redis Pub/Sub 消息
     */
    public void handleRuleUpdate(String message) {
        try {
            var node = objectMapper.readTree(message);
            String action = node.path("action").asText();
            Long productId = node.path("productId").asLong();

            if ("update".equals(action) || "delete".equals(action)) {
                evict(productId);
            }

        } catch (JsonProcessingException e) {
            log.error("解析映射规则更新消息失败: {}", message, e);
        }
    }
}
