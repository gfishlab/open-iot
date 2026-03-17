# Feature Specification: IoT 平台功能补全

**Feature Branch**: `004-iot-platform-completion`
**Created**: 2026-03-16
**Status**: Draft
**Input**: User description: "完成欠缺部分，优先高优先级核心短板，然后再中优先级功能，功能补全"

## 背景

基于对 Open-IoT 项目的全面代码审查，识别出以下核心短板和中优先级缺失功能。本规范按优先级排列，确保平台达到成熟 IoT 产品的完整度，同时为面试复盘和简历提供可验证的技术亮点。

**现有完成度**：核心数据链路（设备接入 -> 协议解析 -> Kafka -> 时序存储 -> 告警 -> 实时推送）已完整打通。微服务架构、多租户隔离、RBAC、可观测性完成度高。

**本次补全目标**：填补测试覆盖、服务韧性、MQTT 完整接入、OTA 升级、设备影子等关键缺失。

## Clarifications

### Session 2026-03-16

- Q: MQTT 指令下发路径（平台如何向设备发布 MQTT 消息） → A: connect-service 内嵌 MQTT Client，直接连接 EMQX 发布指令到设备订阅的 Topic，低延迟直连模式
- Q: 设备影子并发写入冲突策略 → A: 乐观锁（版本号机制），更新时携带 version，版本不匹配返回冲突错误，客户端重试
- Q: OTA 升级断电恢复策略 → A: 断点续传，设备记录已下载偏移量，重启后从断点继续下载，平台支持 HTTP Range 请求

---

## User Scenarios & Testing

### User Story 1 - 单元测试与集成测试覆盖 (Priority: P1)

作为开发者，我需要为核心业务逻辑（Service 层）编写单元测试，为关键链路（设备接入 -> 数据存储）编写集成测试，确保系统可回归验证，且面试时能证明代码质量意识。

**Why this priority**: 测试覆盖是衡量项目成熟度的第一指标，面试必问。当前仅有 4 个测试文件，几乎为零覆盖。补充测试不影响现有功能，风险最低，价值最高。

**Independent Test**: 运行 `mvn test` 全部通过，核心 Service 层覆盖率达到 60% 以上。

**Acceptance Scenarios**:

1. **Given** tenant-service 的 AuthService 和 PermissionService，**When** 运行单元测试，**Then** 登录认证、密码校验、权限缓存、角色检查等核心逻辑全部通过
2. **Given** device-service 的 ProductService 和 DeviceService，**When** 运行单元测试，**Then** 产品 CRUD、设备认证、物模型校验等核心逻辑全部通过
3. **Given** connect-service 的解析引擎（JavaScript/JSON/Regex/Binary），**When** 运行单元测试，**Then** 各解析器对正常数据、异常数据、边界数据的解析结果正确
4. **Given** data-service 的 AlarmService，**When** 运行单元测试，**Then** 阈值触发、表达式触发、变化率触发、静默期、告警恢复逻辑全部通过
5. **Given** 完整的设备数据上报链路，**When** 运行集成测试（Testcontainers），**Then** 数据从 Kafka 消费到 InfluxDB 写入到告警检测全链路验证通过

---

### User Story 2 - 服务熔断降级与网关限流 (Priority: P1)

作为平台运维人员，我需要系统在下游服务不可用时自动熔断降级，在流量突增时网关层限流保护，避免级联故障导致整个平台不可用。

**Why this priority**: 分布式系统的韧性是面试高频考点。当前无任何熔断降级机制，网关层也无限流，一个服务挂掉可能拖垮全部。这是生产级 IoT 平台的必备能力。

**Independent Test**: 手动停止某个下游服务，验证网关返回降级响应而非超时；使用压测工具触发限流，验证返回 429 状态码。

**Acceptance Scenarios**:

1. **Given** device-service 不可用，**When** 前端请求设备列表，**Then** 网关在 3 秒内返回友好的降级提示（如"服务暂时不可用，请稍后重试"），而非长时间等待超时
2. **Given** 网关配置每秒 100 次请求限流，**When** 某 IP 在 1 秒内发送 200 次请求，**Then** 超出部分返回 429 状态码，携带 Retry-After 响应头
3. **Given** device-service 从不可用恢复为可用，**When** 熔断器检测到服务恢复，**Then** 自动从熔断状态切换为半开状态，逐步恢复流量

---

### User Story 3 - MQTT 协议完整接入 (Priority: P1)

作为设备开发者，我需要通过标准 MQTT 协议将设备接入平台，使用 EMQX 作为 Broker，平台通过 Webhook 与 EMQX 集成，实现设备认证、消息接收、指令下发的完整闭环。

