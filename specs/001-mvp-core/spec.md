# Feature Specification: Open-IoT MVP 核心功能

**Feature Branch**: `001-mvp-core`
**Created**: 2026-02-25
**Status**: Draft
**Input**: 基于 constitution 产出 MVP 阶段规范文档，关注"做什么、为什么、验收标准"

---

## 背景与目标

本项目为个人学习型开源项目，MVP 目标是在单环境中跑通可演示的端到端链路，同时覆盖以下核心能力：

1. **微服务治理**：注册中心、配置中心、API 网关
2. **Kafka 消息驱动架构**
3. **Netty 高并发接入能力**
4. **多租户数据库设计**

支持两类业务场景：
- **高实时场景**：如无人驾驶农机轨迹展示（秒级更新）
- **非实时场景**：原始数据归档与延迟解析（分钟级）

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 多协议设备接入 (Priority: P1)

作为设备/设备云，我需要通过 MQTT/TCP/HTTP 协议上报遥测数据，以便平台能够接收并处理我的设备数据。

**Why this priority**: 设备接入是 IoT 平台的核心入口，没有数据接入就没有后续所有功能。

**Independent Test**: 可通过模拟设备发送数据，验证平台是否成功接收并存入消息队列。

**Acceptance Scenarios**:

1. **Given** 一个已注册的 MQTT 设备，**When** 设备通过 MQTT Broker 上报遥测数据，**Then** 数据被成功接收并转换为 EventEnvelope 存入 Kafka 对应 Topic
2. **Given** 一个已注册的 TCP 私有协议设备，**When** 设备通过 Netty 建立连接并上报数据，**Then** 数据被成功解析、转换并存入 Kafka
3. **Given** 一个第三方设备云，**When** 通过 HTTP 接口推送设备数据，**Then** 数据被统一事件模型接收并存入 Kafka

---

### User Story 2 - 实时数据展示 (Priority: P1)

作为租户管理员，我需要在前端实时查看设备在线状态和最新上报数据，以便监控设备运行情况。

**Why this priority**: 实时展示是验证端到端链路的关键，也是用户最基础的使用场景。

**Independent Test**: 可通过设备上报数据后，在前端界面验证数据是否在预期时间内显示。

**Acceptance Scenarios**:

1. **Given** 设备已上线并持续上报数据，**When** 租户管理员打开设备监控页面，**Then** 可看到设备在线状态（在线/离线）
2. **Given** 设备上报轨迹数据，**When** 数据通过实时通道处理，**Then** 前端轨迹在 P95 延迟 <= 3 秒内更新
3. **Given** 设备上报状态数据，**When** 数据进入实时消费链路，**Then** 前端展示最新状态值和更新时间

---

### User Story 3 - 异步数据处理与存储 (Priority: P2)

作为平台，我需要将原始数据持久化后再进行解析，以支持历史重放和故障恢复。

**Why this priority**: 异步通道保证数据可靠性和可追溯性，是系统健壮性的保障。

**Independent Test**: 可通过模拟数据上报后，验证 MongoDB 中原始数据和 PostgreSQL 中解析结果。

**Acceptance Scenarios**:

1. **Given** 设备上报原始数据，**When** 数据进入异步通道，**Then** 原始数据先持久化到 MongoDB（raw events 集合）
2. **Given** 原始数据已落 MongoDB，**When** 定时解析任务执行，**Then** 解析结果写入 PostgreSQL 结构化表
3. **Given** 解析任务执行失败，**When** 达到重试次数上限，**Then** 消息进入死信队列并记录失败原因
4. **Given** 死信队列中存在失败消息，**When** 手动触发重试，**Then** 消息被重新处理并成功写入 PostgreSQL

---

### User Story 4 - 多租户数据隔离 (Priority: P1)

作为平台管理员，我需要确保不同租户的数据完全隔离，以保障数据安全和商业边界。

**Why this priority**: 多租户是 SaaS 平台的基础能力，必须从一开始就内置。

**Independent Test**: 可通过创建两个租户，验证跨租户访问被拒绝。

**Acceptance Scenarios**:

1. **Given** 租户 A 的管理员已登录，**When** 查询设备列表，**Then** 仅返回租户 A 的设备
2. **Given** 租户 A 尝试访问租户 B 的设备 ID，**When** 发起查询请求，**Then** 请求被拒绝并记录安全日志
3. **Given** 设备上报数据，**When** 数据进入处理链路，**Then** 全链路携带 tenant_id 且查询默认隔离

---

### User Story 5 - 微服务治理与网关 (Priority: P1)

作为运维人员，我需要通过统一入口访问所有服务，并能在配置中心集中管理配置。

**Why this priority**: 微服务治理是学习目标之一，也是生产可运维的基础。

**Independent Test**: 可通过网关访问各服务接口，验证路由和配置管理功能。

