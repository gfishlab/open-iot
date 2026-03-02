# LGTM Stack 可观测性指南

## 目录
- [什么是 LGTM Stack](#什么是-lgtm-stack)
- [各组件详细介绍](#各组件详细介绍)
  - [Grafana - 统一可视化平台](#grafana---统一可视化平台)
  - [Prometheus - 指标采集与存储](#prometheus---指标采集与存储)
  - [Loki - 日志聚合系统](#loki---日志聚合系统)
  - [Tempo - 分布式链路追踪](#tempo---分布式链路追踪)
- [组件协作关系](#组件协作关系)
- [数据流转过程](#数据流转过程)
- [实际应用场景](#实际应用场景)
- [Open-IoT 项目配置](#open-iot-项目配置)
- [最佳实践](#最佳实践)

---

## 什么是 LGTM Stack

**LGTM Stack** 是一套开源的可观测性解决方案，由四个核心组件组成：

- **G**rafana - 统一可视化平台
- **L**oki - 日志聚合系统
- **T**empo - 分布式链路追踪
- **P**rometheus - 指标采集与存储

> **注意**：虽然缩写是 LGTM，但通常按照 Prometheus → Loki → Tempo → Grafana 的顺序部署和使用。

### 为什么需要 LGTM Stack？

在现代微服务架构中，系统由多个服务组成，传统的监控方式已经无法满足需求。我们需要：

1. **指标（Metrics）**：了解系统的整体运行状态（CPU、内存、请求量等）
2. **日志（Logs）**：记录系统运行的详细信息，用于故障排查
3. **链路追踪（Traces）**：跟踪请求在多个服务之间的调用链路，定位性能瓶颈

LGTM Stack 提供了这三大支柱的完整解决方案，并且通过 Grafana 实现统一的可视化界面。

---

## 各组件详细介绍

### Grafana - 统一可视化平台

#### 核心作用
Grafana 是一个开源的数据可视化和监控平台，**它本身不存储数据，而是从各种数据源读取数据进行展示**。

#### 主要功能
1. **多数据源支持**：支持 Prometheus、Loki、Tempo、InfluxDB、Elasticsearch 等 30+ 种数据源
2. **丰富的可视化**：提供图表、仪表盘、表格、热力图等多种可视化方式
3. **告警管理**：支持基于指标的告警规则配置和通知
4. **仪表盘共享**：可以导入/导出仪表盘配置，与团队共享

#### 在 LGTM 中的角色
Grafana 是整个栈的**统一入口**，用户通过 Grafana：
- 查看 Prometheus 的指标数据
- 查询 Loki 的日志
- 分析 Tempo 的链路追踪
- 配置告警规则

#### 访问地址
```
http://localhost:3000
默认账号：admin / admin
```

---

### Prometheus - 指标采集与存储

#### 核心作用
Prometheus 是一个开源的系统监控和告警工具，主要负责**采集、存储和查询时间序列指标数据**。

#### 工作原理
1. **Pull 模式采集**：Prometheus 主动从目标服务拉取指标数据（默认 15 秒一次）
2. **数据存储**：将指标数据存储在本地时序数据库（TSDB）中
3. **PromQL 查询**：提供强大的查询语言 PromQL 进行数据分析
4. **告警规则**：支持基于 PromQL 表达式的告警规则

#### 指标类型
- **Counter（计数器）**：只增不减的累积值，如请求总数、错误总数
- **Gauge（仪表盘）**：可增可减的瞬时值，如当前内存使用、当前连接数
- **Histogram（直方图）**：对观测值进行采样并统计分布，如请求延迟分布
- **Summary（摘要）**：类似 Histogram，但计算分位数在客户端完成

#### 在 Open-IoT 中的应用
```yaml
# 应用服务暴露指标端点
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  prometheus:
    metrics:
      export:
        enabled: true
```

#### 采集的指标示例
```promql
# JVM 堆内存使用量
jvm_memory_used_bytes{area="heap"}

# HTTP 请求总数
http_server_requests_seconds_count{method="GET",uri="/api/devices"}

# 数据库连接池活跃连接数
hikaricp_connections_active{pool="openiot"}

# Kafka 消费延迟
kafka_consumer_group_lag{group="data-service",topic="device-data"}
```

#### 访问地址
```
http://localhost:9090
```

---

### Loki - 日志聚合系统

#### 核心作用
Loki 是 Grafana Labs 开源的日志聚合系统，**专门用于存储和查询日志**。它的设计灵感来自 Prometheus，采用标签（Labels）来索引日志。

#### 与 ELK 的区别
| 特性 | Loki | ELK (Elasticsearch) |
|------|------|---------------------|
| 存储方式 | 只索引标签，不索引日志内容 | 全文索引 |
| 资源消耗 | 低（仅为 Elasticsearch 的 10-20%） | 高 |
| 查询语言 | LogQL（类似 PromQL） | Lucene |
| 适用场景 | 云原生、Kubernetes | 传统日志分析 |

#### 工作原理
1. **日志采集**：应用将日志输出到标准输出，由 Promtail 或 Docker 日志驱动采集
2. **标签索引**：Loki 只索引日志的标签（如 `service`、`level`、`tenantId`），不索引日志内容
3. **日志存储**：日志内容以压缩形式存储在对象存储或本地文件系统
4. **LogQL 查询**：使用 LogQL 查询日志，支持正则匹配和过滤

#### 在 Open-IoT 中的应用
```xml
<!-- logback-spring.xml 配置 JSON 格式日志 -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>tenantId</includeMdcKeyName>
        <includeMdcKeyName>service</includeMdcKeyName>
    </encoder>
</appender>
```

#### LogQL 查询示例
```logql
# 查询特定服务的 ERROR 日志
{service="device-service", level="ERROR"}

# 查询特定租户的所有日志
{tenantId="tenant-001"}

# 根据 Trace ID 查询日志
{service="gateway-service"} |= "trace-id-12345"

# 统计最近 5 分钟的错误日志数量
count_over_time({level="ERROR"}[5m])
```

#### 访问地址
```
http://localhost:3100
```

---

### Tempo - 分布式链路追踪

#### 核心作用
Tempo 是 Grafana Labs 开源的分布式链路追踪后端，**用于存储和查询分布式追踪数据**。

#### 与 Jaeger/Zipkin 的区别
| 特性 | Tempo | Jaeger | Zipkin |
|------|-------|--------|--------|
| 存储成本 | 极低（仅索引 Trace ID） | 中等 | 中等 |
| 扩展性 | 高（依赖对象存储） | 中等 | 中等 |
| 集成性 | 深度集成 Grafana LGTM | 独立系统 | 独立系统 |
| 协议支持 | OTLP、Jaeger、Zipkin | Jaeger | Zipkin |

#### 工作原理
1. **Trace 生成**：应用通过 OTLP（OpenTelemetry Protocol）协议将追踪数据发送到 Tempo
2. **Span 传播**：每个请求生成唯一的 Trace ID，在服务间传播
3. **数据存储**：Tempo 只索引 Trace ID，Span 数据存储在对象存储中
4. **TraceQL 查询**：使用 TraceQL 或直接通过 Trace ID 查询完整链路

#### Trace 结构
```
Trace (Trace ID: abc123)
├── Span 1: HTTP GET /api/devices (Gateway Service) - 150ms
│   ├── Span 2: Database Query (Device Service) - 50ms
│   └── Span 3: Redis Cache (Device Service) - 10ms
└── Span 4: Kafka Publish (Device Service) - 20ms
```

#### 在 Open-IoT 中的应用
```yaml
# application.yml 配置
openiot:
  observability:
    tracing:
      enabled: true
      sampling-probability: 1.0  # 采样率 100%
      otlp-endpoint: http://localhost:4317
```

#### TraceQL 查询示例
```tracing
# 查询特定服务的追踪
{service="gateway-service"}

# 查询耗时超过 1 秒的追踪
{duration > 1s}

# 查询包含错误的追踪
{status=error}

# 根据 Trace ID 查询
{traceId="abc123def456"}
```

#### 访问地址
```
http://localhost:3200  # Tempo API
http://localhost:4317  # OTLP gRPC
http://localhost:4318  # OTLP HTTP
```

---

## 组件协作关系

### 架构图
```
┌─────────────────────────────────────────────────────────────┐
│                        应用服务层                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Gateway  │  │  Device  │  │   Data   │  │  Tenant  │   │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       │             │             │             │          │
│       └──────────────┴──────────────┴──────────────┘          │
│                      │                                        │
│         ┌────────────┼────────────┬────────────┐             │
│         │            │            │            │             │
│         ▼            ▼            ▼            ▼             │
└─────────────────────────────────────────────────────────────┘
       Metrics       Logs        Traces
         │            │            │
         ▼            ▼            ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │Prometheus│ │   Loki   │ │  Tempo   │
   │  :9090   │ │  :3100   │ │  :4317   │
   └─────┬────┘ └─────┬────┘ └─────┬────┘
         │            │            │
         └────────────┴────────────┘
                      │
                      ▼
               ┌──────────┐
               │ Grafana  │
               │  :3000   │
               └──────────┘
                      │
                      ▼
                 用户访问
```

### 数据流转过程

#### 1. 指标流转（Metrics）
```
应用服务 → /actuator/prometheus 端点
         ↓
    Prometheus Pull（每 15 秒）
         ↓
    Prometheus TSDB 存储
         ↓
    Grafana 查询展示（PromQL）
```

**示例**：
1. Gateway Service 暴露 `http_server_requests_seconds_count` 指标
2. Prometheus 每 15 秒从 `http://gateway-service:8080/actuator/prometheus` 拉取指标
3. 用户在 Grafana 使用 PromQL 查询：`rate(http_server_requests_seconds_count[5m])`
4. Grafana 展示最近 5 分钟的请求速率图表

---

#### 2. 日志流转（Logs）
```
应用服务 → JSON 格式日志输出到 stdout
         ↓
    Docker 日志驱动 / Promtail 采集
         ↓
    Loki 接收并存储（索引标签）
         ↓
    Grafana 查询展示（LogQL）
```

**示例**：
1. Device Service 输出日志：
   ```json
   {
     "timestamp": "2026-03-02T15:30:00Z",
     "level": "ERROR",
     "service": "device-service",
     "tenantId": "tenant-001",
     "traceId": "abc123",
     "message": "Database connection failed"
   }
   ```
2. Loki 采集并索引标签：`service=device-service`, `level=ERROR`, `tenantId=tenant-001`
3. 用户在 Grafana 使用 LogQL 查询：`{service="device-service", level="ERROR"} |= "Database"`
4. Grafana 展示包含 "Database" 关键字的错误日志

---

#### 3. 链路追踪流转（Traces）
```
请求进入 Gateway Service
         ↓
    生成 Trace ID（如 abc123）
         ↓
    调用 Device Service（传播 Trace ID）
         ↓
    Device Service 调用 PostgreSQL（创建 Span）
         ↓
    Device Service 发送 Kafka 消息（创建 Span）
         ↓
    所有 Span 发送到 Tempo
         ↓
    Grafana 查询展示完整链路
```

**示例**：
1. 用户请求 `GET /api/devices` 进入 Gateway
2. Gateway 生成 Trace ID：`abc123def456`
3. Gateway 调用 Device Service，HTTP Header 携带 `traceparent: 00-abc123def456-...`
4. Device Service 执行数据库查询，创建 Span（Span ID: `span001`，耗时 50ms）
5. Device Service 发送 Kafka 消息，创建 Span（Span ID: `span002`，耗时 20ms）
6. 所有 Span 通过 OTLP 发送到 Tempo
7. 用户在 Grafana 输入 Trace ID `abc123def456`，查看完整调用链和耗时分布

---

## 实际应用场景

### 场景 1：服务故障快速定位

**问题**：用户反馈"设备列表加载很慢"

**排查步骤**：
1. **查看 Grafana 仪表盘** → 发现 Gateway Service 的 P95 延迟飙升至 5 秒
2. **点击延迟图表 → 跳转到 Tempo 链路追踪** → 查看慢请求的 Trace
3. **分析 Trace** → 发现 80% 的时间花在 Device Service 的数据库查询
4. **点击 Span → 跳转到 Loki 日志** → 发现数据库连接池等待告警
5. **根因定位**：数据库连接池配置过小（最大 10 个连接），已耗尽

**解决**：调整连接池配置 `maximum-pool-size: 20`

---

### 场景 2：跨服务调用链分析

**问题**：设备数据上报失败，但不知道哪个环节出错

**排查步骤**：
1. **在 Loki 查询错误日志**：
   ```logql
   {service="data-service", level="ERROR"} |= "device-data"
   ```
2. **从日志中提取 Trace ID**：`traceId: xyz789`
3. **在 Tempo 查询 Trace** → 看到完整调用链：
   ```
   Gateway (10ms) → Device Service (20ms) → Kafka (5ms) → Data Service (150ms)
     └── Database Insert (140ms) - 失败
   ```
4. **点击 Database Span** → 查看错误信息：`Duplicate key violation`
5. **根因定位**：设备重复上报相同数据，主键冲突

**解决**：业务逻辑增加幂等性检查

---

### 场景 3：性能优化

**问题**：Data Service 消费 Kafka 消息速度慢，积压严重

**排查步骤**：
1. **查看 Prometheus 指标**：
   ```promql
   # Kafka 消费延迟
   kafka_consumer_group_lag{group="data-service"}  # 100,000 条消息积压

   # 消费速率
   rate(kafka_consumer_records_consumed_total[5m])  # 500 条/秒
   ```
2. **分析 JVM 指标** → 发现 GC 频繁，CPU 使用率 90%
3. **查看链路追踪** → 单条消息处理耗时 200ms（数据库写入慢）
4. **根因定位**：单条消息处理太慢，导致积压

**优化方案**：
- 增加消费者并发数：`concurrency: 3` → `concurrency: 10`
- 优化数据库批量插入：`batch-size: 100`
- 调整 JVM 内存：`-Xmx2g` → `-Xmx4g`

**优化后**：
- 消费速率提升至 5000 条/秒
- 积压在 10 分钟内清零

---

### 场景 4：告警联动

**配置告警规则**（Prometheus + Alertmanager）：
```yaml
# 服务健康告警
- alert: ServiceDown
  expr: up{job="spring-boot-services"} == 0
  for: 1m
  labels:
    severity: critical
  annotations:
    summary: "服务 {{ $labels.service }} 已宕机"

# 错误率告警
- alert: HighErrorRate
  expr: |
    sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
    /
    sum(rate(http_server_requests_seconds_count[5m])) by (service) > 0.01
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "服务 {{ $labels.service }} 错误率超过 1%"
```

**告警流程**：
1. Device Service 停止 → Prometheus 检测到 `up{service="device-service"} == 0`
2. 持续 1 分钟 → 触发 `ServiceDown` 告警
3. Alertmanager 发送通知（邮件/钉钉/企业微信）
4. 运维人员收到告警，在 Grafana 查看详细信息
5. 重启服务 → Prometheus 检测到服务恢复
6. Alertmanager 发送恢复通知

---

## Open-IoT 项目配置

### Docker Compose 配置

#### Prometheus 配置
```yaml
prometheus:
  image: prom/prometheus:v2.48.0
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - ./prometheus/alert-rules.yml:/etc/prometheus/alert-rules.yml
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.retention.time=15d'  # 数据保留 15 天
```

#### Loki 配置
```yaml
loki:
  image: grafana/loki:2.9.0
  ports:
    - "3100:3100"
  volumes:
    - ./loki/loki-config.yaml:/etc/loki/local-config.yaml
  command: -config.file=/etc/loki/local-config.yaml
```

#### Tempo 配置
```yaml
tempo:
  image: grafana/tempo:2.3.0
  ports:
    - "3200:3200"   # Tempo HTTP API
    - "4317:4317"   # OTLP gRPC
    - "4318:4318"   # OTLP HTTP
  volumes:
    - ./tempo/tempo.yaml:/etc/tempo.yaml
  command: -config.file=/etc/tempo.yaml
```

#### Grafana 配置
```yaml
grafana:
  image: grafana/grafana:10.2.0
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_FEATURE_TOGGLES_ENABLE=traceToLogs  # 启用 Trace → Logs 跳转
  volumes:
    - ./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources
    - ./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards
```

---

### Spring Boot 应用配置

#### pom.xml 依赖
```xml
<!-- Micrometer Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- OpenTelemetry OTLP -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Logback JSON Encoder -->
<dependency>
    <groupId>net.logstash-logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

#### application.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      service: ${spring.application.name}

openiot:
  observability:
    tracing:
      enabled: true
      sampling-probability: 1.0
      otlp-endpoint: http://localhost:4317
    logging:
      tenant-mdc: true
      sensitive-data-masking: true
```

---

## 最佳实践

### 1. 数据保留策略
```yaml
# Prometheus - 指标保留 15 天
--storage.tsdb.retention.time=15d

# Loki - 日志保留 7 天
table_manager:
  retention_deletes_enabled: true
  retention_period: 168h  # 7 天

# Tempo - 链路保留 3 天
compactor:
  compaction:
    block_retention: 72h  # 3 天
```

### 2. 采样策略
```yaml
# 生产环境建议采样率 10%
openiot:
  observability:
    tracing:
      sampling-probability: 0.1
```

### 3. 标签规范
```yaml
# 统一标签命名
metrics:
  tags:
    service: ${spring.application.name}  # 服务名
    version: 1.0.0                       # 版本号
    env: production                      # 环境
```

### 4. 告警分级
- **Critical**：立即处理（服务宕机、数据库不可用）
- **Warning**：关注但不紧急（错误率 > 1%、延迟 > 3s）
- **Info**：仅记录（配置变更、部署完成）

### 5. 仪表盘组织
```
📁 Open-IoT
├── 📊 服务概览（所有服务健康状态、请求量、错误率）
├── 📊 JVM 详情（内存、GC、线程）
├── 📊 数据库监控（连接池、慢查询）
├── 📊 Kafka 监控（消费延迟、吞吐量）
└── 📊 业务指标（设备在线数、消息量）
```

---

## 常见问题

### Q1: Grafana 和 Tempo 是同样的软件吗？

**不是**，它们完全不同：

| 组件 | 类型 | 作用 |
|------|------|------|
| **Grafana** | 可视化平台 | **展示**各种数据源的图表和仪表盘，不存储数据 |
| **Tempo** | 链路追踪存储 | **存储和查询**分布式追踪数据，不提供可视化界面 |

**关系**：Grafana 从 Tempo 读取追踪数据并展示给用户。

---

### Q2: 为什么不用 ELK 而用 Loki？

| 对比项 | Loki | ELK (Elasticsearch) |
|--------|------|---------------------|
| **资源消耗** | 低（仅为 ELK 的 10-20%） | 高 |
| **查询方式** | 标签过滤 + 正则匹配 | 全文搜索 |
| **学习成本** | 低（LogQL 类似 PromQL） | 中（需要学习 Lucene） |
| **适用场景** | 云原生、Kubernetes | 复杂日志分析 |

**选择 Loki 的原因**：
- 与 Prometheus、Grafana 深度集成
- 资源消耗低，适合中小规模部署
- 操作简单，学习曲线平缓

---

### Q3: Trace ID 如何在服务间传播？

通过 HTTP Header 传播：

```http
GET /api/devices HTTP/1.1
Host: gateway-service:8080
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

- Spring Boot 自动通过 `spring-cloud-sleuth` 传播
- Kafka 消息通过 Header 携带 Trace ID
- 所有组件（数据库、Redis）自动关联到同一个 Trace

---

### Q4: 如何降低可观测性系统的成本？

1. **调整采样率**：生产环境 Trace 采样率降至 10%
2. **缩短保留时间**：日志保留 7 天，链路保留 3 天
3. **使用对象存储**：Loki 和 Tempo 数据存储在 S3/MinIO
4. **聚合指标**：只保留关键指标，细粒度指标通过预聚合降低存储

---

## 总结

LGTM Stack 提供了一套完整、高效、低成本的可观测性解决方案：

- **Prometheus**：采集和存储指标数据，使用 PromQL 查询
- **Loki**：采集和存储日志数据，使用 LogQL 查询
- **Tempo**：采集和存储链路追踪，使用 TraceQL 查询
- **Grafana**：统一可视化界面，整合三大支柱

通过 LGTM Stack，可以实现：
- ✅ 快速定位服务故障
- ✅ 分析跨服务调用链
- ✅ 优化系统性能
- ✅ 智能告警和通知

**下一步**：
1. 访问 Grafana（http://localhost:3000）查看预置仪表盘
2. 尝试查询日志和链路追踪
3. 配置自定义告警规则
4. 根据实际需求调整采样率和保留策略

---

**参考资源**：
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Loki 官方文档](https://grafana.com/docs/loki/latest/)
- [Tempo 官方文档](https://grafana.com/docs/tempo/latest/)
- [Grafana 官方文档](https://grafana.com/docs/grafana/latest/)
