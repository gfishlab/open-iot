# Tasks: IoT 平台功能补全

**Input**: Design documents from `/specs/004-iot-platform-completion/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contracts.md, quickstart.md

**Tests**: US1 明确要求测试覆盖，相关测试任务已包含。其余 User Story 不单独生成测试任务。

**Organization**: 任务按 User Story 分组，每个 Story 可独立实现和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属 User Story（US1-US8）
- 所有路径相对于项目根目录

---

## Phase 1: Setup (共享基础设施)

**Purpose**: 新增依赖、模块初始化、数据库迁移脚本

- [x] T001 在 backend/pom.xml 父 POM 中添加 JaCoCo maven plugin 配置，排除 entity/vo/config/mapper 包
- [x] T002 [P] 在 backend/gateway-service/pom.xml 中添加 spring-cloud-starter-circuitbreaker-reactor-resilience4j 依赖
- [x] T003 [P] 在 backend/connect-service/pom.xml 中添加 hivemq-mqtt-client:1.3.3 依赖
- [x] T004 [P] 创建 backend/common/common-feign-api/ 模块：pom.xml（依赖 spring-cloud-starter-openfeign + spring-cloud-starter-loadbalancer + common-core）
- [x] T005 在 backend/pom.xml 的 modules 节点中注册 common/common-feign-api 模块
- [x] T006 [P] 在 backend/device-service/pom.xml 中添加 common-feign-api、testcontainers（junit-jupiter + postgresql）、spring-boot-testcontainers 依赖
- [x] T007 [P] 在 backend/data-service/pom.xml 中添加 common-feign-api 依赖
- [x] T008 [P] 在 backend/rule-service/pom.xml 中添加 common-feign-api 依赖
- [x] T009 [P] 创建 Flyway 迁移脚本 backend/device-service/src/main/resources/db/migration/V1.7.0__add_ota_tables.sql（firmware_version + ota_upgrade_task + ota_device_status 三张表，按 data-model.md 定义）
- [x] T010 [P] 创建 Flyway 迁移脚本 backend/device-service/src/main/resources/db/migration/V1.8.0__add_device_shadow.sql（device_shadow 表，按 data-model.md 定义）

**Checkpoint**: 依赖就绪，模块注册完成，数据库表结构通过 Flyway 迁移创建

---

## Phase 2: Foundational (阻塞性前置)

**Purpose**: OpenFeign 公共模块实现 + Kafka EventType 扩展（多个 User Story 依赖）

**CRITICAL**: 此阶段必须完成后才能开始 User Story 实现

- [x] T011 [P] 创建 Feign DTO：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/dto/TenantDTO.java
- [x] T012 [P] 创建 Feign DTO：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/dto/UserDTO.java
- [x] T013 [P] 创建 Feign DTO：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/dto/DeviceDTO.java
- [x] T014 [P] 创建 Feign DTO：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/dto/ProductDTO.java
- [x] T015 [P] 创建 Feign DTO：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/dto/DeviceShadowDTO.java
- [x] T016 创建 TenantFeignClient 接口：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/client/TenantFeignClient.java（GET /api/v1/tenants/{tenantId}、GET /api/v1/users/{userId}）
- [x] T017 创建 DeviceFeignClient 接口：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/client/DeviceFeignClient.java（GET /api/v1/devices/{deviceId}、GET /api/v1/products/{productId}、GET /api/v1/devices/{deviceId}/shadow）
- [x] T018 创建 TenantContextInterceptor：backend/common/common-feign-api/src/main/java/com/openiot/common/feign/interceptor/TenantContextInterceptor.java（从 TenantContext TTL 读取 tenant/user/role 注入请求头）
- [x] T019 [P] 在 EventType 枚举中新增 OTA_UPGRADE、OTA_PROGRESS、SHADOW_DELTA、SHADOW_GET、SHADOW_REPORTED（定位 backend/ 中的 EventType 或 EventEnvelope 相关类）

**Checkpoint**: Feign 公共模块可被各服务引用，Kafka 事件类型扩展完成

---

## Phase 3: User Story 1 - 单元测试与集成测试覆盖 (Priority: P1)

**Goal**: 为核心 Service 层编写单元测试，核心覆盖率达到 60% 以上

**Independent Test**: 运行 `cd backend && mvn test` 全部通过

### connect-service 解析引擎测试（P0 - 纯计算逻辑，零外部依赖）

- [x] T020 [P] [US1] 创建 BinaryParserTest：backend/connect-service/src/test/java/com/openiot/connect/parser/BinaryParserTest.java（正常解析、异常数据、边界条件）
- [x] T021 [P] [US1] 创建 JsonPathParserTest：backend/connect-service/src/test/java/com/openiot/connect/parser/JsonPathParserTest.java（JSON 路径提取、嵌套对象、数组）
- [x] T022 [P] [US1] 创建 RegexParserTest：backend/connect-service/src/test/java/com/openiot/connect/parser/RegexParserTest.java（正则匹配、分组提取、无匹配）
- [x] T023 [P] [US1] 创建 JavaScriptParserTest：backend/connect-service/src/test/java/com/openiot/connect/parser/JavaScriptParserTest.java（GraalJS 脚本执行、超时控制、异常脚本）
- [x] T024 [P] [US1] 创建 MappingRuleEngineTest：backend/connect-service/src/test/java/com/openiot/connect/rule/MappingRuleEngineTest.java（属性映射、事件映射、转换表达式）

### tenant-service 核心测试（P1）

- [x] T025 [P] [US1] 创建 AuthServiceTest：backend/tenant-service/src/test/java/com/openiot/tenant/service/AuthServiceTest.java（登录认证、密码校验、Token 生成，Mock Mapper + MockedStatic StpUtil）
- [x] T026 [P] [US1] 创建 PermissionServiceTest：backend/tenant-service/src/test/java/com/openiot/tenant/service/PermissionServiceTest.java（权限缓存、角色检查、权限树构建）

### device-service 核心测试（P1）

- [x] T027 [P] [US1] 创建 ProductServiceTest：backend/device-service/src/test/java/com/openiot/device/service/ProductServiceTest.java（产品 CRUD、物模型关联，Mock Mapper）
- [x] T028 [P] [US1] 创建 DeviceServiceTest：backend/device-service/src/test/java/com/openiot/device/service/DeviceServiceTest.java（设备注册、认证、状态更新，Mock Mapper + Redis）
- [x] T029 [P] [US1] 创建 ThingModelServiceTest：backend/device-service/src/test/java/com/openiot/device/service/ThingModelServiceTest.java（物模型校验、属性/事件/服务定义验证）
- [x] T030 [P] [US1] 创建 AlertServiceTest：backend/device-service/src/test/java/com/openiot/device/service/AlertServiceTest.java（阈值触发、表达式触发、静默期、告警恢复）

### data-service 核心测试（P2）

- [x] T031 [P] [US1] 创建 AlarmServiceTest：backend/data-service/src/test/java/com/openiot/data/service/AlarmServiceTest.java（告警规则匹配、变化率触发，Mock Mapper + InfluxDB）
- [x] T032 [P] [US1] 创建 DeviceStatusServiceTest：backend/data-service/src/test/java/com/openiot/data/service/DeviceStatusServiceTest.java（设备状态记录、上下线统计）

### JaCoCo 覆盖率验证

- [x] T033 [US1] 运行 `mvn verify` 验证 JaCoCo 覆盖率报告生成，确认核心 Service 层覆盖率 >= 60%

**Checkpoint**: `mvn test` 全部通过，覆盖率报告可查看

---

## Phase 4: User Story 2 - 服务熔断降级与网关限流 (Priority: P1)

**Goal**: 网关集成 Resilience4j 熔断 + Redis 令牌桶限流，下游不可用时自动降级

**Independent Test**: 停止某下游服务，网关 3 秒内返回降级响应；压测触发限流返回 429

### Implementation

- [x] T034 [P] [US2] 创建 FallbackController：backend/gateway-service/src/main/java/com/openiot/gateway/controller/FallbackController.java（统一降级响应 503，按 api-contracts.md 格式）
- [x] T035 [P] [US2] 创建 RateLimitConfig：backend/gateway-service/src/main/java/com/openiot/gateway/config/RateLimitConfig.java（IP 维度 KeyResolver + 用户维度 KeyResolver）
- [x] T036 [US2] 在 gateway-service 的 application.yml 中配置 Resilience4j CircuitBreaker（slidingWindowSize=20, failureRateThreshold=50%, waitDurationInOpenState=15s, permittedCallsInHalfOpen=5, slowCallDurationThreshold=3s）+ 各路由的 CircuitBreaker GatewayFilter + fallbackUri
- [x] T037 [US2] 在 gateway-service 的 application.yml 中配置 RequestRateLimiter GatewayFilter（Redis 令牌桶，replenishRate + burstCapacity），绑定 IP KeyResolver

**Checkpoint**: 网关熔断降级和限流就绪，可手动验证

---

## Phase 5: User Story 3 - MQTT 协议完整接入 (Priority: P1)

**Goal**: EMQX Webhook 接收 + HiveMQ MQTT Client 指令下发，打通 MQTT 完整数据链路

**Independent Test**: MQTTX 连接 EMQX 发布数据，验证平台管道处理；通过 API 下发指令，MQTTX 收到

### Implementation

- [x] T038 [P] [US3] 创建 MQTT 配置属性类：backend/connect-service/src/main/java/com/openiot/connect/mqtt/MqttProperties.java（broker URL、clientId 前缀、自动重连参数）
- [x] T039 [US3] 创建 MqttClientManager：backend/connect-service/src/main/java/com/openiot/connect/mqtt/MqttClientManager.java（HiveMQ MQTT Client 生命周期管理，@PostConstruct 连接 / @PreDestroy 断开，自动重连，publish 方法支持指定 Topic + QoS）
- [x] T040 [US3] 创建 MqttWebhookController：backend/connect-service/src/main/java/com/openiot/connect/mqtt/MqttWebhookController.java（3 个端点：POST /api/v1/mqtt/webhook/connected、disconnected、message，按 api-contracts.md 契约）
- [x] T041 [US3] 实现 MqttWebhookController 的 connected 处理逻辑：通过 deviceKey 查找设备 → 更新设备在线状态 → 发布 Kafka DEVICE_ONLINE 事件
- [x] T042 [US3] 实现 MqttWebhookController 的 disconnected 处理逻辑：更新设备离线状态 → 发布 Kafka DEVICE_OFFLINE 事件
- [x] T043 [US3] 实现 MqttWebhookController 的 message 处理逻辑：解析 Topic 路由 → 进入现有解析管道（复用 connect-service 的协议解析 + Kafka 投递链路）
- [x] T044 [US3] 实现 MQTT 指令下发：在 MqttClientManager 中添加 publishCommand 方法，支持通过 MQTT Topic 下发属性设置/服务调用指令到设备
- [x] T045 [US3] 在 connect-service 的 application.yml 中添加 MQTT 配置项（emqx broker 地址、端口、客户端配置）
- [x] T046 [US3] 创建 MQTT 健康检查端点：在 MqttClientManager 中暴露连接状态到 Spring Boot Actuator health

**Checkpoint**: MQTT 数据上行 + 指令下行完整闭环，EMQX Webhook 配置文档已在 quickstart.md

---

## Phase 6: User Story 4 - 设备 OTA 升级 (Priority: P2)

**Goal**: 固件版本管理 + OTA 升级任务 + 设备升级状态跟踪 + 断点续传

**Independent Test**: 上传固件 → 创建任务 → 推送设备 → 跟踪状态 → 固件下载（含 Range 请求）

### Entity + Mapper

- [x] T047 [P] [US4] 创建 FirmwareVersion 实体：backend/device-service/src/main/java/com/openiot/device/ota/entity/FirmwareVersion.java
- [x] T048 [P] [US4] 创建 OtaUpgradeTask 实体：backend/device-service/src/main/java/com/openiot/device/ota/entity/OtaUpgradeTask.java
- [x] T049 [P] [US4] 创建 OtaDeviceStatus 实体：backend/device-service/src/main/java/com/openiot/device/ota/entity/OtaDeviceStatus.java
- [x] T050 [P] [US4] 创建 FirmwareVersionMapper：backend/device-service/src/main/java/com/openiot/device/ota/mapper/FirmwareVersionMapper.java
- [x] T051 [P] [US4] 创建 OtaUpgradeTaskMapper：backend/device-service/src/main/java/com/openiot/device/ota/mapper/OtaUpgradeTaskMapper.java
- [x] T052 [P] [US4] 创建 OtaDeviceStatusMapper：backend/device-service/src/main/java/com/openiot/device/ota/mapper/OtaDeviceStatusMapper.java

### VO

- [x] T053 [P] [US4] 创建 OTA 相关 VO：backend/device-service/src/main/java/com/openiot/device/ota/vo/（FirmwareUploadVO、OtaTaskCreateVO、OtaTaskVO、OtaDeviceStatusVO、FirmwareVersionVO）

### Service

- [x] T054 [US4] 创建 FirmwareService：backend/device-service/src/main/java/com/openiot/device/ota/service/FirmwareService.java（固件上传含 MD5/SHA256 计算、版本列表查询、文件存储到 data/firmware/{tenantId}/{productId}/{version}/）
- [x] T055 [US4] 创建 OtaTaskService：backend/device-service/src/main/java/com/openiot/device/ota/service/OtaTaskService.java（创建升级任务、生成 OtaDeviceStatus 记录、发送 Kafka OTA_UPGRADE 事件）
- [x] T056 [US4] 创建 OtaProgressService：backend/device-service/src/main/java/com/openiot/device/ota/service/OtaProgressService.java（处理设备进度上报、状态机流转 pending→pushing→downloading→installing→success/failed、更新任务统计计数）

### Controller

- [x] T057 [US4] 创建 FirmwareController：backend/device-service/src/main/java/com/openiot/device/ota/controller/FirmwareController.java（POST /api/v1/ota/firmware 上传、GET /api/v1/ota/firmware 列表、GET /api/v1/ota/firmware/{firmwareId}/download 下载含 HTTP Range 断点续传）
- [x] T058 [US4] 创建 OtaTaskController：backend/device-service/src/main/java/com/openiot/device/ota/controller/OtaTaskController.java（POST /api/v1/ota/tasks 创建任务、GET /api/v1/ota/tasks/{taskId}/devices 查询设备状态）

### Kafka 消费（connect-service 侧）

- [x] T059 [US4] 在 connect-service 中添加 OTA 升级通知 Kafka 消费者：监听 OTA_UPGRADE 事件 → 通过 MqttClientManager 发布到设备 Topic /devices/{dk}/ota/notify

**Checkpoint**: OTA 完整流程可通过 API 验证

---

## Phase 7: User Story 5 - 设备影子 (Priority: P2)

**Goal**: 设备影子 reported/desired/delta 管理 + Redis 缓存 + 乐观锁 + delta 自动下发

**Independent Test**: 设置 desired → 断开设备 → 重连后验证 delta 下发 → 设备上报后 delta 清空

### Entity + Mapper

- [x] T060 [P] [US5] 创建 DeviceShadow 实体：backend/device-service/src/main/java/com/openiot/device/shadow/entity/DeviceShadow.java（JSONB 字段用 String 或 JsonNode 映射）
- [x] T061 [P] [US5] 创建 DeviceShadowMapper：backend/device-service/src/main/java/com/openiot/device/shadow/mapper/DeviceShadowMapper.java（含自定义乐观锁更新方法 updateReportedWithVersion、updateDesiredWithVersion）

### VO

- [x] T062 [P] [US5] 创建影子 VO：backend/device-service/src/main/java/com/openiot/device/shadow/vo/（DeviceShadowVO、DesiredUpdateVO）

### Service

- [x] T063 [US5] 创建 DeviceShadowService：backend/device-service/src/main/java/com/openiot/device/shadow/service/DeviceShadowService.java（查询影子 Redis→PG 双层缓存、更新 reported + delta 计算、更新 desired 乐观锁 + 409 冲突、delta 下发触发）
- [x] T064 [US5] 实现 delta 计算工具方法：在 DeviceShadowService 中用 Jackson JsonNode 遍历 desired 和 reported 的差异，返回 delta JSON

### Controller

- [x] T065 [US5] 创建 DeviceShadowController：backend/device-service/src/main/java/com/openiot/device/shadow/controller/DeviceShadowController.java（GET /api/v1/devices/{deviceId}/shadow、PUT /api/v1/devices/{deviceId}/shadow/desired 含 409 冲突响应）

### Kafka 集成

- [x] T066 [US5] 在 device-service 中添加设备上线事件消费者：监听 DEVICE_ONLINE → 检查 delta → 非空则发送 SHADOW_DELTA Kafka 事件
- [x] T067 [US5] 在 device-service 中添加 SHADOW_REPORTED 消费者：设备上报 reported → 更新影子 reported + 重算 delta + 更新 Redis 缓存
- [x] T068 [US5] 在 connect-service 中添加 SHADOW_DELTA 消费者：监听 SHADOW_DELTA → 通过 MqttClientManager 发布到设备 Topic /devices/{dk}/shadow/update

**Checkpoint**: 影子完整生命周期可通过 API + Kafka 事件验证

---

## Phase 8: User Story 6 - 服务间同步通信 OpenFeign (Priority: P2)

**Goal**: 微服务间通过 OpenFeign + Nacos 实现同步调用，携带租户上下文

**Independent Test**: device-service 通过 Feign 调用 tenant-service 查询租户信息，调用成功且上下文传递正确

### Implementation

- [x] T069 [P] [US6] 在 tenant-service 中确保 GET /api/v1/tenants/{tenantId} 和 GET /api/v1/users/{userId} 端点存在，返回格式与 TenantDTO/UserDTO 匹配
- [x] T070 [P] [US6] 在 device-service 中确保 GET /api/v1/devices/{deviceId} 和 GET /api/v1/products/{productId} 端点存在，返回格式与 DeviceDTO/ProductDTO 匹配
- [x] T071 [US6] 在 device-service 启动类上添加 @EnableFeignClients(basePackages="com.openiot.common.feign.client")，注册 Feign 配置
- [x] T072 [US6] 在 device-service 中使用 TenantFeignClient 替代一处现有的跨服务查询（如设备注册时查询租户配额）
- [x] T073 [US6] 在 connect-service 启动类上添加 @EnableFeignClients，添加 common-feign-api 依赖到 pom.xml，注册 DeviceFeignClient 用于 Webhook 中查询设备信息

**Checkpoint**: Feign 调用链路通过 Nacos 服务发现正常工作

---

## Phase 9: User Story 7 - 前端 CRUD 功能完善 (Priority: P2)

**Goal**: 补全设备/租户/用户的新增编辑表单 + 规则引擎管理页面 + API 层统一抽取

**Independent Test**: 前端完成设备新增、编辑、删除全流程，数据正确持久化

### API 层统一抽取

- [x] T074 [P] [US7] 创建 frontend/src/api/device.ts（设备 CRUD API：list、create、update、delete）
- [x] T075 [P] [US7] 创建 frontend/src/api/product.ts（产品 CRUD API）
- [x] T076 [P] [US7] 创建 frontend/src/api/tenant.ts（租户 CRUD API）
- [x] T077 [P] [US7] 创建 frontend/src/api/user.ts（用户 CRUD API）

### 设备表单

- [x] T078 [US7] 创建 DeviceForm.vue：frontend/src/views/device/DeviceForm.vue（Element Plus Dialog + Form，新增/编辑模式切换，关联产品选择，玻璃拟态样式）
- [x] T079 [US7] 在 DeviceList.vue 中集成 DeviceForm（"新增设备"按钮打开表单、"编辑"按钮预填数据、提交后刷新列表）

### 租户表单

- [x] T080 [US7] 创建 TenantForm.vue：frontend/src/views/tenant/TenantForm.vue（租户新增/编辑表单，包含租户名称、管理员信息、配额设置）
- [x] T081 [US7] 在 TenantList.vue 中集成 TenantForm（替换"功能开发中"占位）

### 用户表单

- [x] T082 [US7] 创建 UserForm.vue：frontend/src/views/tenant/UserForm.vue（用户新增/编辑表单，角色分配，所属租户）
- [x] T083 [US7] 在用户管理列表中集成 UserForm（替换"功能开发中"占位）

### 规则引擎管理

- [x] T084 [US7] 创建 ParseRuleList.vue：frontend/src/views/rule/ParseRuleList.vue（解析规则列表 + CRUD + 在线测试功能）
- [x] T085 [US7] 创建 MappingRuleList.vue：frontend/src/views/rule/MappingRuleList.vue（映射规则列表 + CRUD + 关联产品选择）
- [x] T086 [US7] 在前端路由中注册规则引擎管理页面路由（ParseRuleList、MappingRuleList）

**Checkpoint**: 前端所有 CRUD 功能完整可用，无"功能开发中"占位

---

## Phase 10: User Story 8 - Nacos 配置中心启用 (Priority: P3)

**Goal**: 各微服务将业务参数迁移到 Nacos Config，支持运行时动态刷新

**Independent Test**: 在 Nacos 控制台修改限流阈值，服务 30 秒内自动生效

### Implementation

- [x] T087 [P] [US8] 在各服务的 bootstrap.yml 中启用 Nacos Config（spring.cloud.nacos.config.server-addr、data-id、group、file-extension）
- [x] T088 [US8] 将 gateway-service 的限流参数（replenishRate、burstCapacity）迁移到 Nacos 配置，使用 @RefreshScope 或 @ConfigurationProperties 动态刷新
- [x] T089 [US8] 将各服务的业务参数（缓存 TTL、重试次数、批量大小等）迁移到 Nacos，敏感配置（数据库密码、Redis 密码）保留本地

**Checkpoint**: Nacos 控制台修改配置后服务自动生效，无需重启

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: 权限注册、文档更新、整体验证

- [x] T090 [P] 在 sys_permission 表中新增 OTA 和设备影子相关权限记录（Flyway 迁移或 SQL 脚本）
- [x] T091 [P] 为 Gateway 路由配置新增 OTA 和影子 API 的路由规则（/api/v1/ota/**、/api/v1/devices/*/shadow/**）
- [x] T092 运行 quickstart.md 中的验证命令，确认所有功能端到端正常
- [x] T093 运行 `cd backend && mvn test` 确认全部测试通过，无回归

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖，可立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 完成（common-feign-api 模块必须先创建）
- **Phase 3 (US1 测试)**: 依赖 Phase 1（JaCoCo 配置），可与 Phase 2 并行
- **Phase 4 (US2 熔断)**: 依赖 Phase 1（gateway 依赖），可与其他 P1 Story 并行
- **Phase 5 (US3 MQTT)**: 依赖 Phase 1（connect 依赖），可与其他 P1 Story 并行
- **Phase 6 (US4 OTA)**: 依赖 Phase 1（迁移脚本）+ Phase 2（EventType）+ Phase 5（MqttClientManager）
- **Phase 7 (US5 影子)**: 依赖 Phase 1（迁移脚本）+ Phase 2（EventType）+ Phase 5（MqttClientManager）
- **Phase 8 (US6 Feign)**: 依赖 Phase 2（Feign 模块完成）
- **Phase 9 (US7 前端)**: 无后端依赖，可与任何 Phase 并行
- **Phase 10 (US8 Nacos)**: 依赖 Phase 4（限流参数已配置后才能迁移）
- **Phase 11 (Polish)**: 依赖所有 User Story 完成

### User Story Dependencies

```
Phase 1 (Setup) ─────────────────────────────────────┐
    │                                                  │
    ├──→ Phase 2 (Foundational) ──┬──→ Phase 6 (US4)  │
    │                             ├──→ Phase 7 (US5)  │
    │                             └──→ Phase 8 (US6)  │
    │                                                  │
    ├──→ Phase 3 (US1 测试) ◄──── 可与 Phase 2 并行   │
    ├──→ Phase 4 (US2 熔断) ◄──── 可与 Phase 2 并行   │
    ├──→ Phase 5 (US3 MQTT) ◄──── 可与 Phase 2 并行   │
    │         │                                        │
    │         └──→ Phase 6 (US4) + Phase 7 (US5)       │
    │                                                  │
    └──→ Phase 9 (US7 前端) ◄──── 独立，随时可开始 ────┘
              │
              └──→ Phase 10 (US8) → Phase 11 (Polish)
