# Quickstart: IoT 平台功能补全

**Feature**: 004-iot-platform-completion
**Branch**: `004-iot-platform-completion`

---

## 开发环境前提

1. **JDK 21** 已安装
2. **Docker Desktop** 运行中（Testcontainers 需要）
3. **PostgreSQL / Redis / Kafka / EMQX / InfluxDB** 通过 Docker Compose 启动
4. **Nacos** 运行中（服务注册发现）
5. 后端各微服务正常运行

## 新增依赖清单

### gateway-service
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### connect-service
```xml
<dependency>
    <groupId>com.hivemq</groupId>
    <artifactId>hivemq-mqtt-client</artifactId>
    <version>1.3.3</version>
</dependency>
```

### 需要 Feign 调用的服务（device-service, data-service, rule-service）
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### 需要集成测试的服务（test scope）
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

## 新增模块

- `common/common-feign-api` — OpenFeign 接口定义 + DTO + 请求拦截器

## EMQX 配置步骤

1. 访问 EMQX Dashboard: `http://localhost:18083` (admin/public)
2. 配置 HTTP Authenticator: POST → `http://host.docker.internal:8080/api/v1/mqtt/auth`
3. 配置 Rule Engine:
   - 设备消息规则 → HTTP Action → `http://host.docker.internal:8083/api/v1/mqtt/webhook/message`
   - 上下线事件规则 → HTTP Action → `http://host.docker.internal:8083/api/v1/mqtt/webhook/{connected|disconnected}`

## 验证命令

```bash
# 运行单元测试
cd backend && mvn test

# 运行覆盖率报告
cd backend && mvn verify -pl device-service -am

# MQTT 连接测试（使用 MQTTX 或 mosquitto_pub）
mosquitto_pub -h localhost -p 1883 -t "/devices/test-key/properties/report" \
  -u "product-key" -P "device-secret" -m '{"temperature": 25}'
```
