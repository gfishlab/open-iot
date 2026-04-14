# Open-IoT 项目入口

## 项目定位

学习型开源物联网平台，目标系统化学习 IoT 后端核心能力。

## 技术基线

### 后端
- JDK 21 + Spring Boot 3.2.3 + Spring Cloud 2023.0.0 + Spring Cloud Alibaba 2022.0.0.0
- MyBatis Plus 3.5.5, Sa-Token, Netty, Kafka, Resilience4j, HiveMQ MQTT Client, OpenFeign, Testcontainers
- 分布式锁：Redisson | 分布式事务：Seata AT

### 前端
- Vue 3 + Vite + TypeScript + Element Plus + Tailwind CSS + Pinia + Axios

### 数据存储
- PostgreSQL (openiot_device/tenant/data/connect) + Redis + InfluxDB + MongoDB

## 项目结构

```text
backend/
  common/                  # 公共模块（core, redis, kafka, mongodb, security, observability）
  gateway-service/         # API 网关 (8080) — 唯一对外暴露端口
  tenant-service/          # 租户管理 (8086)
  device-service/          # 设备管理 (8081)
  connect-service/         # 设备接入 - Netty TCP (8083)
  data-service/            # 数据处理 (8082)
  rule-service/            # 规则引擎 (8088)
frontend/                  # Vue 3 + Vite + Element Plus + Tailwind CSS
infrastructure/            # Docker Compose + 配置文件
specs/                     # 功能规格文档
docs/                      # 项目专题规范文档
```

## 关键架构决策

- **数据库**：独立数据库架构，每个服务独立管理自己的 PostgreSQL 数据库和 Flyway 迁移
- **设备接入**：EMQX (MQTT) + Netty TCP + HTTP 三协议
- **可观测性**：LGTM Stack（Prometheus + Loki + Tempo + Grafana）
- **认证鉴权**：Sa-Token + 严格 RBAC 权限模型
- **前后端通信**：所有请求 MUST 经 Gateway 网关路由，禁止前端直连微服务

## 专题规范索引

详细实施规范请查阅对应文档，不在入口文件重复：

| 主题 | 文档 |
|------|------|
| 数据库架构 | docs/数据库架构说明.md |
| 数据库迁移 | docs/Flyway 数据库迁移指南.md |
| 环境配置 | docs/分环境配置总览.md |
| 配置变更记录 | docs/配置优化修改总结.md |
| 可观测性 | docs/LGTM Stack 指南.md |
| Micrometer 实战 | docs/Micrometer 实战落地指南.md |
| MongoDB 认证 | docs/MONGODB_AUTH_FIX.md |
| PostgreSQL MCP | docs/postgresql mcp整合.md |
| 前端 UI 设计 | frontend/src/styles/glassmorphism.css |

## 数据库操作硬性规则

- 单表操作 MUST 使用 Lambda 语法（`lambdaQuery()` / `lambdaUpdate()`），禁止硬编码字段名
- Redis Key 命名：`业务域:资源类型:唯一标识`
- 分布式锁 Key 命名：`业务域:lock:唯一标识`，必须 `tryLock` + `finally` 释放
- 跨服务写操作使用 `@GlobalTransactional`，单服务内用 `@Transactional`

## 前端硬性规则

- UI 风格：玻璃拟态（Glassmorphism），使用 `.glass-card` / `.glass-button` / `.glass-tag` 等 CSS 类
- 列表页必须 `flex` 布局填满，表格设 `flex: 1` + `min-height`，分页器 `margin-top: auto`
- 悬停过渡 200-300ms，禁止 `scale` 变换，禁止硬编码颜色

## 后端硬性规则

- `@RequestParam` / `@PathVariable` MUST 显式指定 `name` 属性
- API 限流默认开启（`openiot.rate-limit.enabled: true`）
- Swagger 文档地址：`http://localhost:<port>/swagger-ui.html`

## Git 规范

### 提交规范
- **必须使用中文简体**
- 格式：`<类型>: <简短描述>`（标题行不超过 50 字符）
- 类型：feat / fix / docs / refactor / test / chore / style / perf
- 每个 Phase 完成后 MUST 提交并推送

### 分支管理
- **严禁删除 `00x-` 开头的功能分支**，合并后永久保留
- 仅允许删除临时修复分支（`fix/xxx`、`hotfix/xxx`）

## Agent 工程运行约定

- `.claude/rules`：任务分类、记忆写入、子代理路由、验证清单等执行细则
- `.claude/hooks`：任务开始、任务结束、失败复盘、提交前检查等关键阶段触发约定
- `.claude/memory`：纠正记录、阶段观察、已学规则、反模式、演化日志等工程记忆
- `.claude/subagents`：`planner`、`executor`、`verifier` 等子代理职责说明

## 记忆协作关系

- 项目内部记忆存放在 `.claude/memory`
- 外部记忆插件可用于恢复跨会话动态记忆
- 项目内部记忆负责项目规则、项目边界和项目级纠偏
- 外部恢复记忆负责补充历史工作上下文
- 当前任务优先以本项目入口文件和项目内部记忆作为项目事实来源
- 外部恢复结果适合作为补充上下文，不替代项目内部规则与文档

## 记忆优先级顺序

1. 本 `CLAUDE.md`
2. `.claude/rules`
3. `.claude/memory`
4. 外部记忆插件恢复出的动态上下文

读取记忆时优先先核对项目内部规则和项目内部记忆，再参考外部恢复结果。

## 默认加载顺序

1. 识别任务作用域、影响路径与模块范围
2. 读取本 `CLAUDE.md`
3. 按任务类型读取 `.claude/rules`
4. 按需读取 `.claude/memory`
5. 判断是否启用子代理分工
6. 执行任务
7. 在关键 hook 阶段完成验证、复盘与沉淀
