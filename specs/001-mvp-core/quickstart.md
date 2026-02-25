# Quickstart: Open-IoT MVP

**Feature**: 001-mvp-core
**Created**: 2026-02-25

本文档描述如何快速启动和验证 MVP 阶段的核心功能。

---

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | LTS 版本 |
| Maven | 3.9+ | 构建工具 |
| Node.js | 18+ | 前端构建 |
| Docker | 24+ | 容器运行时 |
| Docker Compose | 2.20+ | 容器编排 |

---

## 快速启动

### 1. 克隆项目

```bash
git clone https://github.com/gxj1134506645/open-iot.git
cd open-iot
```

### 2. 启动基础设施

```bash
# 启动 Nacos, PostgreSQL, Redis, MongoDB, Kafka, EMQX
docker-compose -f infrastructure/docker/docker-compose.yml up -d
```

等待所有服务启动完成（约 2-3 分钟）。

### 3. 验证基础设施

```bash
# 检查服务状态
docker-compose -f infrastructure/docker/docker-compose.yml ps

# 验证 Nacos
curl http://localhost:8848/nacos/v1/console/health/readiness

# 验证 PostgreSQL
docker exec -it open-iot-postgres psql -U openiot -d openiot -c "SELECT 1"

# 验证 Redis
docker exec -it open-iot-redis redis-cli ping

# 验证 MongoDB
docker exec -it open-iot-mongo mongosh --eval "db.runCommand({ping:1})"

# 验证 Kafka
docker exec -it open-iot-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 4. 启动后端服务

```bash
# 构建所有服务
cd backend
mvn clean package -DskipTests

# 启动 Gateway（端口 8080）
java -jar gateway-service/target/gateway-service.jar

# 启动 Device Service（端口 8081）
java -jar device-service/target/device-service.jar

# 启动 Connect Service（端口 8082）
java -jar connect-service/target/connect-service.jar

# 启动 Data Service（端口 8083）
java -jar data-service/target/data-service.jar

# 启动 Tenant Service（端口 8084）
java -jar tenant-service/target/tenant-service.jar
```

### 5. 验证后端服务

```bash
# 检查 Nacos 注册
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service

# 健康检查
curl http://localhost:8080/actuator/health
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问地址: http://localhost:5173

---

## 功能验证

### 验证 1: 用户登录

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应示例
# {"code":200,"msg":"success","data":{"token":"xxx","userId":1,"username":"admin","role":"ADMIN"}}
```

### 验证 2: 创建租户

```bash
# 使用上一步获取的 Token
TOKEN="xxx"

curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"tenantCode":"tenant-001","tenantName":"测试租户","contactEmail":"admin@tenant.com"}'

# 响应示例
# {"code":200,"msg":"success","data":{"id":1,"tenantCode":"tenant-001",...}}
```

### 验证 3: 创建设备

```bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"deviceCode":"device-001","deviceName":"测试设备","protocolType":"MQTT"}'

# 响应示例（注意保存 deviceToken）
# {"code":200,"msg":"success","data":{"id":1,"deviceCode":"device-001","deviceToken":"abc123xxx",...}}
```

### 验证 4: MQTT 设备上报

使用 MQTT 客户端（如 MQTTX）连接 EMQX：

```yaml
# 连接配置
Broker: localhost
Port: 1883
Client ID: device-001
Username: device-001
Password: <deviceToken>

# 上报消息
Topic: device/telemetry
Payload:
  {
    "latitude": 30.1234567,
    "longitude": 120.1234567,
    "speed": 25.5,
    "heading": 180.0
  }
```

### 验证 5: 查看实时轨迹

```bash
# 订阅 SSE 流
curl -N http://localhost:8080/api/v1/devices/1/trajectory/stream \
  -H "Authorization: Bearer $TOKEN"

# 或在前端设备监控页面查看
```

### 验证 6: 验证多租户隔离

```bash
# 创建第二个租户用户
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant1-admin","password":"xxx"}'

# 使用租户 Token 查询设备列表，应只返回本租户设备
curl http://localhost:8080/api/v1/devices \
  -H "Authorization: Bearer $TENANT_TOKEN"

# 尝试访问其他租户设备，应返回 403
curl http://localhost:8080/api/v1/devices/999 \
  -H "Authorization: Bearer $TENANT_TOKEN"
```

---

## 常见问题

### Q1: Nacos 启动失败

检查端口 8848 是否被占用：
```bash
netstat -tlnp | grep 8848
```

### Q2: Kafka 连接超时

确保 Kafka 和 Zookeeper 都已启动：
```bash
docker-compose -f infrastructure/docker/docker-compose.yml logs kafka
```

### Q3: 前端无法连接后端

检查网关是否正常运行，并验证代理配置：
```bash
curl http://localhost:8080/actuator/health
```

### Q4: MQTT 连接被拒绝

检查 EMQX 日志：
```bash
docker-compose -f infrastructure/docker/docker-compose.yml logs emqx
```

---

## 端口列表

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API 网关 |
| Device Service | 8081 | 设备管理 |
| Connect Service | 8082 | 设备接入 |
| Data Service | 8083 | 数据处理 |
| Tenant Service | 8084 | 租户管理 |
| Nacos | 8848 | 注册/配置中心 |
| PostgreSQL | 5432 | 主数据库 |
| Redis | 6379 | 缓存 |
| MongoDB | 27017 | 文档库 |
| Kafka | 9092 | 消息队列 |
| EMQX | 1883 | MQTT Broker |
| Frontend | 5173 | 前端开发服务器 |

---

## 下一步

1. 查看 [plan.md](./plan.md) 了解详细架构设计
2. 查看 [data-model.md](./data-model.md) 了解数据结构
3. 查看 [contracts/api.yaml](./contracts/api.yaml) 了解 API 契约
4. 执行 `/speckit.tasks` 生成任务清单
