package com.openiot.common.observability.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * HikariCP 指标配置
 *
 * <p>配置 HikariCP 连接池指标的自动采集。
 * 仅当 DataSource bean 存在时才启用。
 *
 * <h3>关键指标：</h3>
 * <ul>
 *   <li>hikaricp.connections.active - 活跃连接数</li>
 *   <li>hikaricp.connections.idle - 空闲连接数</li>
 *   <li>hikaricp.connections.pending - 等待获取连接的线程数</li>
 *   <li>hikaricp.connections.max - 最大连接数</li>
 *   <li>hikaricp.connections.timeout.total - 连接获取超时次数</li>
 *   <li>hikaricp.connections.creation.seconds - 连接创建时间</li>
 *   <li>hikaricp.connections.usage.seconds - 连接使用时间</li>
 * </ul>
 *
 * @author OpenIoT Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({HikariDataSource.class, MeterRegistry.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "openiot.observability.metrics", name = "hikari-enabled", havingValue = "true", matchIfMissing = true)
public class HikariMetricsConfig {

    /**
     * 配置 HikariCP 指标绑定
     *
     * <p>HikariCP 原生支持 Micrometer，只需设置 poolName 即可。
     * 此配置类主要用于添加额外的自定义指标。
     */
    @Bean
    public HikariMetricsBinder hikariMetricsBinder(DataSource dataSource, MeterRegistry meterRegistry) {
        log.info("HikariCP metrics binding enabled");
        return new HikariMetricsBinder(dataSource, meterRegistry);
    }

    /**
     * HikariCP 指标绑定器
     */
    public static class HikariMetricsBinder {

        private final DataSource dataSource;
        private final MeterRegistry meterRegistry;

        public HikariMetricsBinder(DataSource dataSource, MeterRegistry meterRegistry) {
            this.dataSource = dataSource;
            this.meterRegistry = meterRegistry;

            if (dataSource instanceof HikariDataSource hikariDataSource) {
                // HikariCP 会自动绑定到 MeterRegistry
                // 这里可以添加额外的自定义指标
                log.info("HikariCP metrics bound for pool: {}", hikariDataSource.getPoolName());
            }
        }
    }
}