**指令下发路径**：connect-service 内嵌 MQTT Client SDK，直接连接 EMQX 发布指令到设备订阅的 Topic，采用低延迟直连模式（与 AWS IoT / 阿里云 IoT 一致）。

**Why this priority**: MQTT 是 IoT 行业标准协议，一个 IoT 平台没有完整的 MQTT 支持说不过去。当前只有 MqttAuthController（供 EMQX 调用的认证端点），但 EMQX 侧的 Webhook 配置、消息桥接、指令下发通道尚未打通。

**Independent Test**: 使用 MQTT 客户端工具（如 MQTTX）连接 EMQX，发布设备数据到指定 Topic，验证数据通过平台管道最终写入 InfluxDB；通过平台 API 下发指令，验证 MQTT 客户端收到消息。

**Acceptance Scenarios**:

1. **Given** 设备已在平台注册并获取 DeviceKey/DeviceSecret，**When** 设备使用 MQTT 客户端连接 EMQX（ClientId=deviceKey, Username=productKey, Password=deviceSecret），**Then** EMQX 调用平台认证接口验证通过，设备成功连接
2. **Given** 设备通过 MQTT 发布数据到 Topic `/devices/{deviceKey}/properties/report`，**When** EMQX Webhook 将消息转发到平台 connect-service，**Then** 平台完成协议解析、映射转换、Kafka 投递的完整流程
3. **Given** 平台用户通过 API 调用设备服务（如下发开关指令），**When** connect-service 通过内嵌 MQTT Client 发布指令到 Topic `/devices/{deviceKey}/services/invoke`，**Then** 设备端 MQTT 客户端成功接收指令并执行
4. **Given** 设备 MQTT 连接断开（意外掉线），**When** EMQX 检测到连接断开，**Then** 平台在 30 秒内更新设备状态为"离线"，并记录离线事件

---

### User Story 4 - 设备 OTA 升级 (Priority: P2)

作为设备管理员，我需要通过平台远程升级设备固件，支持版本管理、分批推送、升级进度跟踪和失败回滚，确保大规模设备固件管理的安全性和可控性。

**断电恢复策略**：采用断点续传机制。设备记录已下载的偏移量，断电重启后从断点继续下载，平台固件下载接口支持 HTTP Range 请求。

**Why this priority**: OTA 是成熟 IoT 平台的标志性功能，面试中是体现系统设计能力的加分项。但对当前 MVP 核心链路无阻塞，可在核心短板补齐后实现。

**Independent Test**: 上传一个测试固件包，创建升级任务并推送到测试设备，验证设备收到升级通知、下载固件、上报进度、完成升级的全流程。

**Acceptance Scenarios**:

1. **Given** 管理员上传固件包（含版本号、MD5 校验和），**When** 创建升级任务并选择目标设备/设备组，**Then** 平台生成升级任务并记录计划推送的设备清单
2. **Given** 升级任务已创建，**When** 按计划推送到设备，**Then** 设备收到升级通知（含固件下载地址、版本号、校验和），开始分片下载
3. **Given** 设备正在下载固件，**When** 设备每完成 10% 的下载，**Then** 设备上报进度到平台，管理员可实时查看每台设备的升级状态（待推送/下载中/安装中/成功/失败）
4. **Given** 设备固件升级失败（校验和不匹配或安装异常），**When** 设备上报失败状态，**Then** 平台记录失败原因，设备自动回滚到上一版本，管理员收到告警通知
5. **Given** 设备在下载过程中断电重启，**When** 设备恢复后请求继续下载，**Then** 平台通过 HTTP Range 支持断点续传，设备从已下载偏移量继续，无需重头开始

---

### User Story 5 - 设备影子 (Device Shadow) (Priority: P2)

作为应用开发者，我需要通过设备影子机制读取设备的最新状态（即使设备离线），并设置期望状态，设备上线后自动同步期望值，解决设备离线时的状态管理问题。

**并发控制**：采用乐观锁（版本号机制）。每次更新 desired 属性时必须携带当前 version，版本不匹配返回冲突错误（409），客户端需重新获取最新版本后重试。

**Why this priority**: 设备影子是 AWS IoT Core、阿里云 IoT 等主流平台的核心概念，面试被问到的概率很高。实现后显著提升平台专业度。

**Independent Test**: 通过 API 设置设备期望温度为 25 度，断开设备连接，重新连接后验证设备自动获取期望值并上报实际值更新影子。

**Acceptance Scenarios**:

1. **Given** 设备在线且属性值为 `{temperature: 22}`，**When** 应用通过 API 查询设备影子，**Then** 返回 `{reported: {temperature: 22}, desired: {}, delta: {}, version: N, metadata: {lastUpdated: ...}}`
2. **Given** 设备离线，**When** 应用通过 API 设置期望属性 `{desired: {temperature: 25}, version: N}`，**Then** 平台校验 version 匹配后保存期望值，version 递增为 N+1，返回更新后的影子
3. **Given** 两个应用同时修改同一设备的 desired 属性，**When** 第二个请求携带的 version 已过期，**Then** 平台返回 409 冲突错误，携带当前最新影子数据供客户端重试
4. **Given** 设备影子中存在 delta（期望值与上报值不同），**When** 设备重新上线，**Then** 平台立即将 delta 下发到设备，设备执行后上报新的实际值，影子自动更新
5. **Given** 设备上报新属性值与期望值一致，**When** 影子更新完成，**Then** delta 部分自动清空，reported 和 desired 值一致

---

### User Story 6 - 服务间同步通信（OpenFeign） (Priority: P2)

作为开发者，我需要在微服务之间增加同步调用能力（OpenFeign），用于需要实时返回结果的跨服务查询场景（如设备服务查询租户配额），当前仅有 Kafka 异步通信。

**Why this priority**: 纯 Kafka 异步通信无法满足所有场景，同步查询场景需要 Feign。面试中微服务通信选型是常见问题。

**Independent Test**: device-service 通过 Feign 调用 tenant-service 查询租户信息，验证调用成功且 Nacos 服务发现正常。

**Acceptance Scenarios**:

1. **Given** device-service 需要查询产品所属租户的配额信息，**When** 通过 OpenFeign 调用 tenant-service 的查询接口，**Then** 请求通过 Nacos 服务发现路由到正确实例，返回租户信息
2. **Given** tenant-service 不可用，**When** device-service 通过 Feign 调用失败，**Then** 熔断器自动降级，返回预定义的降级响应

---

### User Story 7 - 前端 CRUD 功能完善 (Priority: P2)

作为平台管理员，我需要在前端完成设备、租户、用户的新增和编辑功能（当前按钮显示"功能开发中"），以及规则引擎管理界面，形成完整的管理闭环。

**Why this priority**: 前端功能不完整会影响演示效果和面试展示。新增/编辑是最基本的 CRUD，补全后平台可完整演示。

**Independent Test**: 在前端完成设备新增、编辑、删除的全流程操作，数据正确持久化到数据库。

**Acceptance Scenarios**:

1. **Given** 管理员点击"新增设备"按钮，**When** 填写设备信息并提交，**Then** 设备创建成功，列表自动刷新显示新设备
2. **Given** 管理员点击某设备的"编辑"按钮，**When** 修改设备名称并保存，**Then** 设备信息更新成功，列表显示新名称
3. **Given** 管理员进入规则引擎管理页面，**When** 创建一条解析规则并测试，**Then** 规则保存成功，测试返回正确的解析结果

---

### User Story 8 - Nacos 配置中心启用 (Priority: P3)

作为运维人员，我需要将各微服务的配置集中管理在 Nacos 配置中心，支持运行时动态刷新，避免修改配置需要重启服务。

**Why this priority**: 配置中心是微服务基础设施标配，当前已引入 Nacos 但仅用于服务发现，配置中心功能未启用。风险低，但优先级低于核心功能。

**Independent Test**: 在 Nacos 控制台修改某个业务参数（如限流阈值），验证服务无需重启即可生效。

**Acceptance Scenarios**:

1. **Given** 各服务的配置存储在 Nacos 配置中心，**When** 服务启动时，**Then** 从 Nacos 拉取配置并覆盖本地默认值
2. **Given** 运维人员在 Nacos 修改限流参数从 10 改为 50，**When** 配置发布后，**Then** 服务在 30 秒内自动刷新，新请求使用新的限流阈值

---

### Edge Cases

- 设备在 OTA 升级过程中突然断电，重启后通过断点续传恢复（平台支持 HTTP Range 请求）
- 设备影子 delta 下发后设备不响应（超时），平台标记为同步失败，保留 delta 等待下次上线重试
- MQTT 连接风暴（大量设备同时重连）时，认证接口通过限流和缓存抗压
- Feign 调用链路过长（A -> B -> C）时，超时逐级递减传递，避免上游等待过久
- 熔断器半开状态下，探测请求失败则重新进入全开状态，等待下一个窗口期再次探测
- 设备影子并发写入冲突时，通过乐观锁（版本号）返回 409 错误，客户端重试

---

## Requirements

### Functional Requirements

**P1 - 高优先级（核心短板）**

