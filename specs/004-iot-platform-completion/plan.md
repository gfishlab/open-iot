# Implementation Plan: IoT 平台功能补全

**Branch**: `004-iot-platform-completion` | **Date**: 2026-03-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-iot-platform-completion/spec.md`

## Summary

为 Open-IoT 平台补全 8 项缺失功能，按优先级分三层实施：P1（单元测试/集成测试、网关熔断限流、MQTT 完整接入）→ P2（OTA 升级、设备影子、OpenFeign、前端 CRUD）→ P3（Nacos 配置中心）。核心技术方案：Resilience4j 熔断 + Redis 限流、HiveMQ MQTT Client + EMQX Webhook、Testcontainers 集成测试、乐观锁设备影子。

## Technical Context

**Language/Version**: JDK 21 (LTS) + Spring Boot 3.2.3
**Primary Dependencies**: Spring Cloud 2023.0.0, Spring Cloud Alibaba 2022.0.0.0, MyBatis Plus 3.5.5, Sa-Token, Netty, Kafka, Resilience4j (新增), HiveMQ MQTT Client (新增), OpenFeign (新增), Testcontainers (新增)
**Storage**: PostgreSQL (openiot_device/tenant/data/connect) + Redis + InfluxDB + MongoDB
**Testing**: JUnit 5 + Mockito + Testcontainers 1.19.5 + JaCoCo
**Target Platform**: Linux/Docker (开发环境 Windows 11 + Docker Desktop)
**Project Type**: 微服务 Web 应用 (6 个后端服务 + Vue 3 前端)
**Performance Goals**: MQTT 端到端 < 500ms, 网关降级 < 3s, 影子 delta 同步 < 10s
**Constraints**: 不引入 Sentinel（版本兼容风险），不引入 gRPC（YAGNI），固件存储用本地文件系统
**Scale/Scope**: 约 200+ Java 源文件，前端约 30 个 Vue 组件

## Constitution Check

*GATE: Pre-Phase 0 — PASS*

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 多租户强制 | PASS | 所有新表包含 tenant_id，MyBatis Plus TenantLineHandler 自动注入 |
| II. 数据演进安全 | PASS | 新表通过 Flyway 迁移脚本 V1.7.0/V1.8.0 创建 |
| III. 渐进式微服务 | PASS | 新增 common-feign-api 模块，OpenFeign 补齐同步通信能力 |
| IV. 协议可扩展 | PASS | MQTT 接入通过 connect-service 统一接管，不在业务代码中写死协议 |
| V. 垂直数据链路优先 | PASS | MQTT 接入打通完整链路（EMQX → connect-service → Kafka → data-service） |
| VI. 学习范围强制 | PASS | 熔断降级、MQTT、OTA、设备影子均为核心学习目标 |
| VII. 认证边界 | PASS | EMQX 认证走 HTTP Authenticator → MqttAuthController，Feign 调用通过 RequestInterceptor 传递上下文 |
| VIII. 严格 RBAC | PASS | 新增 OTA/影子相关权限到 sys_permission 表 |
| IX. 可观测性强制 | PASS | 新功能集成 BusinessMetrics，MQTT Client 暴露健康检查端点 |
| X. AI 辅助开发 | PASS | 测试先行，增量实施，KISS 优先 |

*GATE: Post-Phase 1 Design — PASS（无违规）*

## Project Structure

### Documentation (this feature)

```text
specs/004-iot-platform-completion/
├── plan.md              # 本文件
├── spec.md              # 功能规范
├── research.md          # Phase 0 研究报告
├── data-model.md        # Phase 1 数据模型
├── quickstart.md        # Phase 1 快速启动
├── contracts/
│   └── api-contracts.md # Phase 1 API 契约
├── checklists/
│   └── requirements.md  # 规范质量检查
└── tasks.md             # Phase 2 任务分解（/speckit.tasks 生成）
```

### Source Code (repository root)

```text
backend/
├── common/
│   ├── common-feign-api/                    # [新增] Feign 接口定义 + DTO
│   │   └── src/main/java/com/openiot/common/feign/
│   │       ├── client/                      # TenantFeignClient, DeviceFeignClient
│   │       ├── dto/                         # TenantDTO, DeviceDTO, ProductDTO
│   │       └── interceptor/                 # TenantContextInterceptor
│   └── ... (existing modules)
├── gateway-service/
│   └── src/main/java/com/openiot/gateway/
│       ├── config/
│       │   ├── CircuitBreakerConfig.java    # [新增] Resilience4j 配置
│       │   └── RateLimitConfig.java         # [新增] Redis 限流 KeyResolver
│       └── controller/
│           └── FallbackController.java      # [新增] 熔断降级响应
├── connect-service/
│   └── src/main/java/com/openiot/connect/
│       ├── mqtt/
│       │   ├── MqttClientManager.java       # [新增] HiveMQ MQTT Client 管理
│       │   └── MqttWebhookController.java   # [新增] EMQX Webhook 接收
│       └── ...
├── device-service/
│   └── src/main/java/com/openiot/device/
│       ├── ota/                             # [新增] OTA 升级模块
│       │   ├── controller/
│       │   ├── service/
│       │   ├── entity/
│       │   ├── mapper/
│       │   └── vo/
│       ├── shadow/                          # [新增] 设备影子模块
│       │   ├── controller/
│       │   ├── service/
│       │   ├── entity/
│       │   ├── mapper/
│       │   └── vo/
│       └── src/main/resources/db/migration/
│           ├── V1.7.0__add_ota_tables.sql   # [新增]
│           └── V1.8.0__add_device_shadow.sql # [新增]
│
│   [测试文件]
├── */src/test/java/                         # [新增] 各服务单元测试
│   ├── tenant-service/  (AuthServiceTest, PermissionServiceTest)
│   ├── device-service/  (ProductServiceTest, DeviceServiceTest, ThingModelServiceTest, AlertServiceTest)
│   ├── connect-service/ (BinaryParserTest, JsonPathParserTest, RegexParserTest, JavaScriptParserTest, MappingRuleEngineTest)
│   └── data-service/    (AlarmServiceTest, DeviceStatusServiceTest)

frontend/
├── src/
│   ├── api/                                 # [新增/补全] 统一 API 层
│   │   ├── device.ts
│   │   ├── product.ts
│   │   ├── tenant.ts
│   │   ├── user.ts
│   │   ├── rule.ts (已有)
│   │   ├── ota.ts
│   │   └── shadow.ts
│   └── views/
│       ├── device/DeviceForm.vue            # [新增] 设备新增/编辑表单
│       ├── tenant/TenantForm.vue            # [新增] 租户新增/编辑表单
│       ├── user/UserForm.vue                # [新增] 用户新增/编辑表单
│       └── rule/
│           ├── ParseRuleList.vue            # [新增] 解析规则管理
│           └── MappingRuleList.vue          # [新增] 映射规则管理
```

**Structure Decision**: 沿用现有微服务架构，新增 `common-feign-api` 公共模块。OTA 和设备影子作为 device-service 的子包（非独立服务），符合 Constitution III 渐进式微服务原则。

## Complexity Tracking

> 无 Constitution 违规，无需填写。
