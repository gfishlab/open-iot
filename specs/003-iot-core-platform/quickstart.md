# Quickstart: IoT Platform Core Functionality

**Date**: 2026-03-06
**Feature**: 003-iot-core-platform

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| JDK | 21 (LTS) | Java 运行环境 |
| Maven | 3.9+ | Java 构建工具 |
| Node.js | 18+ | 前端运行环境 |
| npm | 9+ | 前端包管理器 |
| Docker | 24+ | 容器运行环境 |
| Docker Compose | 2.20+ | 容器编排工具 |
| PostgreSQL | 15+ | 主数据库 |
| Redis | 7+ | 缓存和消息队列 |
| Kafka | 3.6+ | 消息队列 |
| InfluxDB | 2.x | 时序数据库 |

### Recommended Tools

- **IDE**: IntelliJ IDEA 2023.3+ (后端) + VS Code (前端)
- **API Testing**: Postman / Insomnia
- **Database GUI**: DBeaver / pgAdmin
- **Redis GUI**: RedisInsight
- **Kafka GUI**: Kafka Tool / AKHQ

---

## Environment Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/open-iot.git
cd open-iot
git checkout 003-iot-core-platform
```

### 2. Start Infrastructure (Docker Compose)

创建 `infrastructure/docker-compose.dev.yml`（如果不存在）：

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: openiot-postgres
    environment:
      POSTGRES_DB: openiot
      POSTGRES_USER: openiot
      POSTGRES_PASSWORD: openiot123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: openiot-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: openiot-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: openiot-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  influxdb:
    image: influxdb:2.7
    container_name: openiot-influxdb
    ports:
      - "8086:8086"
    volumes:
      - influxdb_data:/var/lib/influxdb2
    environment:
      DOCKER_INFLUXDB_INIT_MODE: setup
      DOCKER_INFLUXDB_INIT_USERNAME: admin
      DOCKER_INFLUXDB_INIT_PASSWORD: admin123
      DOCKER_INFLUXDB_INIT_ORG: openiot
      DOCKER_INFLUXDB_INIT_BUCKET: device-data
      DOCKER_INFLUXDB_INIT_RETENTION: 90d

volumes:
  postgres_data:
  redis_data:
  influxdb_data:
```

启动基础设施：

```bash
cd infrastructure
docker-compose -f docker-compose.dev.yml up -d
```

验证服务状态：

```bash
docker-compose -f docker-compose.dev.yml ps
```

### 3. Initialize Database (Flyway Migration)

```bash
cd backend/tenant-service
mvn flyway:migrate
```

验证迁移结果：

```sql
-- 连接 PostgreSQL
psql -U openiot -d openiot

-- 查询 Flyway 迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 验证新表是否创建
\dt

-- 应该看到：
-- product, parse_rule, mapping_rule, forward_rule, alarm_rule, alarm_record, device_service_invoke, device_property, device_event
```

### 4. Configure Application

#### Backend Configuration

编辑 `backend/tenant-service/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openiot
    username: openiot
    password: openiot123

  kafka:
    bootstrap-servers: localhost:9092

  redis:
    host: localhost
    port: 6379

influxdb:
  url: http://localhost:8086
  token: ${INFLUXDB_TOKEN}  # 从 InfluxDB UI 获取
  org: openiot
  bucket: device-data
  retention-days: 90
```

获取 InfluxDB Token：

```bash
# 1. 访问 InfluxDB UI: http://localhost:8086
# 2. 登录（admin / admin123）
# 3. 进入 API Tokens 页面
# 4. 复制生成的 Token

# 5. 设置环境变量
export INFLUXDB_TOKEN="your-token-here"
```

#### Frontend Configuration

编辑 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

---

## Build and Run

### Backend

#### 1. Build All Services

```bash
cd backend
mvn clean install -DskipTests
```

#### 2. Start Services (Recommended Order)

**Terminal 1: Gateway Service**
```bash
cd backend/gateway-service
mvn spring-boot:run
```

