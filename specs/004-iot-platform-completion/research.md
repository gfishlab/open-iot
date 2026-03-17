# Research Report: IoT 平台功能补全

**Feature**: 004-iot-platform-completion
**Date**: 2026-03-16

---

## 1. 服务熔断降级与网关限流

### Decision: Resilience4j + Gateway RequestRateLimiter (Redis)

**熔断方案**：Spring Cloud CircuitBreaker + Resilience4j

- 依赖：`spring-cloud-starter-circuitbreaker-reactor-resilience4j`（Spring Cloud 2023.0.0 BOM 已管理版本）
- 集成方式：YAML 路由级 CircuitBreaker GatewayFilter + `fallbackUri: forward:/fallback/{service}`
- 降级实现：Gateway 本地 FallbackController 返回统一 503 响应

**Rationale**: Resilience4j 是 Spring Cloud 2023.0.0 默认实现，零版本风险。项目的 Spring Cloud Alibaba 2022.0.0.0 与 Spring Cloud 2023.0.0 有版本错位，引入 Sentinel 会增加兼容性风险。

**限流方案**：Spring Cloud Gateway 内置 RequestRateLimiter (Redis)

- 基于 Redis Lua 脚本的令牌桶，天然分布式
- 通过 KeyResolver 支持 IP/用户/租户多维度限流
- 项目 Gateway 已引入 `spring-boot-starter-data-redis-reactive`，零额外依赖

**Alternatives Considered**:
- Sentinel：功能更强（热点限流、系统自适应保护），但需部署 Dashboard，版本兼容性风险高。留作未来生产环境升级选项。
- Resilience4j RateLimiter：纯本地内存，不支持分布式，不推荐。

**熔断器参数**：slidingWindowSize=20, failureRateThreshold=50%, waitDurationInOpenState=15s, permittedCallsInHalfOpen=5, slowCallDurationThreshold=3s

---

## 2. MQTT 协议完整接入

### Decision: EMQX HTTP Authenticator + Rule Engine Webhook → connect-service + HiveMQ MQTT Client

**EMQX 认证**：使用 EMQX 5.x 原生 HTTP Authenticator
- EMQX POST 到 device-service `/api/v1/mqtt/auth`
- 响应格式：`{"result": "allow"}` / `{"result": "deny"}`（现有 MqttAuthController 已兼容）
- 认证凭据统一为：ClientId=deviceKey, Username=productKey, Password=deviceSecret

**Webhook 消息转发**：EMQX Rule Engine + HTTP Action → connect-service
- 3 类事件：client.connected、client.disconnected、message.publish
- connect-service 新建 MqttWebhookController 统一接收
- 上下线事件从 device-service 迁移到 connect-service（单一职责，接入层统一管理）

**MQTT Client SDK 选择**：HiveMQ MQTT Client 1.3.3
- 完整 MQTT 5.0 支持（Paho 仅 3.1.1）
- 内置自动重连（指数退避）、线程安全、CompletableFuture API
- 依赖：`com.hivemq:hivemq-mqtt-client:1.3.3`

**Topic 设计**：`/devices/{deviceKey}/{功能域}/{操作方向}`

| Topic | 方向 | 用途 |
|-------|------|------|
| `/devices/{dk}/properties/report` | 上行 | 属性上报 |
| `/devices/{dk}/properties/set` | 下行 | 属性设置 |
| `/devices/{dk}/events/report` | 上行 | 事件上报 |
| `/devices/{dk}/services/invoke` | 下行 | 服务调用 |
| `/devices/{dk}/services/response` | 上行 | 服务响应 |
| `/devices/{dk}/shadow/get` | 上行 | 获取影子 |
| `/devices/{dk}/shadow/update` | 双向 | 影子更新 |
| `/devices/{dk}/ota/notify` | 下行 | OTA 通知 |
| `/devices/{dk}/ota/progress` | 上行 | OTA 进度 |

**MQTT Client 生命周期**：Spring Bean(@PostConstruct/@PreDestroy) + HiveMQ 内置重连

**Alternatives Considered**:
- Eclipse Paho：不支持 MQTT 5.0，长期技术债
- Netty MQTT Codec：项目已有 netty-all 但仅提供编解码，协议状态机需自行实现，工作量过大
- EMQX 直投 Kafka：性能最高但绕过 connect-service 的解析层，不推荐

---

## 3. 测试策略

### Decision: JUnit 5 + Mockito 单元测试 + Testcontainers 集成测试 + JaCoCo 覆盖率

**现状**：
- `spring-boot-starter-test` 已全局引入（父 POM）
- Testcontainers BOM 1.19.5 已声明但未使用
- 仅 4 个测试文件（3 个在 device-service，1 个在 tenant-service 且无断言）
- JaCoCo 未配置

**单元测试优先级**（按可测试性排序）：

