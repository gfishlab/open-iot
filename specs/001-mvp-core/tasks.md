# Tasks: Open-IoT MVP 核心功能

**Input**: Design documents from `/specs/001-mvp-core/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅, contracts/ ✅, research.md ✅

**Tests**: 本项目不强制 TDD，测试任务为可选。

**Organization**: 任务按用户故事分组，支持独立实现和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1, US2, US3, US4, US5, US6）
- 包含精确的文件路径

## Path Conventions

- **后端**: `backend/` (多模块 Maven 项目)
- **前端**: `frontend/` (Vue 3 + Vite)
- **基础设施**: `infrastructure/` (Docker 配置)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 项目初始化和基础结构

### 1.1 项目骨架

- [x] T001 创建项目根目录结构 (docs/, backend/, frontend/, infrastructure/, scripts/)
- [x] T002 创建后端父 POM 文件 backend/pom.xml (Spring Boot 3.x, Spring Cloud Alibaba 依赖管理)
- [x] T003 [P] 创建前端项目 frontend/package.json (Vue 3, Vite, Element Plus, Pinia)
- [x] T004 [P] 创建 .gitignore 文件 (Java, Node, IDE, 构建产物)

### 1.2 基础设施配置

- [x] T005 创建 Docker Compose 开发环境配置 infrastructure/docker/docker-compose.yml (Nacos, PostgreSQL, Redis, MongoDB, Kafka, EMQX)
- [x] T006 [P] 创建 EMQX 配置文件 infrastructure/emqx/emqx.conf
- [x] T007 [P] 创建 Kafka 配置文件 infrastructure/kafka/server.properties
- [x] T008 创建数据库迁移目录 infrastructure/sql/migrations/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 所有用户故事依赖的核心基础设施

**⚠️ CRITICAL**: 此阶段完成前，不能开始任何用户故事开发

### 2.1 公共模块

- [ ] T009 创建 common-core 模块 backend/common/common-core/pom.xml
- [ ] T010 [P] 创建 ApiResponse 统一响应类 backend/common/common-core/src/main/java/com/openiot/common/core/result/ApiResponse.java
- [ ] T011 [P] 创建全局异常处理器 backend/common/common-core/src/main/java/com/openiot/common/core/exception/GlobalExceptionHandler.java
- [ ] T012 [P] 创建业务异常类 backend/common/common-core/src/main/java/com/openiot/common/core/exception/BusinessException.java

### 2.2 Redis 配置

- [ ] T013 创建 common-redis 模块 backend/common/common-redis/pom.xml
- [ ] T014 创建 Redis 配置类 backend/common/common-redis/src/main/java/com/openiot/common/redis/config/RedisConfig.java
- [ ] T015 创建 Redis 工具类 backend/common/common-redis/src/main/java/com/openiot/common/redis/util/RedisUtil.java

### 2.3 Kafka 配置

- [ ] T016 创建 common-kafka 模块 backend/common/common-kafka/pom.xml
- [ ] T017 创建 EventEnvelope 事件模型 backend/common/common-kafka/src/main/java/com/openiot/common/kafka/model/EventEnvelope.java
- [ ] T018 创建 Kafka 配置类 backend/common/common-kafka/src/main/java/com/openiot/common/kafka/config/KafkaConfig.java

### 2.4 MongoDB 配置

- [ ] T019 创建 common-mongodb 模块 backend/common/common-mongodb/pom.xml
- [ ] T020 创建 RawEvent 文档模型 backend/common/common-mongodb/src/main/java/com/openiot/common/mongodb/document/RawEventDocument.java
- [ ] T021 创建 DeadLetter 文档模型 backend/common/common-mongodb/src/main/java/com/openiot/common/mongodb/document/DeadLetterDocument.java
- [ ] T022 创建 MongoDB 配置类 backend/common/common-mongodb/src/main/java/com/openiot/common/mongodb/config/MongoConfig.java

### 2.5 安全认证模块

- [ ] T023 创建 common-security 模块 backend/common/common-security/pom.xml
- [ ] T024 创建 Sa-Token 配置类 backend/common/common-security/src/main/java/com/openiot/common/security/config/SaTokenConfig.java
- [ ] T025 创建 StpInterface 实现类（权限校验） backend/common/common-security/src/main/java/com/openiot/common/security/service/StpInterfaceImpl.java
- [ ] T026 创建 TenantContext 租户上下文 backend/common/common-security/src/main/java/com/openiot/common/security/context/TenantContext.java

### 2.6 数据库 Schema

- [ ] T027 创建 Flyway 迁移脚本 V1.0.0__init_schema.sql (tenant, device, device_trajectory, sys_user 表)
- [ ] T028 [P] 创建 Flyway 迁移脚本 V1.0.1__init_data.sql (初始化平台管理员数据)

### 2.7 网关服务骨架

- [ ] T029 创建 gateway-service 模块 backend/gateway-service/pom.xml
- [ ] T030 创建网关启动类 backend/gateway-service/src/main/java/com/openiot/gateway/GatewayApplication.java
- [ ] T031 创建网关配置文件 backend/gateway-service/src/main/resources/application.yml (Nacos, 路由规则)
- [ ] T032 创建网关路由配置类 backend/gateway-service/src/main/java/com/openiot/gateway/config/RouteConfig.java
- [ ] T033 创建认证过滤器 backend/gateway-service/src/main/java/com/openiot/gateway/filter/AuthGlobalFilter.java
- [ ] T034 创建访问日志过滤器 backend/gateway-service/src/main/java/com/openiot/gateway/filter/AccessLogFilter.java
- [ ] T035 创建全局异常处理器 backend/gateway-service/src/main/java/com/openiot/gateway/handler/GlobalExceptionHandler.java

**Checkpoint**: ✅ 基础设施就绪 - 用户故事开发可以并行开始

---

## Phase 3: User Story 5 - 微服务治理与网关 (Priority: P1) 🎯 MVP

**Goal**: 实现注册中心、配置中心、API 网关三件套，形成完整治理闭环

**Independent Test**: 启动所有服务后，通过 Nacos 控制台查看服务注册状态，通过网关访问 API

### Implementation for User Story 5

- [ ] T036 [US5] 配置 Nacos 服务发现 backend/gateway-service/src/main/resources/bootstrap.yml
- [ ] T037 [US5] 创建健康检查端点 backend/gateway-service/src/main/java/com/openiot/gateway/controller/HealthController.java
- [ ] T038 [US5] 创建动态路由刷新接口 backend/gateway-service/src/main/java/com/openiot/gateway/controller/RouteController.java
- [ ] T039 [US5] 验证 Nacos 控制台服务注册状态
- [ ] T040 [US5] 验证网关路由转发功能

---

## Phase 4: User Story 4 - 多租户数据隔离 (Priority: P1)

**Goal**: 实现全链路租户隔离，所有核心业务表包含 tenant_id，查询默认隔离

**Independent Test**: 创建两个租户，验证跨租户访问被拒绝

### Implementation for User Story 4

#### 租户服务

- [ ] T041 [US4] 创建 tenant-service 模块 backend/tenant-service/pom.xml
- [ ] T042 [US4] 创建租户服务启动类 backend/tenant-service/src/main/java/com/openiot/tenant/TenantApplication.java
- [ ] T043 [US4] 创建租户服务配置 backend/tenant-service/src/main/resources/application.yml
- [ ] T044 [P] [US4] 创建 Tenant 实体类 backend/tenant-service/src/main/java/com/openiot/tenant/entity/Tenant.java
- [ ] T045 [P] [US4] 创建 SysUser 实体类 backend/tenant-service/src/main/java/com/openiot/tenant/entity/SysUser.java
- [ ] T046 [US4] 创建 TenantMapper backend/tenant-service/src/main/java/com/openiot/tenant/mapper/TenantMapper.java
- [ ] T047 [US4] 创建 SysUserMapper backend/tenant-service/src/main/java/com/openiot/tenant/mapper/SysUserMapper.java
- [ ] T048 [US4] 创建 TenantService backend/tenant-service/src/main/java/com/openiot/tenant/service/TenantService.java
- [ ] T049 [US4] 创建 AuthService backend/tenant-service/src/main/java/com/openiot/tenant/service/AuthService.java
- [ ] T050 [US4] 创建 TenantController backend/tenant-service/src/main/java/com/openiot/tenant/controller/TenantController.java
- [ ] T051 [US4] 创建 AuthController backend/tenant-service/src/main/java/com/openiot/tenant/controller/AuthController.java

#### 多租户拦截器

- [ ] T052 [US4] 创建 MyBatis Plus 租户拦截器 backend/common/common-security/src/main/java/com/openiot/common/security/interceptor/TenantLineInterceptor.java
- [ ] T053 [US4] 创建租户上下文过滤器 backend/common/common-security/src/main/java/com/openiot/common/security/filter/TenantContextFilter.java
- [ ] T054 [US4] 验证租户数据隔离功能

---

## Phase 5: User Story 1 - 多协议设备接入 (Priority: P1)

**Goal**: 实现 MQTT/TCP/HTTP 三种协议的设备接入，统一转换为 EventEnvelope

**Independent Test**: 模拟设备发送数据，验证平台成功接收并存入 Kafka

### Implementation for User Story 1

#### 设备服务

- [x] T055 [US1] 创建 device-service 模块 backend/device-service/pom.xml
- [x] T056 [US1] 创建设备服务启动类 backend/device-service/src/main/java/com/openiot/device/DeviceApplication.java
- [x] T057 [US1] 创建设备服务配置 backend/device-service/src/main/resources/application.yml
- [x] T058 [P] [US1] 创建 Device 实体类 backend/device-service/src/main/java/com/openiot/device/entity/Device.java
- [x] T059 [P] [US1] 创建 DeviceTrajectory 实体类 backend/device-service/src/main/java/com/openiot/device/entity/DeviceTrajectory.java
- [x] T060 [US1] 创建 DeviceMapper backend/device-service/src/main/java/com/openiot/device/mapper/DeviceMapper.java
- [x] T061 [US1] 创建 DeviceTrajectoryMapper backend/device-service/src/main/java/com/openiot/device/mapper/DeviceTrajectoryMapper.java
- [x] T062 [US1] 创建 DeviceService backend/device-service/src/main/java/com/openiot/device/service/DeviceService.java
- [x] T063 [US1] 创建 DeviceTokenService backend/device-service/src/main/java/com/openiot/device/service/DeviceTokenService.java
- [x] T064 [US1] 创建 DeviceController backend/device-service/src/main/java/com/openiot/device/controller/DeviceController.java
- [x] T065 [US1] 创建 DeviceDataController（HTTP 上报） backend/device-service/src/main/java/com/openiot/device/controller/DeviceDataController.java

#### MQTT 接入（EMQX Rule Engine）

- [x] T066 [US1] 配置 EMQX Rule Engine 转发到 Kafka infrastructure/emqx/rules/kafka_rule.sql
- [x] T067 [US1] 创建 MQTT 设备认证 Webhook backend/device-service/src/main/java/com/openiot/device/controller/MqttAuthController.java

#### TCP 接入（Netty）

- [x] T068 [US1] 创建 connect-service 模块 backend/connect-service/pom.xml
- [x] T069 [US1] 创建接入服务启动类 backend/connect-service/src/main/java/com/openiot/connect/ConnectApplication.java
- [x] T070 [US1] 创建接入服务配置 backend/connect-service/src/main/resources/application.yml
- [x] T071 [US1] 创建 Netty Server backend/connect-service/src/main/java/com/openiot/connect/netty/NettyServer.java
- [x] T072 [US1] 创建 Netty Server 配置 backend/connect-service/src/main/java/com/openiot/connect/netty/NettyServerConfig.java
- [x] T073 [US1] 创建 TCP 消息处理器 backend/connect-service/src/main/java/com/openiot/connect/netty/TcpMessageHandler.java
- [x] T074 [US1] 创建协议适配器接口 backend/connect-service/src/main/java/com/openiot/connect/protocol/ProtocolAdapter.java
- [x] T075 [US1] 创建私有协议解析器 backend/connect-service/src/main/java/com/openiot/connect/protocol/PrivateProtocolParser.java
- [x] T076 [US1] 创建设备认证服务 backend/connect-service/src/main/java/com/openiot/connect/auth/DeviceAuthService.java
- [x] T077 [US1] 创建 Kafka 消息生产者 backend/connect-service/src/main/java/com/openiot/connect/producer/EventProducer.java

---

## Phase 6: User Story 2 - 实时数据展示 (Priority: P1)

**Goal**: 前端实时展示设备在线状态和轨迹数据，P95 延迟 <= 3 秒

**Independent Test**: 设备上报数据后，前端界面 5 秒内可见更新

### Implementation for User Story 2

#### 数据服务（实时消费者）

- [x] T078 [US2] 创建 data-service 模块 backend/data-service/pom.xml
- [x] T079 [US2] 创建数据服务启动类 backend/data-service/src/main/java/com/openiot/data/DataApplication.java
- [x] T080 [US2] 创建数据服务配置 backend/data-service/src/main/resources/application.yml
- [x] T081 [US2] 创建实时消费者 backend/data-service/src/main/java/com/openiot/data/consumer/RealtimeConsumer.java
- [x] T082 [US2] 创建轨迹服务 backend/data-service/src/main/java/com/openiot/data/service/TrajectoryService.java
- [x] T083 [US2] 创建设备状态服务 backend/data-service/src/main/java/com/openiot/data/service/DeviceStatusService.java
- [x] T084 [US2] 创建 SSE 控制器 backend/data-service/src/main/java/com/openiot/data/controller/SseController.java

#### 设备服务（状态查询 API）

- [x] T085 [US2] 创建设备状态查询接口 backend/device-service/src/main/java/com/openiot/device/controller/DeviceStatusController.java
- [x] T086 [US2] 创建轨迹查询接口 backend/device-service/src/main/java/com/openiot/device/controller/TrajectoryController.java

#### 前端实时监控页面

- [x] T087 [US2] 创建前端项目配置 frontend/vite.config.ts
- [x] T088 [US2] 创建前端入口文件 frontend/src/main.ts
- [x] T089 [P] [US2] 创建 API 请求工具 frontend/src/utils/request.ts
- [x] T090 [P] [US2] 创建 SSE 工具 frontend/src/utils/sse.ts
- [x] T091 [US2] 创建 Pinia Store frontend/src/stores/device.ts
- [x] T092 [US2] 创建路由配置 frontend/src/router/index.ts
- [x] T093 [US2] 创建布局组件 frontend/src/layouts/MainLayout.vue
- [x] T094 [US2] 创建设备监控页面 frontend/src/views/monitor/DeviceMonitor.vue
- [x] T095 [US2] 创建轨迹展示组件 frontend/src/components/TrajectoryMap.vue
- [x] T096 [US2] 创建设备状态组件 frontend/src/components/DeviceStatus.vue

---

## Phase 7: User Story 3 - 异步数据处理与存储 (Priority: P2)

**Goal**: 实现原始数据持久化、解析任务、死信队列、历史重放

**Independent Test**: 验证 MongoDB 原始数据和 PostgreSQL 解析结果

### Implementation for User Story 3

#### 异步消费者

- [x] T097 [US3] 创建异步消费者 backend/data-service/src/main/java/com/openiot/data/consumer/AsyncConsumer.java
- [x] T098 [US3] 创建原始事件服务 backend/data-service/src/main/java/com/openiot/data/service/RawEventService.java
- [x] T099 [US3] 创建 MongoDB RawEvent Repository backend/data-service/src/main/java/com/openiot/data/repository/RawEventRepository.java

#### 解析任务

- [x] T100 [US3] 创建解析任务调度器 backend/data-service/src/main/java/com/openiot/data/scheduler/ParseScheduler.java
- [x] T101 [US3] 创建轨迹解析器 backend/data-service/src/main/java/com/openiot/data/parser/TrajectoryParser.java
- [x] T102 [US3] 创建解析结果服务（TrajectoryParser 已包含此功能）

#### 死信队列

- [x] T103 [US3] 创建死信消费者 backend/data-service/src/main/java/com/openiot/data/consumer/DlqConsumer.java
- [x] T104 [US3] 创建死信服务 backend/data-service/src/main/java/com/openiot/data/service/DeadLetterService.java
- [x] T105 [US3] 创建死信 Repository backend/data-service/src/main/java/com/openiot/data/repository/DeadLetterRepository.java
- [x] T106 [US3] 创建重试服务 backend/data-service/src/main/java/com/openiot/data/service/RetryService.java

#### 历史重放

- [x] T107 [US3] 创建重放服务 backend/data-service/src/main/java/com/openiot/data/replay/ReplayService.java
- [x] T108 [US3] 创建重放控制器 backend/data-service/src/main/java/com/openiot/data/controller/ReplayController.java

---

## Phase 8: User Story 6 - 平台与租户管理 (Priority: P3)

**Goal**: 实现租户 CRUD、用户认证、权限校验

**Independent Test**: 创建、查看、禁用租户功能验证

### Implementation for User Story 6

#### 租户管理完善

- [x] T109 [US6] 完善租户 CRUD 接口 backend/tenant-service/src/main/java/com/openiot/tenant/controller/TenantController.java
- [x] T110 [US6] 创建用户管理服务 backend/tenant-service/src/main/java/com/openiot/tenant/service/UserService.java
- [x] T111 [US6] 创建用户管理接口 backend/tenant-service/src/main/java/com/openiot/tenant/controller/UserController.java
- [x] T112 [US6] 创建权限校验服务 backend/tenant-service/src/main/java/com/openiot/tenant/service/PermissionService.java

#### 前端管理页面

- [x] T113 [P] [US6] 创建登录页面 frontend/src/views/auth/Login.vue
- [x] T114 [P] [US6] 创建租户管理页面 frontend/src/views/tenant/TenantList.vue
- [x] T115 [P] [US6] 创建设备管理页面 frontend/src/views/device/DeviceList.vue
- [x] T116 [US6] 创建用户 Store frontend/src/stores/user.ts
- [x] T117 [US6] 创建权限指令 frontend/src/directives/permission.ts

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 跨故事的改进和优化

### 文档与脚本

- [ ] T118 [P] 创建构建脚本 scripts/build.sh
- [ ] T119 [P] 创建部署脚本 scripts/deploy.sh
- [ ] T120 创建 README.md 项目说明文档

### 性能优化

- [ ] T121 优化 Netty 线程模型 backend/connect-service/src/main/java/com/openiot/connect/netty/NettyServerConfig.java
- [ ] T122 优化 Kafka 消费者配置 backend/data-service/src/main/resources/application.yml
- [ ] T123 优化 Redis 连接池配置 backend/common/common-redis/src/main/resources/redis.properties

### 可观测性

- [ ] T124 [P] 添加服务健康检查 backend/*/src/main/java/com/openiot/*/config/HealthConfig.java
- [ ] T125 [P] 添加链路追踪配置 backend/common/common-core/src/main/java/com/openiot/common/core/config/TraceConfig.java
- [ ] T126 创建监控指标收集 backend/data-service/src/main/java/com/openiot/data/metrics/DeviceMetrics.java

### 验证

- [ ] T127 运行 quickstart.md 验证端到端流程
- [ ] T128 验证多租户隔离功能
- [ ] T129 验证实时轨迹延迟 <= 3 秒
- [ ] T130 验证 1000 并发设备连接

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖 - 立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 - **阻塞所有用户故事**
- **Phase 3-8 (User Stories)**: 依赖 Phase 2 完成
  - US5 (治理) 和 US4 (多租户) 优先级最高，其他故事依赖它们
  - US1 (设备接入) 依赖 US4 的多租户基础设施
  - US2 (实时展示) 依赖 US1 的数据流
  - US3 (异步处理) 可与 US2 并行
  - US6 (管理) 相对独立
- **Phase 9 (Polish)**: 依赖所需用户故事完成

### User Story Dependencies

```
Phase 2 (Foundational)
         │
    ┌────┴────┐
    │         │
   US5       US4
 (治理)    (多租户)
    │         │
    └────┬────┘
         │
        US1
     (设备接入)
         │
    ┌────┴────┐
    │         │
   US2       US3
 (实时展示) (异步处理)
         │
        US6
      (管理)