**Terminal 2: Tenant Service**
```bash
cd backend/tenant-service
mvn spring-boot:run
```

**Terminal 3: Device Service**
```bash
cd backend/device-service
mvn spring-boot:run
```

**Terminal 4: Connect Service**
```bash
cd backend/connect-service
mvn spring-boot:run
```

**Terminal 5: Data Service**
```bash
cd backend/data-service
mvn spring-boot:run
```

**Terminal 6: Rule Service (新增)**
```bash
cd backend/rule-service
mvn spring-boot:run
```

#### 3. Verify Services

检查服务健康状态：

```bash
# Gateway
curl http://localhost:8080/actuator/health

# Tenant Service
curl http://localhost:8081/actuator/health

# Device Service
curl http://localhost:8082/actuator/health

# Connect Service
curl http://localhost:8083/actuator/health

# Data Service
curl http://localhost:8084/actuator/health

# Rule Service
curl http://localhost:8085/actuator/health
```

### Frontend

#### 1. Install Dependencies

```bash
cd frontend
npm install
```

#### 2. Start Development Server

```bash
npm run dev
```

访问：http://localhost:5173

---

## Test the Feature

### 1. User Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

保存返回的 Token：

```bash
export TOKEN="your-jwt-token-here"
```

### 2. Create Product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "智能温度传感器",
    "productType": "DEVICE",
    "protocolType": "MQTT",
    "nodeType": "DIRECT",
    "dataFormat": "JSON"
  }'
```

记录返回的 `id` 和 `productKey`：

```bash
export PRODUCT_ID=1
export PRODUCT_KEY="PROD_A1B2C3"
```

### 3. Define Thing Model

```bash
curl -X PUT http://localhost:8080/api/products/$PRODUCT_ID/thing-model \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "properties": [
      {
        "identifier": "temperature",
        "name": "温度",
        "type": "float",
        "unit": "°C",
        "min": -40,
        "max": 80,
        "required": true
      },
      {
        "identifier": "humidity",
        "name": "湿度",
        "type": "float",
        "unit": "%",
        "min": 0,
        "max": 100,
        "required": false
      }
    ],
    "events": [],
    "services": [
      {
        "identifier": "reboot",
        "name": "重启设备",
        "inputParams": [],
        "outputParams": [
          {"identifier": "success", "name": "执行结果", "type": "boolean"}
        ]
      }
    ]
  }'
```

### 4. Create Device

```bash
curl -X POST http://localhost:8080/api/devices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "deviceName": "温度传感器-001"
  }'
```

记录返回的 `deviceKey` 和 `deviceSecret`（仅此一次返回明文）：

```bash
export DEVICE_ID=1
export DEVICE_KEY="550e8400-e29b-41d4-a716-446655440000"
export DEVICE_SECRET="550e8400-e29b-41d4-a716-446655440001"
```

### 5. Create Parse Rule

```bash
curl -X POST http://localhost:8080/api/parse-rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "ruleName": "JSON字段映射",
    "ruleType": "JSON",
    "ruleConfig": {
      "fieldMappings": [
        {"source": "t", "target": "temperature"},
        {"source": "h", "target": "humidity"}
      ]
    },
    "priority": 10
  }'
```

### 6. Test Parse Rule

```bash
curl -X POST http://localhost:8080/api/parse-rules/1/test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rawData": "{\"t\": 25.5, \"h\": 60.2}"
  }'
```

预期输出：

```json
{
  "code": 200,
  "message": "测试成功",
  "data": {
    "parsedData": {
      "temperature": 25.5,
      "humidity": 60.2
    },
    "executionTime": 5
  }
}
```

### 7. Simulate Device Data Upload (MQTT)

使用 MQTT 客户端（如 MQTTX）连接：

**Connection Settings**:
- Broker: `tcp://localhost:1883`
- Username: `$DEVICE_KEY`
- Password: `$DEVICE_SECRET`

**Publish Message**:
- Topic: `device/data`
- Payload:
  ```json
  {
    "t": 26.5,
    "h": 65.0
  }
  ```

