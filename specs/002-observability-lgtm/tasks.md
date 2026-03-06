# Tasks: 可观测性系统 (LGTM Stack)

**Input**: Design documents from `/specs/002-observability-lgtm/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**Tests**: 本项目不强制 TDD，测试任务为可选。

**Organization**: 任务按用户故事分组，支持独立实现和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1, US2, US3, US4, US5）
- 包含精确的文件路径

## Path Conventions

- **后端**: `backend/` (多模块 Maven 项目)
- **基础设施**: `infrastructure/docker/` (Docker Compose 配置)
- **Grafana**: `infrastructure/docker/grafana/` (仪表盘和告警)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 可观测性基础设施配置

### 1.1 Docker Compose 可观测性服务

- [x] T001 创建 Docker Compose 可观测性配置 infrastructure/docker/docker-compose.observability.yml
- [x] T002 [P] 创建 Prometheus 目录 infrastructure/docker/prometheus/
- [x] T003 [P] 创建 Loki 目录 infrastructure/docker/loki/
- [x] T004 [P] 创建 Tempo 目录 infrastructure/docker/tempo/
- [x] T005 [P] 创建 Grafana 目录结构 infrastructure/docker/grafana/provisioning/

### 1.2 后端依赖管理

- [x] T006 更新父 POM 添加可观测性依赖管理 backend/pom.xml
- [x] T007 [P] 创建 common-observability 模块 backend/common/common-observability/pom.xml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 所有用户故事依赖的共享配置

**⚠️ CRITICAL**: 此阶段完成前，不能开始任何用户故事开发

### 2.1 Prometheus 配置

- [x] T008 创建 Prometheus 配置文件 infrastructure/docker/prometheus/prometheus.yml
- [x] T009 配置服务发现和抓取规则 infrastructure/docker/prometheus/prometheus.yml

### 2.2 Loki 配置

- [x] T010 创建 Loki 配置文件 infrastructure/docker/loki/loki-config.yaml
- [x] T011 配置日志保留和压缩策略 infrastructure/docker/loki/loki-config.yaml

### 2.3 Tempo 配置

- [x] T012 创建 Tempo 配置文件 infrastructure/docker/tempo/tempo.yaml
- [x] T013 配置 OTLP 接收器和存储 infrastructure/docker/tempo/tempo.yaml

### 2.4 Grafana 数据源

- [x] T014 创建 Grafana 数据源配置 infrastructure/docker/grafana/provisioning/datasources/datasources.yml

### 2.5 通用可观测性模块

- [x] T015 创建可观测性自动配置类 backend/common/common-observability/src/main/java/com/openiot/common/observability/config/ObservabilityAutoConfiguration.java
- [x] T016 [P] 创建 Trace ID 过滤器 backend/common/common-observability/src/main/java/com/openiot/common/observability/filter/TraceIdFilter.java
- [x] T017 [P] 创建 Tenant ID 日志注入器 backend/common/common-observability/src/main/java/com/openiot/common/observability/filter/TenantIdMdcFilter.java

**Checkpoint**: ✅ 基础设施就绪 - 用户故事开发可以并行开始

---

## Phase 3: User Story 1 - 实时监控仪表盘 (Priority: P1) 🎯 MVP

**Goal**: 统一仪表盘实时查看所有微服务运行状态

**Independent Test**: 启动 Grafana 后，访问预置仪表盘，能看到所有微服务的实时指标

### Implementation for User Story 1

#### 各服务 Actuator 配置

- [x] T018 [US1] 配置 Gateway Actuator 端点 backend/gateway-service/src/main/resources/application.yml
- [x] T019 [P] [US1] 配置 Tenant Service Actuator 端点 backend/tenant-service/src/main/resources/application.yml
- [x] T020 [P] [US1] 配置 Device Service Actuator 端点 backend/device-service/src/main/resources/application.yml
- [x] T021 [P] [US1] 配置 Data Service Actuator 端点 backend/data-service/src/main/resources/application.yml
- [x] T022 [P] [US1] 配置 Connect Service Actuator 端点 backend/connect-service/src/main/resources/application.yml

#### JVM 和系统指标

- [x] T023 [US1] 添加 HikariCP 指标配置 backend/common/common-observability/src/main/java/com/openiot/common/observability/config/HikariMetricsConfig.java
- [x] T024 [P] [US1] 添加 Lettuce Redis 指标配置 backend/common/common-observability/src/main/java/com/openiot/common/observability/config/RedisMetricsConfig.java

#### Grafana 仪表盘

- [x] T025 [US1] 创建仪表盘配置文件 infrastructure/docker/grafana/provisioning/dashboards/dashboard.yml
- [x] T026 [US1] 创建服务概览仪表盘 infrastructure/docker/grafana/provisioning/dashboards/service-overview.json
- [x] T027 [US1] 创建 JVM 详情仪表盘 infrastructure/docker/grafana/provisioning/dashboards/jvm-details.json

**Checkpoint**: ✅ User Story 1 完成 - 可通过 Grafana 查看所有服务状态

---

## Phase 4: User Story 2 - 分布式链路追踪 (Priority: P1)

**Goal**: 通过 Trace ID 查看完整调用链路和各节点耗时

**Independent Test**: 发起跨服务请求，在 Tempo 中通过 Trace ID 查看完整调用链

### Implementation for User Story 2

#### OpenTelemetry 依赖和配置

- [x] T028 [US2] 添加 OpenTelemetry 依赖到 common-observability backend/common/common-observability/pom.xml
- [x] T029 [US2] 创建 Tracing 配置类 backend/common/common-observability/src/main/java/com/openiot/common/observability/config/TracingConfig.java

#### 各服务 Tracing 配置

- [x] T030 [US2] 配置 Gateway Tracing backend/gateway-service/src/main/resources/application.yml
- [x] T031 [P] [US2] 配置 Tenant Service Tracing backend/tenant-service/src/main/resources/application.yml
- [x] T032 [P] [US2] 配置 Device Service Tracing backend/device-service/src/main/resources/application.yml
- [x] T033 [P] [US2] 配置 Data Service Tracing backend/data-service/src/main/resources/application.yml
- [x] T034 [P] [US2] 配置 Connect Service Tracing backend/connect-service/src/main/resources/application.yml

#### Kafka 消息 Trace 传播

- [x] T035 [US2] 创建 Kafka Tracing 配置 backend/common/common-kafka/src/main/java/com/openiot/common/kafka/config/KafkaTracingConfig.java
- [x] T036 [US2] 配置 Kafka Producer Trace 传播 backend/common/common-kafka/src/main/java/com/openiot/common/kafka/producer/TracingProducerInterceptor.java
- [x] T037 [US2] 配置 Kafka Consumer Trace 传播 backend/common/common-kafka/src/main/java/com/openiot/common/kafka/consumer/TracingConsumerInterceptor.java

**Checkpoint**: ✅ User Story 2 完成 - 可通过 Trace ID 追踪跨服务调用

---

## Phase 5: User Story 3 - 结构化日志查询 (Priority: P2)

**Goal**: 使用结构化字段进行精确日志查询

**Independent Test**: 在 Grafana/Loki 中使用 LogQL 查询特定租户、特定服务的日志

### Implementation for User Story 3

#### Logback JSON 配置

- [x] T038 [US3] 添加 logstash-logback-encoder 依赖 backend/pom.xml
- [x] T039 [US3] 创建通用 Logback 配置模板 backend/common/common-observability/src/main/resources/logback-spring.xml

#### 各服务 Logback 配置

- [x] T040 [US3] 配置 Gateway JSON 日志 backend/gateway-service/src/main/resources/logback-spring.xml
- [x] T041 [P] [US3] 配置 Tenant Service JSON 日志 backend/tenant-service/src/main/resources/logback-spring.xml
- [x] T042 [P] [US3] 配置 Device Service JSON 日志 backend/device-service/src/main/resources/logback-spring.xml
- [x] T043 [P] [US3] 配置 Data Service JSON 日志 backend/data-service/src/main/resources/logback-spring.xml
- [x] T044 [P] [US3] 配置 Connect Service JSON 日志 backend/connect-service/src/main/resources/logback-spring.xml

#### 敏感信息脱敏

- [x] T045 [US3] 创建日志脱敏过滤器 backend/common/common-observability/src/main/java/com/openiot/common/observability/filter/SensitiveDataFilter.java

**Checkpoint**: ✅ User Story 3 完成 - 可通过结构化字段查询日志

---

## Phase 6: User Story 4 - 智能告警 (Priority: P2)

**Goal**: 系统异常时自动通知

**Independent Test**: 模拟服务故障，在 1 分钟内收到告警通知

### Implementation for User Story 4

#### Prometheus 告警规则

- [x] T046 [US4] 创建告警规则文件 infrastructure/docker/prometheus/alert-rules.yml
- [x] T047 [US4] 配置服务健康告警 infrastructure/docker/prometheus/alert-rules.yml
- [x] T048 [P] [US4] 配置错误率告警 infrastructure/docker/prometheus/alert-rules.yml
- [x] T049 [P] [US4] 配置延迟告警 infrastructure/docker/prometheus/alert-rules.yml
- [x] T050 [P] [US4] 配置资源告警 infrastructure/docker/prometheus/alert-rules.yml
- [x] T051 [P] [US4] 配置 Kafka 积压告警 infrastructure/docker/prometheus/alert-rules.yml

#### Alertmanager 配置

- [x] T052 [US4] 添加 Alertmanager 到 Docker Compose infrastructure/docker/docker-compose.observability.yml
- [x] T053 [US4] 创建 Alertmanager 配置 infrastructure/docker/alertmanager/alertmanager.yml
- [x] T054 [US4] 配置告警通知渠道 infrastructure/docker/alertmanager/alertmanager.yml
- [x] T055 [US4] 配置告警分组和静默规则 infrastructure/docker/alertmanager/alertmanager.yml

#### Grafana 告警

- [x] T056 [US4] 创建 Grafana 告警规则配置 infrastructure/docker/grafana/provisioning/alerting/alert-rules.yml

**Checkpoint**: ✅ User Story 4 完成 - 服务异常时自动触发告警

---

## Phase 7: User Story 5 - 业务指标监控 (Priority: P3)

**Goal**: 查看设备在线数、消息吞吐量等业务指标

**Independent Test**: 在 Grafana 业务仪表盘中查看实时设备在线数、消息总量

### Implementation for User Story 5

#### 业务指标收集

- [x] T057 [US5] 创建业务指标类 backend/common/common-observability/src/main/java/com/openiot/common/observability/metrics/BusinessMetrics.java
- [x] T058 [US5] 创建设备指标收集器 backend/device-service/src/main/java/com/openiot/device/metrics/DeviceMetricsCollector.java
- [x] T059 [US5] 创建消息指标收集器 backend/data-service/src/main/java/com/openiot/data/metrics/MessageMetricsCollector.java
- [x] T060 [P] [US5] 创建 Kafka 消费延迟指标 backend/data-service/src/main/java/com/openiot/data/metrics/KafkaLagMetrics.java

#### 业务仪表盘

- [x] T061 [US5] 创建业务指标仪表盘 infrastructure/docker/grafana/provisioning/dashboards/business-metrics.json

**Checkpoint**: ✅ User Story 5 完成 - 可查看业务运营指标

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 跨故事的改进和优化

### 健康检查完善

- [x] T062 [P] 完善 Gateway 健康检查 backend/gateway-service/src/main/java/com/openiot/gateway/config/HealthCheckConfig.java
- [x] T063 [P] 完善 Device Service 健康检查 backend/device-service/src/main/java/com/openiot/device/config/HealthCheckConfig.java
- [x] T064 [P] 完善 Data Service 健康检查 backend/data-service/src/main/java/com/openiot/data/config/HealthCheckConfig.java

### 文档更新

- [x] T065 更新 README 添加可观测性说明 README.md
- [x] T066 [P] 更新部署脚本添加可观测性服务 scripts/deploy.sh

### 验证

- [x] T067 运行 quickstart.md 验证端到端流程
- [x] T068 验证可观测性系统开销 < 5%
- [x] T069 验证仪表盘加载时间 < 3s

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖 - 立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 - **阻塞所有用户故事**
- **Phase 3-7 (User Stories)**: 依赖 Phase 2 完成
  - US1 (仪表盘) 和 US2 (链路追踪) 优先级最高，可并行
  - US3 (日志) 依赖 US2 的 Trace ID 机制
  - US4 (告警) 依赖 US1 的指标采集
  - US5 (业务指标) 相对独立
- **Phase 8 (Polish)**: 依赖所需用户故事完成

### User Story Dependencies

```
Phase 2 (Foundational)
         │
    ┌────┴────┐
    │         │
   US1       US2
 (仪表盘)   (链路追踪)
    │         │
    └────┬────┘
         │
    ┌────┴────┐
    │         │
   US3       US4
   (日志)    (告警)
         │
        US5
     (业务指标)