```

### Within Each User Story

1. 实体/模型优先
2. Mapper/Repository
3. Service 业务逻辑
4. Controller API 层
5. 前端组件
6. 集成验证

### Parallel Opportunities

- Phase 1: T002, T003, T004 可并行
- Phase 2: T010-T012 可并行；T013-T015 可并行；T016-T018 可并行
- Phase 5: T058, T059 可并行
- Phase 6: T089, T090 可并行
- Phase 7: T113-T115 可并行
- Phase 9: T118, T119, T124, T125 可并行

---

## Parallel Example: User Story 1

```bash
# 并行创建实体类
Task: "T058 [P] [US1] 创建 Device 实体类 backend/device-service/src/main/java/com/openiot/device/entity/Device.java"
Task: "T059 [P] [US1] 创建 DeviceTrajectory 实体类 backend/device-service/src/main/java/com/openiot/device/entity/DeviceTrajectory.java"

# 并行创建 Mapper
Task: "T060 [US1] 创建 DeviceMapper backend/device-service/src/main/java/com/openiot/device/mapper/DeviceMapper.java"
Task: "T061 [US1] 创建 DeviceTrajectoryMapper backend/device-service/src/main/java/com/openiot/device/mapper/DeviceTrajectoryMapper.java"
```

---

## Implementation Strategy

### MVP First (Phase 1-3 + US1 + US2)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational (阻塞点)
3. 完成 Phase 3: US5 (微服务治理)
4. 完成 Phase 4: US4 (多租户)
5. 完成 Phase 5: US1 (设备接入) - **MVP 核心链路**
6. 完成 Phase 6: US2 (实时展示)
7. **STOP and VALIDATE**: 端到端演示 MQTT 设备上报 → 前端轨迹展示

### Incremental Delivery

1. Setup + Foundational → 基础设施就绪
2. + US5 + US4 → 治理和租户就绪
3. + US1 → 设备接入就绪
4. + US2 → **MVP 交付** (可演示)
5. + US3 → 异步处理增强
6. + US6 → 管理功能完善
7. + Phase 9 → 优化打磨

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
| **总任务数** | 130 |
| **Phase 1 (Setup)** | 8 |
| **Phase 2 (Foundational)** | 27 |
| **Phase 3 (US5 治理)** | 5 |
| **Phase 4 (US4 多租户)** | 14 |
| **Phase 5 (US1 设备接入)** | 23 |
| **Phase 6 (US2 实时展示)** | 10 |
| **Phase 7 (US3 异步处理)** | 12 |
| **Phase 8 (US6 租户管理)** | 9 |
| **Phase 9 (Polish)** | 13 |
| **并行机会** | ~30 个任务可并行 |

**MVP 范围**: Phase 1-6，约 87 个任务
