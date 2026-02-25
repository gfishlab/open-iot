# Research: Open-IoT MVP 核心功能

**Feature**: 001-mvp-core
**Created**: 2026-02-25
**Purpose**: 解决技术选型和实现方案中的不确定项

---

## 研究结论

### 1. EMQX 与 Kafka 集成方案

**Decision**: 使用 EMQX Rule Engine 直接将 MQTT 消息转发到 Kafka

**Rationale**:
- EMQX 5.x 内置 Kafka 集成，无需额外中间件
- Rule Engine 支持消息格式转换，可直接生成 EventEnvelope
- 相比 Webhook 方案，减少网络跳数，降低延迟

**Alternatives Considered**:
- EMQX Webhook + HTTP 接收服务：增加组件复杂度，延迟更高
- 自建 MQTT Bridge：开发成本高，EMQX 已提供成熟方案

**Implementation Notes**:
```sql
-- EMQX Rule SQL 示例
SELECT
  payload,
  clientid as deviceId,
  topic,
  timestamp
FROM "device/+/telemetry"
```

---

### 2. 简化 Kafka 消费者架构

**Decision**: 采用 Spring Kafka + 单消费者组模式

**Rationale**:
- MVP 阶段数据量可控，无需复杂流处理
- Spring Kafka 提供完善的重试和错误处理机制
- 代码简洁，学习成本低

**Alternatives Considered**:
- Kafka Streams：功能强大但 MVP 阶段过度设计
- Flink：部署和运维复杂度高，不适合学习型项目
- Reactor Kafka：响应式编程增加复杂度

**Implementation Notes**:
```java
@KafkaListener(topics = "device-events", groupId = "realtime-consumer")
public void consume(EventEnvelope event) {
    // 实时处理逻辑
}
```

---

### 3. Redis 轨迹存储策略

**Decision**: 使用 ZSET 存储最近 N 个轨迹点

**Rationale**:
- ZSET 按时间戳自动排序，查询效率高
- 可通过 ZREMRANGEBYRANK 限制数量，自动淘汰旧数据
- 支持范围查询，便于获取指定时间段轨迹

**Alternatives Considered**:
- LIST：无法按时间范围查询
- HASH：需要额外维护时间索引
- Stream：功能更强但 MVP 阶段不需要

**Implementation Notes**:
```redis
# 添加轨迹点
ZADD device:trajectory:{tenantId}:{deviceId} {timestamp} {json}

# 获取最近100个点
ZREVRANGE device:trajectory:{tenantId}:{deviceId} 0 99 WITHSCORES

# 限制最多保留100个点
ZREMRANGEBYRANK device:trajectory:{tenantId}:{deviceId} 0 -101
```

---

### 4. Token 设备认证方案

**Decision**: 设备 Token = SHA256(tenantId + deviceId + secretKey + timestamp)

**Rationale**:
- 无状态验证，无需每次查库
- 可包含过期时间，支持 Token 刷新
- 实现简单，安全性足够（MVP 阶段）

**Alternatives Considered**:
- JWT：功能更强但设备端实现复杂
- 证书双向认证：安全性高但运维成本高，后续升级
- 简单密钥：安全性不足，易泄露

**Implementation Notes**:
```java
// Token 生成
public String generateToken(String tenantId, String deviceId) {
    String raw = tenantId + deviceId + secretKey + System.currentTimeMillis();
    return DigestUtils.sha256Hex(raw);
}

// Token 验证（EMQX/Webhook）
public boolean validateToken(String token, String tenantId, String deviceId) {
    // 查询 Redis 缓存的 Token 进行比对
}
```

---

### 5. 历史重放实现方案

**Decision**: 基于 MongoDB 时间范围查询 + 手动触发

**Rationale**:
- MongoDB 原始数据已按时间索引，查询效率高
- 手动触发避免自动重放的资源消耗
- 实现简单，灵活性高

**Alternatives Considered**:
- Kafka 重置 Offset：影响正常消费，不适合局部重放
- 自动定时重放：资源消耗不可控
- 基于 Flink：过度设计

**Implementation Notes**:
```java
// 按时间窗口查询原始事件
Query query = Query.query(
    Criteria.where("tenantId").is(tenantId)
        .and("deviceId").is(deviceId)
        .and("timestamp").gte(startTime).lte(endTime)
        .and("processed").is(false)
);

// 重新发送到 Kafka 进行处理
List<RawEvent> events = mongoTemplate.find(query, RawEvent.class);
events.forEach(e -> kafkaTemplate.send("device-events", e.toEnvelope()));
```

---

### 6. 多租户上下文传递

**Decision**: 基于 ThreadLocal + Sa-Token 实现

**Rationale**:
- Sa-Token 提供完善的会话管理
- ThreadLocal 保证线程隔离
- 结合 MyBatis Plus 拦截器自动注入租户条件

**Alternatives Considered**:
- 纯 JWT：需要每次解析，性能略低
- 手动传递：代码侵入性高，易遗漏

**Implementation Notes**:
```java
// 租户上下文
public class TenantContext {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }
}

// MyBatis Plus 拦截器
public class TenantInterceptor implements InnerInterceptor {
    @Override
    public void beforeQuery(Executor executor, ...) {
        String tenantId = TenantContext.getTenantId();
        // 自动添加 tenant_id 条件
    }
}
```

---

### 7. 前端实时推送方案

**Decision**: 采用 SSE (Server-Sent Events)

**Rationale**:
- 单向推送满足轨迹展示需求
- 比 WebSocket 实现更简单
- 原生支持断线重连
- HTTP 协议，无需额外端口

**Alternatives Considered**:
- WebSocket：双向通信但 MVP 不需要
- 轮询：延迟高，资源浪费
- 长轮询：实现复杂，连接管理麻烦

**Implementation Notes**:
```java
@GetMapping(value = "/devices/{id}/trajectory/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<TrajectoryPoint> streamTrajectory(@PathVariable Long id) {
    return Flux.interval(Duration.ofMillis(500))
        .map(t -> trajectoryService.getLatest(id));
}
```

---

## 技术债务记录

| 项目 | 描述 | 后续规划 |
|------|------|---------|
| Token 安全性 | 当前 Token 无加密，后续需升级 | M4+ 引入证书 |
| 消费者单点 | 单消费者组，无高可用 | M4+ 增加副本 |
| MongoDB 索引 | 需根据实际查询优化 | M3 后优化 |
| 前端轨迹渲染 | 简单折线图，后续优化 | M4+ 引入地图组件 |

---

## 依赖版本确认

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | LTS，支持虚拟线程 |
| Spring Boot | 3.2.x | 最新稳定版 |
| Spring Cloud Alibaba | 2022.0.x | 兼容 Spring Boot 3.x |
| Nacos | 2.3.x | 支持 Spring Cloud Alibaba |
| EMQX | 5.x | 内置 Kafka 集成 |
| Kafka | 3.6.x | 最新稳定版 |
| PostgreSQL | 15.x | LTS |
| Redis | 7.x | 最新稳定版 |
| MongoDB | 7.x | 最新稳定版 |
| Vue | 3.4.x | 最新稳定版 |
| Element Plus | 2.5.x | 最新稳定版 |
