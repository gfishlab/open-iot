package com.openiot.data.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 配置类
 *
 * <p>当 influxdb.enabled=true 时创建 InfluxDBClient bean。
 *
 * @author OpenIoT Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "influxdb", name = "enabled", havingValue = "true")
public class InfluxDBConfig {

    private final InfluxDBProperties properties;

    /**
     * 创建 InfluxDB 客户端
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("初始化 InfluxDB 客户端: url={}, org={}, bucket={}",
                properties.getUrl(), properties.getOrg(), properties.getBucket());

        return InfluxDBClientFactory.create(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getOrg(),
                properties.getBucket()
        );
    }
}
