<!--
=============================================================================
SYNC IMPACT REPORT
=============================================================================
Version change: 1.6.0 → 1.7.0 (MINOR - 新增数据字典强制原则)
Modified principles:
  - None
Added sections:
  - X. 数据字典优先原则 (Data Dictionary First)
Removed sections: None
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ (no changes needed - generic)
  - .specify/templates/spec-template.md ⚠ (建议添加数据字典需求检查项)
  - .specify/templates/tasks-template.md ⚠ (建议添加字典数据初始化任务类型)
Follow-up TODOs: None
=============================================================================
-->

# OpenIoT 开放物联网平台 Constitution

## Core Principles

### I. 多租户强制原则 (Multi-Tenant Enforcement)

SaaS 模式下所有核心业务表 MUST 包含租户隔离能力。

- **租户字段**: 所有核心业务表必须包含 `tenant_id` 字段
- **查询隔离**: 所有查询必须默认添加租户过滤条件
- **跨租户禁止**: 严格禁止任何形式的跨租户数据访问
- **租户上下文**: 通过上下文自动注入租户信息，避免手动传参

**理由**: IoT 平台面向多客户提供服务，租户隔离是数据安全和商业模式的基石。

### II. 数据演进安全原则 (Safe Data Evolution)

数据库变更 MUST 可追溯、可回滚、可审计。

- **Migration 强制**: 所有数据库变更必须使用 Flyway/Liquibase 等 migration 工具
- **生产禁止手动**: 严格禁止直接修改生产环境数据库
- **非破坏性修改**: 禁止破坏性 schema 修改（如直接删除列），必须分阶段演进
- **版本记录**: 每次 migration 必须有清晰的版本号和变更说明

**理由**: IoT 系统数据量大、业务连续性要求高，数据库变更必须可控可回滚。

### III. 渐进式微服务实践原则 (Progressive Microservice Practice)

架构演进 MUST 兼顾学习目标与工程复杂度，按节奏落地微服务治理能力。

- **治理能力必建**: 必须落地注册中心、配置中心、API 网关，形成完整治理闭环
- **服务拆分适度**: 按业务域拆分最小必要服务，避免无意义细粒度拆分
- **边界先于数量**: 服务数量不是目标，清晰边界、独立部署与可观测性才是目标
- **演进路径**: 先跑通核心链路，再根据瓶颈逐步扩展服务数量与治理深度

**理由**: 该项目以学习微服务治理为明确目标，需要“可运行的微服务实践”而非纯理论或过度拆分。

### IV. 协议可扩展原则 (Protocol Extensibility)

设备通信层 MUST 支持多协议插件化扩展。

- **协议层插件化**: 协议处理层必须设计为可插拔架构
- **模型协议解耦**: 设备数据模型与通信协议严格解耦
- **禁止写死协议**: 不允许在业务代码中写死特定协议（如 MQTT），必须通过协议适配器
- **统一抽象**: 对上层业务提供统一的设备接入抽象接口

**理由**: IoT 设备协议多样（MQTT、CoAP、HTTP、TCP 私有协议等），协议层必须具备扩展能力。

### V. 垂直数据链路优先原则 (Vertical Data Pipeline First)

开发优先级 MUST 以端到端数据流通为目标。

- **完整数据流**: 优先打通「设备接入 → 数据处理 → 存储 → 展示」完整链路
- **禁止 CRUD 优先**: 严格禁止先做管理后台 CRUD，后做核心数据链路
- **阶段闭环**: 每个开发阶段必须形成可演示的端到端闭环
- **增量价值**: 每个迭代必须交付可验证的业务价值

**理由**: IoT 平台的核心价值在于数据流通，管理后台是辅助而非核心，垂直打通才能验证架构正确性。

### VI. 学习范围强制原则 (Learning Scope Mandate)

本项目作为个人学习型开源项目，以下能力 MUST 在架构与实现中完整体现：

- **微服务治理**: 必须包含服务注册中心、配置中心、API 网关三件套（注册、配置、网关）
- **消息驱动架构**: 必须使用 Kafka 构建异步解耦的数据流与事件流
- **高并发接入层**: 必须提供基于 Netty 的设备接入能力，并验证高并发连接场景
- **多租户数据库设计**: 必须在数据库模型、查询策略、权限边界中落实租户隔离