**Acceptance Scenarios**:

1. **Given** 多个微服务已启动并注册，**When** 查看注册中心，**Then** 所有服务实例可见且状态健康
2. **Given** 外部请求访问任意 API，**When** 请求到达网关，**Then** 网关正确路由到目标服务并记录访问日志
3. **Given** 配置中心存在配置项，**When** 修改配置并发布，**Then** 相关服务动态加载新配置（无需重启）
4. **Given** 服务实例健康检查端点，**When** 发起健康检查请求，**Then** 返回服务状态（UP/DOWN）及关键依赖状态

---

### User Story 6 - 平台与租户管理 (Priority: P3)

作为平台管理员，我需要管理租户账户，以便为不同客户提供隔离的服务。

**Why this priority**: 管理功能相对独立，可在核心链路跑通后完善。

**Independent Test**: 可通过创建、查看、禁用租户，验证管理功能。

**Acceptance Scenarios**:

1. **Given** 平台管理员已登录，**When** 创建新租户，**Then** 租户信息持久化且租户可正常登录
2. **Given** 租户已存在，**When** 租户管理员登录，**Then** 可查看本租户设备和数据
3. **Given** 租户被禁用，**When** 租户用户尝试登录，**Then** 登录被拒绝并提示账户已禁用

---

### Edge Cases

- **设备离线重连**：设备网络波动断开后重连，系统能正确恢复连接状态
- **消息积压**：Kafka 消费延迟时，系统有监控告警且不丢失数据
- **解析异常**：原始数据格式异常时，进入死信队列而非阻塞正常流程
- **并发写入**：多设备同时上报时，数据不丢失、不混乱
- **租户配额**：单个租户设备数超限时，有明确提示（MVP 可仅记录日志）
- **网关超时**：后端服务响应慢时，网关返回明确错误而非无限等待

---

## Requirements *(mandatory)*

### Functional Requirements

#### 接入层

- **FR-001**: 系统 MUST 接收 MQTT 协议的设备数据上报（通过 EMQX Broker）
- **FR-002**: 系统 MUST 接收 TCP/私有协议的设备数据上报（通过 Netty）
- **FR-003**: 系统 MUST 接收 HTTP 协议的设备云数据推送
- **FR-004**: 系统 MUST 将不同协议数据统一映射为 EventEnvelope 格式
- **FR-005**: 系统 MUST 为每个设备分配唯一标识并使用 Token 验证设备身份

#### 实时通道

- **FR-006**: 系统 MUST 将关键状态数据在秒级传递至前端（通过简化 Kafka 消费者）
- **FR-007**: 系统 MUST 支持轨迹数据的实时推送展示（实时数据存 Redis）
- **FR-008**: 系统 MUST 保证实时轨迹端到端延迟 P95 <= 3 秒

#### 异步通道

- **FR-009**: 系统 MUST 先持久化原始数据到 MongoDB（raw events）
- **FR-010**: 系统 MUST 执行定时规则解析任务
- **FR-011**: 系统 MUST 将解析结果写入 PostgreSQL 结构化表（含历史轨迹）
- **FR-012**: 系统 MUST 支持解析失败的消息重试
- **FR-013**: 系统 MUST 将重试失败的消息放入死信队列
- **FR-014**: 系统 MUST 支持基于历史原始数据的重放解析（按时间窗口 + 手动触发）

#### 多租户

- **FR-015**: 系统 MUST 在所有核心业务表和事件中包含 tenant_id
- **FR-016**: 系统 MUST 默认为所有查询添加租户过滤条件
- **FR-017**: 系统 MUST 拒绝非授权的跨租户访问并记录日志
- **FR-018**: 系统 MUST 在全链路（API → 服务 → 数据库）保持租户上下文

#### 治理与网关

- **FR-019**: 系统 MUST 通过注册中心管理所有服务实例
- **FR-020**: 系统 MUST 通过配置中心集中管理服务配置
- **FR-021**: 系统 MUST 通过 API 网关统一所有外部访问入口
- **FR-022**: 系统 MUST 对所有外部 API 请求进行鉴权
- **FR-023**: 系统 MUST 记录网关访问日志（含 tenant_id、trace_id）

#### 前端展示

- **FR-024**: 前端 MUST 展示设备在线状态（在线/离线）
- **FR-025**: 前端 MUST 展示设备最近上报数据
- **FR-026**: 前端 MUST 展示实时轨迹（基础轨迹线）

### Non-Functional Requirements

#### 性能（MVP 基线）

- **NFR-001**: 系统 MUST 支持 >= 1000 并发设备连接（MQTT + TCP 总量）
- **NFR-002**: 系统 MUST 支持 >= 2000 条/秒消息写入 Kafka（短时峰值）
- **NFR-003**: 系统 MUST 保证实时轨迹端到端延迟 P95 <= 3 秒

