package com.openiot.device.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 配置类
 *
 * @author OpenIoT Team
 */
@Configuration
@ConditionalOnProperty(prefix = "influxdb", name = "enabled", havingValue = "true", matchIfMissing = false)
public class InfluxDBConfig {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBConfig.class);

    @Value("${influxdb.url:http://localhost:8086}")
    private String url;

    @Value("${influxdb.token:}")
    private String token;

    @Value("${influxdb.org:openiot}")
    private String org;

    @Value("${influxdb.bucket:device-data}")
    private String bucket;

    /**
     * 创建 InfluxDB 客户端
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("初始化 InfluxDB 客户端: url={}, org={}, bucket={}", url, org, bucket);

        InfluxDBClient client = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);

        // 启用 gzip 压缩
        client.enableGzip();

        log.info("InfluxDB 客户端初始化完成");
        return client;
    }

    /**
     * 创建 WriteApi（批量写入）
     */
    @Bean
    public WriteApi influxDBWriteApi(InfluxDBClient client) {
        return client.makeWriteApi();
    }

    /**
     * InfluxDB 属性配置
     */
    @Bean
    public InfluxDBProperties influxDBProperties() {
        return new InfluxDBProperties(org, bucket);
    }
}