**理由**: 该项目目标是系统化学习 IoT 后端核心能力，上述四项是最小闭环能力集合，缺一不可。

### VII. 认证边界与职责划分原则 (Authentication Boundary & Responsibility)

用户认证与会话治理 MUST 按职责边界统一实施，禁止隐式信任与重复认证。

- **网关统一认证**: 所有用户侧 HTTP 请求 MUST 在 Gateway 完成 Token 校验与未授权拦截
- **身份服务签发**: 登录、登出、会话签发与会话失效 MUST 由 `tenant-service` 统一负责
- **下游禁止公网信任**: 下游服务 MUST NOT 信任公网直连请求中的 `X-Tenant-Id`、`X-User-Id`、`X-User-Role` 等身份 Header
- **受控入口信任**: 下游服务仅可在内网或受控入口条件下信任 Gateway 注入的身份上下文

**理由**: 清晰认证边界可避免认证逻辑分散、身份伪造与跨租户越权，提升系统安全性与可维护性。

### VIII. 严格 RBAC 权限模型原则 (Strict RBAC Permission Model)

权限系统 MUST 遵循严格 RBAC（Role-Based Access Control）模型，实现用户-角色-权限的灵活配置。

#### 核心模型

```
用户(sys_user) ←→ 角色(sys_role) ←→ 权限(sys_permission)
        │                    │
        └─ sys_user_role ────┘    └─ sys_role_permission ──┘
```

#### 数据库设计规范

| 表名 | 说明 | 必要字段 |
|------|------|----------|
| `sys_user` | 用户表 | id, tenant_id, username, password, status |
| `sys_role` | 角色表 | id, tenant_id, role_code, role_name, status |
| `sys_permission` | 权限表 | id, permission_code, permission_name, resource_type, resource_path |
| `sys_user_role` | 用户角色关联 | user_id, role_id |
| `sys_role_permission` | 角色权限关联 | role_id, permission_id |

#### 权限粒度规范

- **模块级**: `module:action` 格式，如 `device:view`、`device:create`
- **菜单级**: 控制菜单显示，如 `menu:device`
- **按钮级**: 控制操作按钮，如 `btn:device:delete`
- **数据级**: 结合租户隔离，控制数据范围

#### 设计约束

- **多角色支持**: 一个用户 MUST 可以拥有多个角色，权限取并集
- **租户隔离**: 角色和权限 MUST 支持租户级别隔离，平台级角色 tenant_id 为 NULL
- **权限继承**: 子角色可选继承父角色权限（MVP 阶段可暂不实现）
- **动态加载**: 权限 MUST 从数据库动态加载，禁止硬编码
- **缓存策略**: 权限数据 SHOULD 缓存到 Redis，变更时主动刷新

#### 预置角色

| 角色编码 | 角色名称 | 说明 | 级别 |
|----------|----------|------|------|
| `ADMIN` | 平台管理员 | 拥有所有权限 | 平台级 |
| `TENANT_ADMIN` | 租户管理员 | 管理租户内用户和设备 | 租户级 |
| `TENANT_USER` | 租户普通用户 | 仅查看权限 | 租户级 |

#### 权限校验流程

1. 用户登录 → 查询用户所有角色 → 查询角色所有权限 → 存入 Session/Redis
2. 请求到达 → Gateway 校验 Token → 下游服务校验权限 → 放行或拒绝
3. 前端渲染 → 根据权限列表控制菜单/按钮显示

**理由**: 严格 RBAC 模型提供灵活的权限配置能力，支持企业级多角色场景，权限变更无需发版，满足 SaaS 平台权限管理需求。

### IX. 可观测性强制原则 (Observability Enforcement)

系统 MUST 默认接入完整的可观测性三支柱（Metrics/Logs/Traces），生产环境必须具备监控告警能力。

#### 三支柱要求

| 支柱 | 技术选型 | 说明 |
|------|----------|------|
| **Metrics** | Micrometer + Prometheus | 指标收集与暴露 |
| **Logs** | SLF4J + Logback + ELK/LoKi | 结构化日志采集 |
| **Traces** | Micrometer Tracing + Zipkin/Jaeger | 分布式链路追踪 |

