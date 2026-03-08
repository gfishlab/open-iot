package com.openiot.device.influxdb;

/**
 * InfluxDB 配置属性类
 *
 * @param orgName  组织名称
 * @param bucket 存储桶名称
 * @author OpenIoT Team
 */
public record InfluxDBProperties(String orgName, String bucket) {
}
