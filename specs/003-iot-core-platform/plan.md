# Implementation Plan: IoT Platform Core Functionality

**Branch**: `003-iot-core-platform` | **Date**: 2026-03-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-iot-core-platform/spec.md`

## Summary

实现 IoT 平台核心功能，参考阿里云 IoT 平台架构，包括：
- **产品-设备层级管理**：建立产品作为设备模板的抽象层
- **物模型（TSL）定义**：支持属性、事件、服务的可视化定义与验证
- **可配置解析规则**：支持 JSON/JS/Regex/Binary 四种解析方式，替换硬编码逻辑
- **可配置映射规则**：将解析后的数据映射到物模型属性
- **设备属性存储**：PostgreSQL（当前状态）+ InfluxDB（历史时序数据，90 天保留）
- **多目标数据转发**：支持 Kafka/HTTP/MQTT/InfluxDB 四种转发目标
- **智能告警管理**：基于 Aviator 表达式的实时告警，支持邮件/短信/Webhook 通知
- **设备服务调用（RPC）**：支持同步/异步服务调用，30 秒超时

**技术方案**：
- GraalJS 23.1.2 作为 JavaScript 脚本引擎（沙箱模式，3 秒超时）
- InfluxDB 2.x 作为时序数据库（90 天保留策略）
- Aviator 5.4.1 作为规则表达式引擎
- Redis pub/sub 实现解析规则热更新（30 秒生效）
- 设备认证：DeviceKey + DeviceSecret（一机一密）
- 产品密钥：租户内唯一、10-20 字符（如 PROD_A1B2C3）

## Technical Context

**Language/Version**: JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.2.3
**Primary Dependencies**:
- Spring Cloud Alibaba 2022.0.0.0（微服务治理）
- Kafka（事件驱动）
- Netty（设备接入）
- MyBatis Plus 3.5.5（ORM）
- Sa-Token（认证授权）
- GraalJS 23.1.2（脚本引擎）
- Aviator 5.4.1（表达式引擎）
- InfluxDB Java Client（时序数据库）

**Storage**:
- PostgreSQL（主数据库，共享数据库架构）
- Redis（缓存 + 分布式锁 + pub/sub）
- InfluxDB 2.x（时序数据，90 天保留）
- MongoDB（设备日志，已存在）

**Testing**:
- JUnit 5 + Mockito（单元测试）
- Spring Boot Test（集成测试）
- Testcontainers（容器化测试）

**Target Platform**: Linux server（生产环境）+ Docker/Kubernetes（容器化部署）

**Project Type**: Microservice web application（前后端分离微服务架构）

**Performance Goals**:
- 10,000 messages/second throughput
- P95 latency < 3 seconds end-to-end
- Parse rule execution < 100ms (JSON/Regex) / < 3s (JavaScript)
- 100,000 concurrent devices per tenant
- Historical query < 5 seconds (90 days range)

**Constraints**:
- Parse rule hot-reload within 30 seconds
- JavaScript script timeout: 3 seconds
- JavaScript script size limit: 10KB
- InfluxDB data retention: 90 days
- Service invocation timeout: 30 seconds
- Alarm notification delivery: within 30 seconds
- 99.9% uptime for device data ingestion

**Scale/Scope**:
- 100,000 devices per tenant
- 10,000 devices initial deployment
- 10 messages/second/device average
- 8 microservices (gateway, tenant, device, connect, data, rule - new)
- 8 user stories (P1: 3, P2: 3, P3: 2)
- 53 functional requirements

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. 多租户强制原则 ✅ PASS

**合规情况**：
- ✅ 所有新增实体（Product, ParseRule, MappingRule, ForwardRule, AlarmRule, AlarmRecord, DeviceServiceInvoke）均包含 `tenant_id` 字段
- ✅ 查询默认添加租户过滤条件（MyBatis Plus 自动注入）
- ✅ 设备认证（DeviceKey + DeviceSecret）确保设备归属租户
- ✅ 产品密钥在租户内唯一（`tenant_id` + `product_key` 复合唯一索引）

**实施要点**：
- Product, ParseRule, MappingRule, ForwardRule, AlarmRule, AlarmRecord, DeviceServiceInvoke 表必须包含 `tenant_id`
- 所有 Service 层查询必须使用 `LambdaQueryWrapper` 自动注入租户条件
- 设备数据上报时，通过 DeviceKey 查询设备，自动获取 `tenant_id`

### II. 数据演进安全原则 ✅ PASS

**合规情况**：
- ✅ 所有数据库变更通过 Flyway migration（由 tenant-service 集中管理）
- ✅ 迁移脚本命名规范：`V1.3.0__add_product_and_rule_tables.sql`
- ✅ 非破坏性修改：先添加字段，数据迁移后再删除旧字段

**实施要点**：
- 在 `backend/tenant-service/src/main/resources/db/migration/` 创建 `V1.3.0__add_product_and_rule_tables.sql`
- 包含表：product, parse_rule, mapping_rule, forward_rule, alarm_rule, alarm_record, device_service_invoke
- 修改 device 表，添加 `product_id` 字段
- 所有表包含审计字段：create_time, update_time, create_by, update_by, delete_flag

### III. 渐进式微服务实践原则 ✅ PASS

**合规情况**：
- ✅ 治理能力已建：注册中心（Nacos）、配置中心（Nacos）、API 网关（Spring Cloud Gateway）
- ✅ 服务拆分适度：按业务域拆分（新增 rule-service 负责转发、告警、规则引擎）
- ✅ 边界清晰：rule-service 独立负责数据转发、告警管理、规则引擎
- ✅ 演进路径：Phase 1-2 先建立产品-设备层级，Phase 3-8 逐步添加规则、转发、告警、RPC

**实施要点**：
- 新增 `backend/rule-service` 模块
- rule-service 依赖：common-core, common-kafka, common-redis, common-observability
- rule-service 职责：ForwardRuleService, AlarmRuleService, AlarmEngine, ForwarderManager

### IV. 协议可扩展原则 ✅ PASS

**合规情况**：
- ✅ 协议层已插件化（connect-service 支持多种协议）
- ✅ 解析规则（ParseRule）与协议解耦，按产品配置
- ✅ 不写死特定协议，通过 product.protocol_type 动态选择

**实施要点**：
- ParseRuleEngine 根据产品选择解析规则，与协议无关
- 设备上报数据时，connect-service 根据 device_id 查询 product，获取 parse_rule

### V. 垂直数据链路优先原则 ✅ PASS

**合规情况**：
- ✅ Phase 1-2 优先打通：产品创建 → 设备关联 → 物模型定义 → 数据上报 → 验证
- ✅ Phase 3-4：解析规则 → 映射规则 → 数据存储
- ✅ Phase 5-6：历史数据查询 → 数据转发
- ✅ Phase 7-8：告警管理 → 服务调用
- ✅ 每个阶段形成可演示的端到端闭环

**实施要点**：
- 严格按照 Phase 1 → Phase 2 → ... → Phase 8 的顺序实施
- 每个 Phase 完成后进行端到端验证

### VI. 学习范围强制原则 ✅ PASS

**合规情况**：
- ✅ 微服务治理：Nacos（注册+配置）+ Gateway（网关）已落地
- ✅ 消息驱动架构：Kafka 已集成，用于 device-events 主题
- ✅ 高并发接入层：connect-service 基于 Netty，支持 MQTT/TCP
- ✅ 多租户数据库设计：所有表包含 `tenant_id`，查询自动注入

**实施要点**：
- rule-service 注册到 Nacos
- 数据转发使用 Kafka（device-events → forward_dlq）
- 解析规则热更新使用 Redis pub/sub

### VII. 认证边界与职责划分原则 ✅ PASS

**合规情况**：
- ✅ Gateway 统一认证：Sa-Token 校验用户 Token
- ✅ tenant-service 统一签发：登录、登出、会话管理
- ✅ 下游服务不信任公网请求的身份 Header
- ✅ 设备认证（DeviceKey + DeviceSecret）由 connect-service 验证

**实施要点**：
- 设备认证：connect-service 验证 DeviceKey + DeviceSecret，查询 device 表
- 用户认证：Gateway 校验 Token，注入 `X-Tenant-Id`, `X-User-Id`, `X-User-Role` Header
- 下游服务从 Header 获取租户和用户信息

### VIII. 严格 RBAC 权限模型原则 ✅ PASS

**合规情况**：
- ✅ RBAC 模型已实现（sys_user, sys_role, sys_permission）
- ✅ 多角色支持、租户隔离、动态加载、Redis 缓存
- ✅ 预置角色：ADMIN, TENANT_ADMIN, TENANT_USER

**实施要点**：
- 新增权限：
  - `product:view`, `product:create`, `product:update`, `product:delete`
  - `thing_model:view`, `thing_model:edit`
  - `parse_rule:view`, `parse_rule:edit`
  - `mapping_rule:view`, `mapping_rule:edit`
  - `forward_rule:view`, `forward_rule:edit`
  - `alarm_rule:view`, `alarm_rule:edit`
  - `alarm:acknowledge`
  - `device_service:invoke`
- TENANT_ADMIN 拥有所有产品相关权限
- TENANT_USER 仅拥有查看权限

### IX. 可观测性强制原则 ✅ PASS

**合规情况**：
- ✅ Metrics: Micrometer + Prometheus（已集成 common-observability）
- ✅ Logs: SLF4J + Logback + Loki（已集成 common-observability）
- ✅ Traces: Micrometer Tracing + Zipkin（已集成 common-observability）
- ✅ 健康检查：`/actuator/health/liveness`, `/actuator/health/readiness`

**实施要点**：
- 新增业务指标：
  - `openiot.device.connected.count`（在线设备数）
  - `openiot.parse_rule.execution.time`（解析规则执行时间）
  - `openiot.forward.success.count`（转发成功数）
  - `openiot.forward.failure.count`（转发失败数）
  - `openiot.alarm.triggered.count`（告警触发数）
- 新增告警规则：
  - 设备离线率 > 20%
  - 解析规则执行时间 P95 > 1s
  - 转发失败率 > 1%
  - Kafka Lag > 10000

**总体评估**：✅ **所有宪法原则通过，可进入 Phase 0 研究**

## Project Structure

### Documentation (this feature)

```text
specs/003-iot-core-platform/
├── spec.md              # Feature specification (已完成)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── common/                      # 公共模块
│   ├── common-core/            # 核心工具类
│   ├── common-kafka/           # Kafka 配置
│   ├── common-redis/           # Redis 配置
│   ├── common-mongodb/         # MongoDB 配置
│   ├── common-security/        # Sa-Token 认证
│   └── common-observability/   # 可观测性（Metrics/Logs/Traces）
├── gateway-service/            # API 网关（已有）
├── tenant-service/             # 租户服务（已有，管理 Flyway 迁移）
│   └── src/main/resources/db/migration/
│       └── V1.3.0__add_product_and_rule_tables.sql  # 新增
├── device-service/             # 设备服务（已有，扩展产品管理）
│   ├── src/main/java/com/openiot/device/
│   │   ├── entity/
│   │   │   ├── Product.java
│   │   │   ├── Device.java (修改，添加 product_id)
│   │   │   ├── DeviceProperty.java
│   │   │   ├── DeviceEvent.java
│   │   │   └── DeviceServiceInvoke.java
│   │   ├── mapper/
│   │   │   ├── ProductMapper.java
│   │   │   └── ...
│   │   ├── service/
│   │   │   ├── ProductService.java
│   │   │   ├── ThingModelService.java
│   │   │   ├── DeviceServiceInvokeService.java
│   │   │   └── ...
│   │   └── controller/
│   │       ├── ProductController.java
│   │       ├── ThingModelController.java
│   │       └── ...
│   └── pom.xml (添加 GraalJS, Aviator 依赖)
├── connect-service/            # 连接服务（已有，扩展解析和映射）
│   ├── src/main/java/com/openiot/connect/
│   │   ├── parser/
│   │   │   ├── ParseRuleEngine.java
│   │   │   └── impl/
│   │   │       ├── JsonPathParser.java
│   │   │       ├── JavaScriptParser.java
│   │   │       ├── RegexParser.java
│   │   │       └── BinaryParser.java
│   │   ├── mapper/
│   │   │   └── MappingRuleEngine.java
│   │   └── validator/
│   │       └── ThingModelValidator.java
│   └── pom.xml (添加 GraalJS 依赖)
├── data-service/               # 数据服务（已有，扩展 InfluxDB）
│   ├── src/main/java/com/openiot/data/
│   │   ├── influxdb/
│   │   │   └── InfluxDBService.java
│   │   └── controller/
│   │       └── HistoryDataController.java
│   └── pom.xml (添加 InfluxDB Java Client 依赖)
├── rule-service/               # 规则服务（新增）
│   ├── src/main/java/com/openiot/rule/
│   │   ├── forwarder/
│   │   │   ├── ForwarderManager.java
│   │   │   └── impl/
│   │   │       ├── KafkaForwarder.java
│   │   │       ├── HttpWebhookForwarder.java
│   │   │       ├── MqttForwarder.java
│   │   │       └── InfluxDbForwarder.java
│   │   ├── alarm/
│   │   │   ├── AlarmEngine.java
│   │   │   ├── AlarmNotifier.java
│   │   │   └── impl/
│   │   │       ├── EmailNotifier.java
│   │   │       ├── SmsNotifier.java
│   │   │       └── WebhookNotifier.java
│   │   ├── entity/
│   │   │   ├── ParseRule.java
│   │   │   ├── MappingRule.java
│   │   │   ├── ForwardRule.java
│   │   │   ├── AlarmRule.java
│   │   │   └── AlarmRecord.java
│   │   ├── mapper/
│   │   │   ├── ParseRuleMapper.java
│   │   │   ├── MappingRuleMapper.java
│   │   │   ├── ForwardRuleMapper.java
│   │   │   ├── AlarmRuleMapper.java
│   │   │   └── AlarmRecordMapper.java
│   │   ├── service/
│   │   │   ├── ParseRuleService.java
│   │   │   ├── MappingRuleService.java
│   │   │   ├── ForwardRuleService.java
│   │   │   └── AlarmRuleService.java
│   │   └── controller/
│   │       ├── ParseRuleController.java
│   │       ├── MappingRuleController.java
│   │       ├── ForwardRuleController.java
│   │       └── AlarmRuleController.java
│   └── pom.xml
└── pom.xml (添加 rule-service 模块)