#### Metrics 指标规范

- **业务指标 MUST 暴露**：
  - 设备连接数（在线/离线）
  - 消息吞吐量（接收/处理/失败）
  - 处理延迟（P50/P95/P99）
  - 错误率（按类型分类）
- **系统指标 SHOULD 暴露**：
  - JVM 内存、GC、线程
  - 数据库连接池
  - Redis 连接池
  - Kafka 消费延迟
- **命名规范**：`{namespace}.{subsystem}.{metric_name}`，如 `openiot.device.connected.count`

#### Logs 日志规范

- **结构化输出**：日志 MUST 使用 JSON 格式，包含以下字段：
  ```json
  {
    "timestamp": "2026-03-01T12:00:00.000Z",
    "level": "INFO",
    "traceId": "abc123",
    "tenantId": "tenant-001",
    "service": "device-service",
    "message": "...",
    "context": {}
  }
  ```
- **日志级别**：
  - ERROR：异常、错误、需要立即处理
  - WARN：潜在问题、需要关注
  - INFO：关键业务事件（设备上下线、用户登录等）
  - DEBUG：调试信息（生产默认关闭）
- **敏感信息**：日志 MUST NOT 包含密码、Token、身份证等敏感信息

#### Traces 链路规范

- **Trace ID 传播**：所有跨服务调用 MUST 传播 Trace ID
- **Span 命名**：`{服务名}.{操作名}`，如 `device-service.createDevice`
- **关键埋点**：
  - HTTP 请求入口/出口
  - 数据库操作
  - Redis 操作
  - Kafka 消息生产/消费
  - 外部 API 调用

#### 生产环境告警要求

| 告警类型 | 条件 | 严重级别 |
|----------|------|----------|
| 服务宕机 | 健康检查失败 > 1min | Critical |
| 错误率 | 5xx > 1% | Warning |
| 响应延迟 | P95 > 3s | Warning |
| 消息积压 | Kafka Lag > 10000 | Warning |
| 设备离线率 | > 20% | Warning |
| 内存使用 | > 85% | Warning |
| 磁盘使用 | > 90% | Critical |

#### 健康检查端点

- **Liveness**: `/actuator/health/liveness` - 容器存活探针
- **Readiness**: `/actuator/health/readiness` - 服务就绪探针
- **详细健康**: `/actuator/health` - 包含依赖组件状态（仅内网访问）

#### 实现要求

- **自动装配**：可观测性组件 MUST 通过 Spring Boot Starter 自动装配
- **开关控制**：开发环境可关闭部分采集，生产环境 MUST 全部开启
- **性能影响**：采集开销 MUST < 5% CPU 和内存
- **采样策略**：Traces 生产环境 SHOULD 使用采样（如 10%）降低存储成本

**理由**: 生产系统必须具备"看见"能力，没有可观测性就是盲人摸象。Metrics 发现问题、Logs 定位问题、Traces 追踪问题，三者缺一不可。告警是可观测性的最后一公里，确保问题被及时感知。

## Technical Standards

### 后端技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| JDK | JDK 21 | 使用 LTS 版本，支持虚拟线程 |
| 框架 | Spring Boot 3.x | 企业级应用框架 |
| 微服务 | Spring Cloud Alibaba | 服务注册、配置、熔断等 |
| 网关 | Spring Cloud Gateway | 统一入口、路由转发、鉴权与限流 |
| 消息队列 | Kafka | 事件驱动、削峰填谷、异步解耦 |
| 数据库 | PostgreSQL | 主数据库 |
| 缓存 | Redis | 缓存和会话存储 |
| 文档库 | MongoDB | 设备日志、时序数据 |
| 认证 | Sa-Token | 轻量级权限认证 |
| 通信 | Netty | MQTT/TCP 设备接入 |
| ORM | MyBatis Plus | 数据访问层 |

### 前端技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| 框架 | Vue 3 | 渐进式 JavaScript 框架 |
| 构建工具 | Vite | 下一代前端构建工具 |
| UI 组件库 | Element Plus | Vue 3 企业级 UI 组件库 |
| 状态管理 | Pinia | Vue 3 官方推荐状态管理库 |

### 前端 UI/UX 设计原则