#### 可靠性

- **NFR-004**: 系统 MUST 对消息处理链路提供可观测的失败记录
- **NFR-005**: 系统 MUST 对消费失败提供重试与死信机制

#### 安全性

- **NFR-006**: 系统 MUST 对所有 API 请求进行身份认证
- **NFR-007**: 系统 MUST 确保租户隔离不可绕过

#### 可观测性

- **NFR-008**: 系统 MUST 提供服务健康检查端点
- **NFR-009**: 系统 MUST 在关键链路日志中包含 tenant_id、device_id、trace_id

---

### Key Entities

- **Tenant（租户）**: 表示一个独立的客户/组织，拥有独立的设备和数据空间
- **Device（设备）**: 表示一个 IoT 设备，归属特定租户，具有唯一标识和协议类型
- **EventEnvelope（统一事件）**: 协议无关的事件模型，包含 tenant_id、device_id、事件类型、时间戳、原始载荷
- **RawEvent（原始事件）**: 存储在 MongoDB 的原始上报数据，用于重放和审计
- **ParsedData（解析数据）**: 存储在 PostgreSQL 的结构化数据，支持业务查询
- **DeviceStatus（设备状态）**: 设备实时状态，包含在线/离线、最后上报时间等
- **DeviceTrajectory（设备轨迹）**: 设备位置轨迹数据，用于实时展示

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: MQTT 设备上报数据在 5 秒内进入 Kafka Topic
- **SC-002**: TCP 设备通过 Netty 接入并在 5 秒内完成数据上报
- **SC-003**: HTTP 云设备上报在 3 秒内返回成功响应
- **SC-004**: 实时轨迹在前端可见且 P95 延迟 <= 3 秒
- **SC-005**: 原始数据在 10 秒内落 MongoDB
- **SC-006**: 定时解析任务成功处理数据并写入 PostgreSQL
- **SC-007**: 解析失败消息进入死信队列并可手动重试恢复
- **SC-008**: 租户 A 无法查询到租户 B 的任何数据
- **SC-009**: 网关统一入口可路由到所有服务并记录访问日志
- **SC-010**: 系统支持 1000 并发设备连接稳定运行 10 分钟
- **SC-011**: 系统支持 2000 条/秒消息写入 Kafka 短时峰值

---

## 技术决策（已确认）

以下技术选型已在澄清阶段确认：

| 决策项 | 确认方案 | 说明 |
|--------|---------|------|
| MQTT Broker | EMQX | 开源、功能全、支持大规模连接 |
| 实时处理 | 简化消费者 | MVP 阶段采用简化 Kafka 消费者，避免 Flink 复杂度 |
| 轨迹存储 | Redis + PostgreSQL | Redis 存实时热数据，PostgreSQL 存历史查询数据 |
| 设备鉴权 | Token | 基于令牌的设备认证，后续可升级为证书 |
| 历史重放 | 按时间窗口 + 手动触发 | 支持指定时间范围重放，或手动触发单条重试 |

---

## Out of Scope

以下内容不在 MVP 阶段范围内：

1. **多集群容灾与跨地域部署**
2. **复杂规则引擎可视化编排**
3. **计费结算系统**
4. **高级权限模型**（先实现最小 RBAC）
5. **大规模压测到生产级别容量**
6. **设备固件 OTA 升级**
7. **告警通知（短信/邮件/推送）**
8. **数据导出与报表**

---

## 验收场景（Gherkin 格式）

### 场景 1: MQTT 设备上报成功并入 Kafka

```gherkin
Feature: MQTT 设备数据上报

  Scenario: MQTT 设备上报遥测数据成功
    Given 一个已注册的 MQTT 设备 "device-001" 属于租户 "tenant-A"
    And 设备已连接到 MQTT Broker
    When 设备发送遥测数据到主题 "device/telemetry"
    Then 平台在 5 秒内收到数据
    And 数据被转换为 EventEnvelope 格式
    And EventEnvelope 存入 Kafka Topic "device-events"
    And EventEnvelope 包含正确的 tenant_id "tenant-A"
```

### 场景 2: TCP 设备通过 Netty 接入并上报成功

```gherkin
Feature: TCP 私有协议设备接入

  Scenario: TCP 设备通过 Netty 上报数据成功
    Given 一个已注册的 TCP 设备 "device-002" 属于租户 "tenant-A"
    And 设备鉴权密钥为 "secret-key-123"
    When 设备通过 Netty 建立连接并发送鉴权请求
    Then 连接建立成功
    When 设备发送私有协议格式的遥测数据
    Then 数据被正确解析
    And 数据被转换为 EventEnvelope 格式
    And EventEnvelope 存入 Kafka Topic "device-events"
```

### 场景 3: HTTP 云设备上报被统一事件模型接收