### 8. Query Device Properties

```bash
curl -X GET "http://localhost:8080/api/devices/$DEVICE_ID/properties" \
  -H "Authorization: Bearer $TOKEN"
```

预期输出：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "propertyIdentifier": "temperature",
      "propertyValue": "26.5",
      "dataType": "float",
      "updateTime": "2026-03-06T12:00:00"
    },
    {
      "propertyIdentifier": "humidity",
      "propertyValue": "65.0",
      "dataType": "float",
      "updateTime": "2026-03-06T12:00:00"
    }
  ]
}
```

### 9. Create Alarm Rule

```bash
curl -X POST http://localhost:8080/api/alarm-rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "ruleName": "高温告警",
    "alarmLevel": "WARNING",
    "conditionExpression": "temperature > 30",
    "notifyConfig": {
      "channels": ["EMAIL"],
      "email": {
        "recipients": ["admin@example.com"]
      }
    }
  }'
```

### 10. Trigger Alarm

发布高温数据（temperature > 30）：

```json
{
  "t": 35.0,
  "h": 60.0
}
```

查询告警记录：

```bash
curl -X GET "http://localhost:8080/api/alarm-records?status=ACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Troubleshooting

### 1. Flyway Migration Failed

**问题**：Flyway 迁移失败，报错 `Checksum mismatch`

**解决**：
```bash
# 1. 检查 Flyway 历史
SELECT * FROM flyway_schema_history WHERE version = '1.3.0';

# 2. 如果已执行过，删除该记录（仅开发环境）
DELETE FROM flyway_schema_history WHERE version = '1.3.0';

# 3. 重新执行迁移
mvn flyway:migrate
```

### 2. InfluxDB Connection Failed

**问题**：连接 InfluxDB 失败

**解决**：
```bash
# 1. 检查 InfluxDB 是否运行
docker ps | grep influxdb

# 2. 检查 Token 是否正确
echo $INFLUXDB_TOKEN

# 3. 测试连接
curl http://localhost:8086/health
```

### 3. Kafka Consumer Lag

**问题**：Kafka 消费延迟过大

**解决**：
```bash
# 1. 检查消费者组状态
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group device-consumer-group

# 2. 增加消费者实例数或调整 partition 数
```

### 4. Device Authentication Failed

**问题**：设备认证失败

**解决**：
```bash
# 1. 检查 DeviceKey 和 DeviceSecret 是否正确
# 2. 检查设备状态是否为启用
SELECT device_key, device_secret, status FROM device WHERE id = 1;

# 3. 检查 Redis 缓存是否过期
redis-cli
> GET device:auth:550e8400-e29b-41d4-a716-446655440000
```

---

## Next Steps

完成快速开始后，您可以：

1. **阅读详细文档**：
   - `plan.md` - 实施计划
   - `research.md` - 技术研究
   - `data-model.md` - 数据模型设计
   - `contracts/api-contracts.md` - API 契约

2. **开始开发**：
   - 运行 `/speckit.tasks` 生成任务清单
   - 按 Phase 1 → Phase 2 → ... → Phase 8 顺序实施
   - 每个阶段完成后进行端到端验证

3. **探索功能**：
   - 创建更多产品和设备
   - 配置不同的解析规则（JavaScript、Regex、Binary）
   - 设置转发规则（Kafka、HTTP、MQTT、InfluxDB）
   - 配置告警规则和通知

4. **性能测试**：
   - 使用 JMeter 或 Gatling 进行压力测试
   - 监控 Metrics（Prometheus + Grafana）
   - 检查日志（Loki）
   - 追踪链路（Zipkin）

---

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [GraalJS Documentation](https://www.graalvm.org/latest/reference-manual/js/)
- [InfluxDB 2.x Documentation](https://docs.influxdata.com/influxdb/v2/)
- [Aviator Documentation](https://github.com/killme2008/aviator)
- [Vue 3 Documentation](https://vuejs.org/)
- [Element Plus Documentation](https://element-plus.org/)