```

### Within Each User Story

- Entity/Mapper（可并行） → Service → Controller → Kafka 集成
- 前端：API 层（可并行） → 组件 → 路由注册

### Parallel Opportunities

**P1 Story 并行**:
```
  Phase 3 (US1 测试)  ─┐
  Phase 4 (US2 熔断)  ─┤── 三者可同时进行
  Phase 5 (US3 MQTT)  ─┘
```

**P2 Story 并行**（Phase 2 + Phase 5 完成后）:
```
  Phase 6 (US4 OTA)   ─┐
  Phase 7 (US5 影子)   ─┤── 三者可同时进行
  Phase 8 (US6 Feign)  ─┘
  Phase 9 (US7 前端)   ─┘ ← 前端可更早开始
```

---

## Implementation Strategy

### MVP First（P1 核心短板）

1. Complete Phase 1: Setup（依赖 + 模块 + 迁移脚本）
2. Parallel: Phase 2 (Foundational) + Phase 3 (US1) + Phase 4 (US2) + Phase 5 (US3)
3. **STOP and VALIDATE**: 测试通过、熔断生效、MQTT 闭环
4. 此时项目已具备：测试覆盖 + 服务韧性 + MQTT 完整接入

### Incremental Delivery（P2 增值功能）

5. Phase 6 (US4 OTA) + Phase 7 (US5 影子) + Phase 8 (US6 Feign) + Phase 9 (US7 前端) — 并行推进
6. **STOP and VALIDATE**: OTA 全流程、影子同步、Feign 调用、前端完整
7. Phase 10 (US8 Nacos) → Phase 11 (Polish)

---

## Notes

- [P] 任务 = 不同文件，无依赖，可并行
- [Story] 标签映射到 spec.md 中的 User Story
- 每个 Phase Checkpoint 后执行 git commit + push
- 前端组件必须遵循 CLAUDE.md 玻璃拟态设计规范
- 所有 Controller 的 @RequestParam/@PathVariable 必须显式指定 name 属性
- 所有新表包含 tenant_id，MyBatis Plus TenantLineHandler 自动注入