```gherkin
Feature: HTTP 云设备数据上报

  Scenario: HTTP 云设备上报数据成功
    Given 一个已注册的设备云 "cloud-001" 属于租户 "tenant-B"
    And 设备云拥有有效的访问令牌
    When 设备云通过 POST /api/v1/devices/data 上报数据
    Then 响应状态码为 200
    And 响应在 3 秒内返回
    And 数据被转换为 EventEnvelope 格式
    And EventEnvelope 存入 Kafka Topic "device-events"
```

### 场景 4: 实时轨迹在前端可见且延迟满足目标

```gherkin
Feature: 实时轨迹展示

  Scenario: 设备轨迹实时更新
    Given 设备 "tractor-001" 正在运行并持续上报位置数据
    And 租户管理员已打开设备监控页面
    When 设备上报新的位置坐标 (120.5, 30.2)
    Then 前端轨迹在 P95 延迟 <= 3 秒内更新
    And 轨迹点正确显示在地图上
    And 显示更新时间戳
```

### 场景 5: 原始数据落 MongoDB 且定时解析后入 PostgreSQL

```gherkin
Feature: 异步数据处理

  Scenario: 原始数据持久化与解析
    Given 设备 "device-001" 上报原始遥测数据
    When 数据进入异步处理通道
    Then 原始数据在 10 秒内写入 MongoDB "raw_events" 集合
    And 原始数据包含 tenant_id、device_id、timestamp、raw_payload
    When 定时解析任务执行
    Then 解析任务读取原始数据并执行解析规则
    And 解析结果写入 PostgreSQL 结构化表
    And 解析结果关联正确的 tenant_id
```

### 场景 6: 解析失败进入死信并可重试恢复

```gherkin
Feature: 解析失败处理

  Scenario: 解析失败进入死信队列
    Given 设备 "device-003" 上报格式异常的原始数据
    When 解析任务尝试解析数据
    And 解析连续失败达到重试上限（3 次）
    Then 消息进入死信队列 "dlq-parse-failed"
    And 死信记录包含原始数据、失败原因、失败时间

  Scenario: 死信消息重试恢复
    Given 死信队列中存在一条失败消息
    When 运维人员修复解析规则后触发重试
    Then 消息被重新处理
    And 解析成功后写入 PostgreSQL
    And 消息从死信队列移除
```

### 场景 7: 租户 A 无法访问租户 B 数据

```gherkin
Feature: 多租户数据隔离

  Scenario: 租户数据查询隔离
    Given 租户 "tenant-A" 的管理员已登录
    And 租户 "tenant-B" 存在设备 "device-B-001"
    When 管理员尝试查询设备 "device-B-001" 的数据
    Then 请求被拒绝
    And 返回 403 Forbidden 错误
    And 安全日志记录此次越权访问尝试
    And 日志包含 tenant_id、user_id、target_device_id

  Scenario: 租户设备列表隔离
    Given 租户 "tenant-A" 有 5 台设备
    And 租户 "tenant-B" 有 3 台设备
    When 租户 "tenant-A" 管理员查询设备列表
    Then 仅返回租户 "tenant-A" 的 5 台设备
    And 不包含任何租户 "tenant-B" 的设备
```

### 场景 8: 网关统一入口可路由并记录访问

```gherkin
Feature: API 网关统一入口

  Scenario: 网关路由到设备服务
    Given 设备服务已启动并注册到注册中心
    And 网关已配置设备服务的路由规则
    When 外部请求 GET /api/v1/devices 通过网关访问
    Then 请求被路由到设备服务
    And 响应正确返回
    And 网关记录访问日志
    And 日志包含 tenant_id、trace_id、请求路径、响应状态、耗时

  Scenario: 未认证请求被网关拒绝
    Given 网关已启用鉴权
    When 外部请求不带认证令牌访问 /api/v1/devices
    Then 返回 401 Unauthorized
    And 响应包含错误信息 "未授权访问"
```

---

## Assumptions

1. **单环境部署**：MVP 阶段在单一环境中运行，不涉及多环境切换
2. **最小 RBAC**：权限模型仅包含平台管理员和租户管理员两个角色
3. **设备注册**：设备需预先在平台注册，暂不支持设备自主注册
4. **网络稳定**：假设设备与平台间网络基本稳定，短暂断线可重连
5. **数据量级**：MVP 阶段数据量可控，不需要分库分表

---

## Clarifications

### Session 2026-02-25

- Q: MQTT Broker 选型与部署方式？ → A: EMQX
- Q: 实时处理方案选型？ → A: 简化消费者（先不用 Flink）
- Q: 轨迹数据存储策略？ → A: Redis（实时）+ PostgreSQL（查询）
- Q: 设备鉴权策略？ → A: Token（后续再升级证书）
- Q: 历史重放的成本与窗口策略？ → A: 按时间窗口 + 手动触发