frontend/
├── src/
│   ├── views/
│   │   ├── product/
│   │   │   ├── ProductList.vue
│   │   │   └── ProductDetail.vue
│   │   ├── rule/
│   │   │   ├── ParseRuleConfig.vue
│   │   │   ├── MappingRuleConfig.vue
│   │   │   └── ForwardRuleConfig.vue
│   │   ├── alarm/
│   │   │   ├── AlarmRuleConfig.vue
│   │   │   └── AlarmRecordList.vue
│   │   └── device/
│   │       └── HistoryDataQuery.vue
│   ├── components/
│   │   └── ThingModelEditor.vue
│   └── api/
│       ├── product.ts
│       ├── rule.ts
│       └── alarm.ts
└── package.json (添加 ECharts 依赖)

infrastructure/
├── docker-compose.yml (添加 InfluxDB 服务)
└── grafana/
    └── dashboards/ (添加 IoT 平台监控面板)
```

**Structure Decision**:
- 采用 **Option 2: Web application** 结构（前后端分离微服务架构）
- 后端：按业务域拆分为 6 个微服务（gateway, tenant, device, connect, data, rule）
- 前端：Vue 3 SPA，按功能模块组织（product, rule, alarm, device）
- 数据库：PostgreSQL（共享数据库）+ InfluxDB（时序数据）+ Redis（缓存）+ MongoDB（日志）

## Complexity Tracking

> **无宪法违规，无需记录**

---

## Phase 0: Research

### Research Tasks

1. **GraalJS 23.1.2 沙箱配置最佳实践**
   - 如何在 JDK 21 上集成 GraalJS（无需 GraalVM）
   - 如何配置沙箱（禁止文件系统、网络访问）
   - 如何设置脚本超时（3 秒）和大小限制（10KB）
   - 如何编译和缓存脚本以提升性能

2. **InfluxDB 2.x 与 Spring Boot 集成最佳实践**
   - InfluxDB Java Client 配置（连接池、重试策略）
   - 时序数据写入优化（批量写入、异步写入）
   - 90 天保留策略配置（Retention Policy）
   - 查询优化（降采样、连续查询）

3. **Aviator 5.4.1 表达式引擎使用指南**
   - Aviator 语法和函数库
   - 如何在 Spring Boot 中集成 Aviator
   - 如何自定义函数（如设备属性访问）
   - 表达式性能优化和缓存

4. **Redis pub/sub 实现配置热更新**
   - 如何使用 Redis pub/sub 广播配置变更
   - 如何处理消息丢失（客户端重连、消息持久化）
   - 如何保证 30 秒内生效（TTL + pub/sub）

5. **设备认证（DeviceKey + DeviceSecret）安全实践**
   - DeviceKey 和 DeviceSecret 的生成算法（UUID vs 自定义）
   - 如何安全存储 DeviceSecret（加密 vs Hash）
   - 如何防止重放攻击（Timestamp + Nonce）
   - 如何支持密钥轮换（重新生成 DeviceSecret）

### Research Output

See `research.md` for detailed findings.

---

## Phase 1: Design & Contracts

### Data Model

See `data-model.md` for detailed database schema design.

**Key Entities**:
- Product (产品)
- Device (设备，添加 product_id)
- ParseRule (解析规则)
- MappingRule (映射规则)
- ForwardRule (转发规则)
- AlarmRule (告警规则)
- AlarmRecord (告警记录)
- DeviceServiceInvoke (设备服务调用)
- DeviceProperty (设备属性，新增)
- DeviceEvent (设备事件，新增)

### Contracts

See `contracts/` directory for API contracts.

**API Endpoints** (RESTful, JSON):

**Product Management**:
- `POST /api/products` - 创建产品
- `GET /api/products/{id}` - 获取产品详情
- `PUT /api/products/{id}` - 更新产品
- `DELETE /api/products/{id}` - 删除产品
- `GET /api/products/{id}/devices` - 获取产品下的设备列表

**Thing Model**:
- `PUT /api/products/{id}/thing-model` - 更新物模型
- `GET /api/products/{id}/thing-model` - 获取物模型

**Parse Rules**:
- `POST /api/parse-rules` - 创建解析规则
- `GET /api/parse-rules/{id}` - 获取解析规则
- `PUT /api/parse-rules/{id}` - 更新解析规则
- `DELETE /api/parse-rules/{id}` - 删除解析规则
- `POST /api/parse-rules/{id}/test` - 测试解析规则

**Mapping Rules**:
- `POST /api/mapping-rules` - 创建映射规则
- `GET /api/mapping-rules/{id}` - 获取映射规则
- `PUT /api/mapping-rules/{id}` - 更新映射规则
- `DELETE /api/mapping-rules/{id}` - 删除映射规则
- `POST /api/mapping-rules/{id}/test` - 测试映射规则

**Forward Rules**:
- `POST /api/forward-rules` - 创建转发规则
- `GET /api/forward-rules/{id}` - 获取转发规则
- `PUT /api/forward-rules/{id}` - 更新转发规则
- `DELETE /api/forward-rules/{id}` - 删除转发规则
- `POST /api/forward-rules/{id}/test` - 测试转发规则

**Alarm Rules**:
- `POST /api/alarm-rules` - 创建告警规则
- `GET /api/alarm-rules/{id}` - 获取告警规则
- `PUT /api/alarm-rules/{id}` - 更新告警规则
- `DELETE /api/alarm-rules/{id}` - 删除告警规则
- `GET /api/alarm-records` - 查询告警记录
- `POST /api/alarm-records/{id}/acknowledge` - 确认告警

**Device Service Invocation**:
- `POST /api/devices/{id}/services/{serviceIdentifier}` - 调用设备服务
- `GET /api/service-invocations/{invocationId}` - 查询服务调用状态

**Historical Data**:
- `GET /api/devices/{id}/properties/history` - 查询设备属性历史
- `GET /api/devices/{id}/events/history` - 查询设备事件历史
- `GET /api/devices/{id}/data/export` - 导出设备数据

### Quickstart

See `quickstart.md` for development environment setup and local testing guide.

### Agent Context Update

Run `.specify/scripts/powershell/update-agent-context.ps1 -AgentType claude` to update agent context file with new technologies:
- GraalJS 23.1.2
- InfluxDB 2.x
- Aviator 5.4.1

---

## Constitution Re-Check (Post-Design)

After completing Phase 1 design, all constitution principles remain compliant:

- ✅ I. 多租户强制原则 - 所有实体包含 tenant_id
- ✅ II. 数据演进安全原则 - 使用 Flyway migration
- ✅ III. 渐进式微服务实践原则 - 新增 rule-service，边界清晰
- ✅ IV. 协议可扩展原则 - 解析规则与协议解耦
- ✅ V. 垂直数据链路优先原则 - 按 Phase 1-8 顺序实施
- ✅ VI. 学习范围强制原则 - 微服务治理、消息驱动、高并发、多租户全覆盖
- ✅ VII. 认证边界与职责划分原则 - Gateway 统一认证，设备认证独立
- ✅ VIII. 严格 RBAC 权限模型原则 - 新增产品相关权限
- ✅ IX. 可观测性强制原则 - 新增业务指标和告警

**总体评估**：✅ **设计符合所有宪法原则，可进入 Phase 2 任务分解**

---

## Next Steps

After completing this plan:

1. Run `/speckit.tasks` to generate detailed implementation tasks
2. Start Phase 1 implementation (Product Management Foundation)
3. Follow the 8-phase implementation plan sequentially
4. Verify each phase with acceptance scenarios before moving to next phase
