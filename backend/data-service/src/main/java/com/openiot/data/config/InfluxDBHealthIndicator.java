package com.openiot.data.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.HealthCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * InfluxDB 健康检查指示器
 *
 * <p>检查 InfluxDB 时序数据库的连接状态和健康情况。
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@Component("influxDBHealthIndicator")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "influxdb.enabled", havingValue = "true", matchIfMissing = false)
public class InfluxDBHealthIndicator implements HealthIndicator {

    private final InfluxDBClient influxDBClient;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // 检查连接状态 - 使用 health() 方法
            checkHealth(builder);

            // 检查存储桶状态
            checkBucket(builder);

            log.debug("InfluxDB 健康检查通过");

        } catch (Exception e) {
            log.warn("InfluxDB 健康检查失败: {}", e.getMessage());
            builder.down()
                   .withDetail("error", e.getMessage())
                   .withDetail("error_type", e.getClass().getSimpleName());
        }

        return builder.build();
    }

    /**
     * 检查 InfluxDB 健康状态
     */
    private void checkHealth(Health.Builder builder) {
        try {
            // 使用 health() 方法检查健康状态
            HealthCheck health = influxDBClient.health();

            String status = health.getStatus() != null ? health.getStatus().name() : "UNKNOWN";
            builder.withDetail("status", status)
                   .withDetail("message", health.getMessage() != null ? health.getMessage() : "N/A");

            // 检查健康状态
            if (health.getStatus() != null) {
                // HealthCheck.Status 枚举值：PASS, FAIL
                if (health.getStatus() == HealthCheck.StatusEnum.FAIL) {
                    builder.down().withDetail("health_status", "UNHEALTHY");
                }
            }
        } catch (Exception e) {
            log.debug("获取 InfluxDB 健康状态失败: {}", e.getMessage());
            builder.down().withDetail("connection", "DOWN - " + e.getMessage());
        }
    }

    /**
     * 检查存储桶状态
     */
    private void checkBucket(Health.Builder builder) {
        try {
            var bucketsApi = influxDBClient.getBucketsApi();

            // 尝试列出存储桶，验证权限
            var buckets = bucketsApi.findBuckets();
            int bucketCount = buckets.size();

            builder.withDetail("buckets_count", bucketCount);

            // 检查是否有可用的存储桶
            if (bucketCount == 0) {
                builder.withDetail("bucket_warning", "No buckets found");
            }

        } catch (Exception e) {
            log.debug("检查 InfluxDB 存储桶失败: {}", e.getMessage());
            builder.withDetail("bucket_check", "FAILED - " + e.getMessage());
        }
    }
}
