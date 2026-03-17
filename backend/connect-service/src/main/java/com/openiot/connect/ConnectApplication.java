package com.openiot.connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 设备接入服务启动类
 *
 * 职责：
 * 1. 维护设备长连接（Netty TCP Server）
 * 2. 解析私有协议（MQTT/CoAP/自定义二进制）
 * 3. 验证设备 Token（从 Redis 快速验证，不查数据库）
 * 4. 转发数据到 Kafka（由 data-service 消费并存储）
 *
 * 不访问数据库，数据持久化由 data-service 负责
 * 设备 Token 由 device-service 在设备注册时写入 Redis
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.openiot.common.feign.client")
public class ConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectApplication.class, args);
    }
}