| 优先级 | 模块 | 测试类 | 策略 |
|--------|------|--------|------|
| P0 | connect-service | BinaryParser, JsonPathParser, RegexParser, JavaScriptParser, MappingRuleEngine | 纯计算逻辑，零外部依赖，最适合单元测试 |
| P1 | tenant-service | AuthService, PermissionService | Mock Mapper + RedisTemplate + MockedStatic(StpUtil) |
| P1 | device-service | ThingModelService, ProductService, DeviceService, AlertService | Mock Mapper + Redis，物模型验证是纯函数 |
| P2 | data-service | AlarmService, DataForwardService | Mock Mapper + InfluxDB |

**集成测试**：Spring Boot 3.x @ServiceConnection + Testcontainers
- 容器：PostgreSQL、Redis、Kafka
- 验证：Flyway 迁移 + 端到端数据链路
- Windows 注意：需 Docker Desktop + WSL2 后端

**JaCoCo**：父 POM 配置 maven plugin，排除 entity/vo/config/mapper 包

**Alternatives Considered**:
- H2 内存数据库：device-service 已引入，可用于简单数据库测试但不支持 PostgreSQL 特有语法（JSONB）
- WireMock：外部 API mock，当前阶段不需要

---

## 4. OTA 升级

### Decision: 3 表模型 + 5 态状态机 + 自定义 Controller 断点续传 + Kafka 通知

**数据表**：firmware_version、ota_upgrade_task、ota_device_status，放在 openiot_device 数据库

**状态机**：pending → pushing → downloading → installing → success/failed

**断点续传**：自定义 Controller + HTTP Range 请求（需设备鉴权和进度记录，静态资源方案无法满足）

**固件存储**：本地文件系统 `data/firmware/{tenantId}/{productId}/{version}/`，生产环境可迁移到 MinIO/OSS

**OTA 通知**：device-service → Kafka(iot-ota-command) → connect-service → MQTT/TCP → 设备

**Alternatives Considered**:
- Spring Resource + ResourceRegion：快速但无法插入鉴权逻辑
- EMQX 直接下发 OTA：绕过 connect-service 的管理层

---

## 5. 设备影子

### Decision: Redis 热缓存 + PostgreSQL 持久化 + 应用层 JsonNode diff + version CAS 乐观锁

**数据表**：device_shadow（JSONB 存储 reported/desired/delta），放在 openiot_device 数据库

**存储策略**：
- 读路径：Redis 缓存优先 → 未命中查 PostgreSQL → 回填 Redis（TTL 1h）
- reported 写路径：更新 Redis → 异步写 PostgreSQL
- desired 写路径：PostgreSQL CAS 更新（乐观锁）→ 更新 Redis → 计算 delta

**Delta 计算**：应用层 Jackson JsonNode 遍历 diff，不用数据库函数（可维护性优先）

**乐观锁**：`WHERE version = :expected` CAS 更新，冲突返回 409，最多重试 3 次

**Delta 下发时机**：
1. 设备上线事件（Kafka 消费）→ 检查 delta → 非空则下发
2. desired 更新时设备在线 → 立即下发
3. 设备主动 `$shadow/get` 请求 → 兜底机制

**Redis Key**：`device:shadow:{deviceId}`（TTL 1h，活跃设备自动续期）

---

## 6. OpenFeign 服务间调用

### Decision: common-feign-api 集中定义 + Resilience4j 熔断 + RequestInterceptor 上下文传递

**模块结构**：新建 `common/common-feign-api` 模块
- `client/`：TenantFeignClient、DeviceFeignClient 接口定义
- `dto/`：跨服务传输对象（与 Entity 解耦）
- `interceptor/`：TenantContextInterceptor（传递 X-Tenant-Id/X-User-Id/X-User-Role 请求头）

**依赖**：`spring-cloud-starter-openfeign` + `spring-cloud-starter-loadbalancer`（BOM 已管理版本）

**上下文传递**：Feign RequestInterceptor 从 TenantContext(TransmittableThreadLocal) 读取租户信息注入请求头，下游 TenantContextFilter 无需改动

**熔断降级**：复用 Resilience4j，Feign 调用失败 → fallback 从 Redis 缓存降级读取

**Alternatives Considered**:
- 各服务独立定义 Feign Client：解耦更彻底但维护成本高，项目仅 6 个服务不适用
- gRPC：性能更好但增加序列化复杂度和学习曲线，当前阶段 YAGNI

---

## 7. 前端 CRUD 补全

### Decision: 复用现有组件体系 + Element Plus 表单组件 + 玻璃拟态样式

前端新增/编辑功能均复用现有的 Element Plus Dialog + Form 组件，遵循 CLAUDE.md 中的玻璃拟态设计规范。API 层统一抽取到 `src/api/` 目录（当前仅 rule.ts 抽了 API 层，其余散落在组件中）。

---

## 8. Nacos 配置中心

### Decision: 启用 Nacos Config + @RefreshScope 动态刷新

项目已引入 `spring-cloud-starter-alibaba-nacos-config` 但未启用。启用方式：各服务 bootstrap.yml 配置 Nacos Config 地址和 data-id，敏感配置（数据库密码、Redis 密码）仍保留本地，业务参数（限流阈值、缓存 TTL）迁移到 Nacos。
