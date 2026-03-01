package com.openiot.data.config;

import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据服务健康检查
 * 检查 PostgreSQL、Redis、MongoDB 连接状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataServiceHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MongoTemplate mongoTemplate;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // 检查 PostgreSQL
            checkPostgreSQL(builder);
        } catch (Exception e) {
            builder.withDetail("postgresql", "DOWN: " + e.getMessage());
            log.warn("PostgreSQL 健康检查失败", e);
        }

        try {
            // 检查 Redis
            checkRedis(builder);
        } catch (Exception e) {
            builder.withDetail("redis", "DOWN: " + e.getMessage());
            log.warn("Redis 健康检查失败", e);
        }

        try {
            // 检查 MongoDB
            checkMongoDB(builder);
        } catch (Exception e) {
            builder.withDetail("mongodb", "DOWN: " + e.getMessage());
            log.warn("MongoDB 健康检查失败", e);
        }

        return builder.build();
    }

    private void checkPostgreSQL(Health.Builder builder) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result != null && result == 1) {
            builder.withDetail("postgresql", "UP");
        } else {
            builder.withDetail("postgresql", "DOWN");
        }
    }

    private void checkRedis(Health.Builder builder) {
        String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping()
                .toString();
        builder.withDetail("redis", "UP").withDetail("redis_ping", pong);
    }

    private void checkMongoDB(Health.Builder builder) {
        MongoDatabase db = mongoTemplate.getDb();
        String dbName = db.getName();
        Document result = db.runCommand(new Document("ping", 1));
        boolean isHealthy = result.containsKey("ok") && result.getInteger("ok") == 1;
        builder.withDetail("mongodb", isHealthy ? "UP" : "DOWN")
                .withDetail("mongodb_database", dbName);
    }
}