```

### Within Each User Story

1. 配置文件优先
2. 依赖注入配置
3. 各服务具体配置
4. 仪表盘/规则配置
5. 集成验证

### Parallel Opportunities

- Phase 1: T002-T005 可并行
- Phase 2: T016-T017 可并行
- Phase 3: T019-T022 可并行
- Phase 4: T031-T034 可并行
- Phase 5: T041-T044 可并行
- Phase 6: T048-T051 可并行
- Phase 8: T062-T064, T065-T066 可并行

---

## Parallel Example: User Story 1

```bash
# 并行配置各服务 Actuator
Task: "T019 [P] [US1] 配置 Tenant Service Actuator 端点"
Task: "T020 [P] [US1] 配置 Device Service Actuator 端点"
Task: "T021 [P] [US1] 配置 Data Service Actuator 端点"
Task: "T022 [P] [US1] 配置 Connect Service Actuator 端点"
```

---

## Implementation Strategy

### MVP First (Phase 1-3)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational (阻塞点)
3. 完成 Phase 3: US1 (仪表盘) - **MVP 核心功能**
4. **STOP and VALIDATE**: 验证 Grafana 能查看所有服务状态

### Incremental Delivery

1. Setup + Foundational → 基础设施就绪
2. + US1 → 仪表盘可用 (MVP!)
3. + US2 → 链路追踪可用
4. + US3 → 日志查询可用
5. + US4 → 告警可用
6. + US5 → 业务指标可用
7. + Phase 8 → 优化打磨

---

## Notes

- [P] 任务 = 不同文件，无依赖，可并行
- [Story] 标签映射到 spec.md 中的用户故事
- 每个用户故事应独立可完成和测试
- 在每个检查点验证故事独立性
- 提交时按任务或逻辑组提交
- 避免模糊任务、同文件冲突、跨故事依赖

---

## Summary

| 统计项 | 数量 |
|--------|------|
| **总任务数** | 69 |
| **Phase 1 (Setup)** | 7 |
| **Phase 2 (Foundational)** | 10 |
| **Phase 3 (US1 仪表盘)** | 10 |
| **Phase 4 (US2 链路追踪)** | 10 |
| **Phase 5 (US3 日志)** | 8 |
| **Phase 6 (US4 告警)** | 11 |
| **Phase 7 (US5 业务指标)** | 5 |
| **Phase 8 (Polish)** | 8 |
| **并行机会** | ~25 个任务可并行 |

**MVP 范围**: Phase 1-3，约 27 个任务

## Progress Summary

- **已完成**: 69/69 任务 (100%)
- **所有任务已完成** ✅