前端界面开发 MUST 遵循以下设计规范和质量标准：

- **设计工具**: 使用 UI/UX Pro Max skill 进行前端界面设计和代码生成，确保设计质量
- **组件库**: 统一使用 Element Plus 组件库，保持视觉一致性
- **响应式设计**: 所有界面必须支持响应式布局，适配不同屏幕尺寸
- **设计风格**: 遵循现代简洁风格，推荐使用 glassmorphism（毛玻璃）、minimalism（极简）或 bento grid（便当盒布局）
- **色彩系统**: 使用统一的色彩调色板，确保品牌一致性和可访问性
- **字体配对**: 使用推荐的字体配对方案，提升阅读体验
- **交互反馈**: 所有用户操作必须提供清晰的视觉反馈（加载状态、成功/错误提示）
- **可访问性**: 遵循 WCAG 标准，确保色盲用户、键盘用户等群体的可访问性

**理由**: 统一的 UI/UX 规范可以提升用户体验、减少设计决策成本、提高开发效率，并确保产品视觉一致性。

### 后端接口与对象映射规范

- 对象映射：Entity 与 VO/DTO 之间的转换 MUST 使用 `MapStruct`，禁止在业务代码中手写大段属性拷贝逻辑
- 入参约束：Controller 接口入参 MUST 使用 VO/DTO，禁止直接使用 Entity 作为请求参数
- 出参约束：Controller 接口返回值 MUST 使用 VO/DTO，禁止直接返回 Entity
- 边界约束：Entity 仅用于持久化层与领域内部，不得作为对外 API 契约
- **透明类型禁止**：接口入参和返回数据结构 MUST 使用自定义 VO/DTO 类，禁止使用 `Map`、`JSONObject`、`Object` 等不透明数据结构
  - 入参必须定义明确的请求 VO（如 `XxxRequest`、`XxxCreateVO`、`XxxUpdateVO`）
  - 出参必须定义明确的响应 VO（如 `XxxResponse`、`XxxVO`）
  - 禁止 `Map<String, Object>` 作为 Controller 层入参或出参
  - 禁止 `JSONObject`/`Map` 直接暴露给 API 层，必须在 Service 层完成转换

### 代码规范

- 编码格式：统一使用 UTF-8
- 注释要求：代码注释 MUST 使用中文简体；关键类、关键方法、复杂业务分支、边界条件与异常处理 MUST 提供详细注释
- 命名规范：遵循阿里巴巴 Java 开发手册
- 异常处理：统一使用 ApiResponse 封装返回，错误信息由前端处理

### 数据库规范

- 主键：使用 bigint 类型自增 ID
- 审计字段：createTime、updateTime、createBy、updateBy 必须存在
- 软删除：delete_flag 字段，默认 '0'，'1' 表示已删除
- 状态字段：status 字段，默认 '1' 启用，'0' 禁用
- 业务字典：多含义业务字段使用字符类型，通过字典表管理

## Development Workflow

### 开发流程

1. **需求分析**：明确功能需求和技术方案
2. **设计评审**：数据库设计、API 设计、架构设计
3. **测试先行**：编写测试用例，确认测试失败
4. **功能实现**：编写业务代码，通过测试
5. **代码审查**：Code Review 确保代码质量
6. **部署验证**：测试环境验证，生产环境发布

### 分支管理

- `main`: 生产环境分支，受保护
- `develop`: 开发环境分支
- `feature/*`: 功能开发分支
- `hotfix/*`: 紧急修复分支

### 提交规范

使用 Conventional Commits 格式：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具变更

## Governance

### 宪法管理

- 本宪法是项目开发的最高准则，所有开发活动 MUST 遵循本宪法
- 宪法修订需要经过团队讨论和批准
- 修订后需要更新版本号和修订日期

### 版本控制

- MAJOR: 原则删除或重新定义
- MINOR: 新增原则或重要扩展
- PATCH: 文字修正、澄清说明

### 合规检查

- 所有 PR MUST 经过 Code Review
- 代码审查需要检查是否符合宪法原则
- 复杂度增加必须有合理的理由和文档

**Version**: 1.6.0 | **Ratified**: 2026-02-25 | **Last Amended**: 2026-03-01