- **FR-001**: 系统 MUST 为 tenant-service、device-service、connect-service、data-service 的核心 Service 层提供单元测试，覆盖正常流程、异常流程和边界条件
- **FR-002**: 系统 MUST 提供至少 1 套端到端集成测试，使用容器化测试框架模拟 PostgreSQL、Kafka、Redis，验证设备数据上报完整链路
- **FR-003**: 网关 MUST 集成熔断器，当下游服务不可用时自动熔断并返回降级响应
- **FR-004**: 网关 MUST 实现基于 IP 和基于用户的双维度请求限流，超出阈值返回 429 状态码
- **FR-005**: 系统 MUST 完整集成 EMQX MQTT Broker，支持设备通过 MQTT 协议连接、发布数据、接收指令
- **FR-006**: connect-service MUST 提供 EMQX Webhook 接收端点，处理设备连接/断开/消息发布等事件
- **FR-007**: connect-service MUST 通过内嵌 MQTT Client 直连 EMQX，向设备下发指令（服务调用、属性设置）

**P2 - 中优先级**

- **FR-008**: 系统 MUST 提供固件版本管理功能，支持固件包上传、版本记录、校验和验证
- **FR-009**: 系统 MUST 支持创建 OTA 升级任务，选择目标设备/设备组，分批推送升级通知
- **FR-010**: 系统 MUST 跟踪每台设备的升级状态（待推送/下载中/安装中/成功/失败），支持实时查看进度
- **FR-010a**: 固件下载接口 MUST 支持 HTTP Range 请求，实现设备断电后的断点续传
- **FR-011**: 系统 MUST 实现设备影子功能，包含 reported（设备上报）、desired（期望值）、delta（差异值）三层数据结构
- **FR-011a**: 设备影子 MUST 采用乐观锁（版本号）控制并发更新，版本不匹配时返回 409 冲突错误
- **FR-012**: 系统 MUST 在设备上线时自动检测 delta 并下发期望值到设备
- **FR-013**: 微服务之间 MUST 支持同步调用，结合服务发现实现负载均衡
- **FR-014**: 前端 MUST 完成设备、租户、用户的新增和编辑表单功能（替换当前的"功能开发中"占位）
- **FR-015**: 前端 MUST 提供规则引擎管理页面（解析规则、映射规则的 CRUD 和在线测试）

**P3 - 低优先级**

- **FR-016**: 各微服务 MUST 将核心配置迁移到配置中心，支持运行时动态刷新

### Key Entities

- **FirmwareVersion**: 固件版本（版本号、产品关联、固件包存储路径、校验和、文件大小、发布说明）
- **OtaUpgradeTask**: OTA 升级任务（目标产品、源版本、目标版本、推送策略、设备清单、整体进度）
- **OtaDeviceStatus**: 设备升级状态（设备关联、任务关联、当前状态、下载进度、已下载偏移量、失败原因、开始时间、完成时间）
- **DeviceShadow**: 设备影子（设备关联、reported 属性、desired 属性、delta 属性、version 版本号、最后更新时间）

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: 核心 Service 层单元测试覆盖率达到 60% 以上，所有测试在 3 分钟内通过
- **SC-002**: 端到端集成测试覆盖设备数据上报完整链路（接入 -> 解析 -> 消息投递 -> 存储 -> 告警），测试可重复执行且结果稳定
- **SC-003**: 下游服务不可用时，网关在 3 秒内返回降级响应，用户无需等待超时
- **SC-004**: 网关限流在超出阈值时正确返回 429 状态码，正常请求不受影响（误杀率 < 1%）
- **SC-005**: 设备通过 MQTT 协议完成认证、数据上报、指令接收的完整流程，端到端延迟 < 500ms
- **SC-006**: OTA 升级任务创建后，设备在 1 分钟内收到升级通知，升级进度实时可查
- **SC-007**: 设备影子在设备上线后 10 秒内完成 delta 同步，离线期间的期望值不丢失
- **SC-008**: 前端所有 CRUD 表单功能完整可用，无"功能开发中"占位提示
- **SC-009**: 配置中心变更后，服务在 30 秒内自动生效，无需重启

---

## Assumptions

1. EMQX Broker 已通过 Docker Compose 部署在开发环境中（当前 docker-compose 已包含 EMQX 配置）
2. 容器化测试框架（Testcontainers）可在开发环境正常运行（需要 Docker Desktop）
3. OTA 固件包存储使用本地文件系统（开发阶段），生产环境可替换为对象存储
4. 设备影子数据存储在缓存中（高频读写），持久化备份到关系数据库
5. 同步调用场景有限（仅用于跨服务实时查询），主要通信仍通过消息队列异步完成
6. 前端 CRUD 表单复用现有组件库和玻璃拟态样式体系
