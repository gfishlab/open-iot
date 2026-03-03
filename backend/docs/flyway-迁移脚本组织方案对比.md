# Flyway 迁移脚本组织方案对比

> 本文档详细对比两种 Flyway 迁移脚本组织方案，帮助你做出架构决策

## 目录

- [1. 两种架构方案对比](#1-两种架构方案对比)
- [2. 当前项目分析](#2-当前项目分析)
- [3. 方案 1：共享数据库 + 集中式迁移](#3-方案-1共享数据库--集中式迁移)
- [4. 方案 2：独立数据库 + 分布式迁移](#4-方案-2独立数据库--分布式迁移)
- [5. 混合方案（推荐）](#5-混合方案推荐)
- [6. 迁移实战指南](#6-迁移实战指南)
- [7. 决策树](#7-决策树)

---

## 1. 两种架构方案对比

### 方案对比表

| 维度 | 方案 1：共享数据库 | 方案 2：独立数据库 |
|------|------------------|------------------|
| **数据库数量** | 1 个 | N 个（每服务一个） |
| **迁移脚本位置** | `infrastructure/sql/migrations/` | 各服务 `resources/db/migration/` |
| **Flyway 启用** | 仅 1 个服务 | 所有服务 |
| **版本号管理** | 统一递增 | 独立管理（可重复） |
| **服务耦合度** | 高（共享数据库） | 低（独立数据库） |
| **跨服务事务** | 简单（本地事务） | 复杂（分布式事务） |
| **运维成本** | 低（1 个数据库） | 高（N 个数据库） |
| **数据隔离** | 差（同一数据库） | 好（独立数据库） |
| **适用场景** | MVP、中小型项目 | 大型微服务、企业级 |

---

## 2. 当前项目分析

### 2.1 当前架构

```
当前状态：共享数据库架构

PostgreSQL (openiot)
├── tenant            ← tenant-service
├── sys_user          ← tenant-service
├── device            ← device-service
├── device_trajectory ← device-service / data-service
└── (future tables...)

所有服务连接同一个数据库：jdbc:postgresql://127.0.0.1:5432/openiot
```

### 2.2 表与服务映射

| 表名 | 主要使用服务 | 说明 |
|------|------------|------|
| `tenant` | tenant-service | 租户表 |
| `sys_user` | tenant-service | 系统用户表 |
| `device` | device-service | 设备表 |
| `device_trajectory` | data-service | 设备轨迹表 |

**关键问题**：
- ❓ `device_trajectory` 被多个服务使用，如何拆分？
- ❓ `tenant_id` 在多个表中作为外键，如何保证数据一致性？

---

## 3. 方案 1：共享数据库 + 集中式迁移

### 3.1 架构图

```
┌─────────────────────────────────────────┐
│      PostgreSQL (openiot)               │
│  ┌────────────────────────────────────┐ │
│  │ flyway_schema_history              │ │
│  │ (统一记录所有迁移)                  │ │
│  └────────────────────────────────────┘ │
│                                         │
│  ┌────────────────────────────────────┐ │
│  │ tenant, sys_user, device,         │ │
│  │ device_trajectory, ...            │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
            ↑         ↑         ↑
            │         │         │
    ┌───────┴──┐  ┌──┴────┐  ┌─┴──────┐
    │ tenant-  │  │device-│  │  data- │
    │ service  │  │service│  │service │
    │          │  │       │  │        │
    │ ✅ Flyway│  │❌ 禁用│  │❌ 禁用 │
    └──────────┘  └───────┘  └────────┘
```

### 3.2 迁移脚本组织

```
infrastructure/sql/migrations/
├── README.md
├── V1.0.0__init_schema.sql          # 所有表（tenant、sys_user、device、device_trajectory）
├── V1.0.1__init_data.sql            # 初始数据
├── V1.1.0__rbac_tables.sql          # RBAC 权限表（tenant-service 用）
├── V1.2.0__add_device_config.sql    # 设备配置表（device-service 用）
├── V1.3.0__add_alarm_config.sql     # 告警配置表（device-service 用）
└── V1.4.0__add_tenant_quota.sql     # 租户配额表（tenant-service 用）
```

**版本号规则**：
- ✅ 统一递增：`1.0.0 → 1.1.0 → 1.2.0 → 1.3.0 → ...`
- ✅ 不区分服务，按时间顺序递增
- ✅ 谁先开发谁先提交

### 3.3 Flyway 配置

#### tenant-service（启用）

```yaml
# backend/tenant-service/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot
    username: openiot
    password: openiot123

flyway:
  enabled: true
  locations: filesystem:infrastructure/sql/migrations
  baseline-on-migrate: true
  validate-on-migrate: true
  out-of-order: false
```

#### device-service、data-service（禁用）

```yaml
# backend/device-service/src/main/resources/application.yml
# backend/data-service/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot  # 同一个数据库
    username: openiot
    password: openiot123

flyway:
  enabled: false  # ❌ 禁用，避免重复迁移
```

### 3.4 工作流程

#### 场景 1：device-service 需要新增告警配置表

**Step 1：开发者在 infrastructure 创建迁移脚本**

```bash
cd infrastructure/sql/migrations
touch V1.3.0__add_alarm_config.sql
```

**Step 2：编写 SQL**

```sql
-- V1.3.0__add_alarm_config.sql
CREATE TABLE IF NOT EXISTS alarm_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    alarm_name VARCHAR(100) NOT NULL,
    -- ...
);

COMMENT ON TABLE alarm_config IS '告警配置表';
-- ...
```

**Step 3：重启 tenant-service**

```bash
cd backend/tenant-service
mvn spring-boot:run  # Flyway 自动执行 V1.3.0 脚本
```

**Step 4：device-service 使用新表**

```java
// device-service 创建 Entity
@Data
@TableName("alarm_config")
public class AlarmConfig {
    // ...
}
```

### 3.5 优缺点

#### 优点

- ✅ **简单直观**：所有脚本集中管理，清晰明了
- ✅ **版本号统一**：不会冲突，按时间递增
- ✅ **运维成本低**：只需维护 1 个数据库
- ✅ **跨服务事务简单**：同一个数据库，本地事务即可
- ✅ **开发效率高**：适合 MVP 快速迭代

#### 缺点

- ❌ **服务耦合**：共享数据库，服务不独立
- ❌ **不符合微服务原则**：数据层耦合
- ❌ **安全性低**：所有服务都能访问所有表
- ❌ **扩展性差**：无法独立扩展数据库
- ❌ **单点故障**：数据库挂了，所有服务都挂

---

## 4. 方案 2：独立数据库 + 分布式迁移

### 4.1 架构图

```
┌──────────────────┐
│ openiot_tenant   │ ← tenant-service 专属
│ ┌──────────────┐ │
│ │flyway_history│ │
│ └──────────────┘ │
│ ┌──────────────┐ │
│ │tenant        │ │
│ │sys_user      │ │
│ └──────────────┘ │
└──────────────────┘
        ↑
        │
  ┌─────┴─────┐
  │  tenant-  │
  │  service  │
  │✅ Flyway  │
  └───────────┘

┌──────────────────┐
│ openiot_device   │ ← device-service 专属
│ ┌──────────────┐ │
│ │flyway_history│ │
│ └──────────────┘ │
│ ┌──────────────┐ │
│ │device        │ │
│ │alarm_config  │ │
│ └──────────────┘ │
└──────────────────┘
        ↑
        │
  ┌─────┴─────┐
  │  device-  │
  │  service  │
  │✅ Flyway  │
  └───────────┘

┌──────────────────┐
│ openiot_data     │ ← data-service 专属
│ ┌──────────────┐ │
│ │flyway_history│ │
│ └──────────────┘ │
│ ┌──────────────┐ │
│ │device_       │ │
│ │trajectory    │ │
│ └──────────────┘ │
└──────────────────┘
        ↑
        │
  ┌─────┴─────┐
  │   data-   │
  │  service  │
  │✅ Flyway  │
  └───────────┘
```

### 4.2 迁移脚本组织

#### 方案 2A：各服务独立管理（推荐）

```
backend/
├── tenant-service/
│   └── src/main/resources/db/migration/
│       ├── V1.0.0__init_tenant_schema.sql    # tenant、sys_user
│       ├── V1.0.1__init_tenant_data.sql
│       ├── V1.1.0__add_tenant_quota.sql
│       └── V1.2.0__add_tenant_config.sql
│
├── device-service/
│   └── src/main/resources/db/migration/
│       ├── V1.0.0__init_device_schema.sql    # device
│       ├── V1.1.0__add_device_config.sql
│       ├── V1.2.0__add_alarm_config.sql
│       └── V1.3.0__add_alarm_history.sql
│
└── data-service/
    └── src/main/resources/db/migration/
        ├── V1.0.0__init_data_schema.sql      # device_trajectory
        ├── V1.1.0__add_event_index.sql
        └── V1.2.0__partition_trajectory.sql
```

**版本号规则**：
- ✅ 每个服务独立管理版本号
- ✅ tenant-service: `1.0.0 → 1.1.0 → 1.2.0`
- ✅ device-service: `1.0.0 → 1.1.0 → 1.2.0 → 1.3.0`（可以和 tenant 重复）
- ✅ 每个数据库有独立的 `flyway_schema_history` 表

#### 方案 2B：集中管理但按服务分目录

```
infrastructure/sql/migrations/
├── tenant-service/
│   ├── V1.0.0__init_tenant_schema.sql
│   ├── V1.0.1__init_tenant_data.sql
│   └── V1.1.0__add_tenant_quota.sql
│
├── device-service/
│   ├── V1.0.0__init_device_schema.sql
│   ├── V1.1.0__add_device_config.sql
│   └── V1.2.0__add_alarm_config.sql
│
└── data-service/
    ├── V1.0.0__init_data_schema.sql
    └── V1.1.0__add_event_index.sql
```

**Flyway 配置**：

```yaml
# tenant-service/application.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot_tenant
  flyway:
    enabled: true
    locations: filesystem:infrastructure/sql/migrations/tenant-service
```

```yaml
# device-service/application.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot_device
  flyway:
    enabled: true
    locations: filesystem:infrastructure/sql/migrations/device-service
```

### 4.3 Flyway 配置（所有服务启用）

```yaml
# 所有服务的 application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration  # 各服务自己的脚本
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 4.4 工作流程

#### 场景 1：device-service 需要新增告警配置表

**Step 1：开发者在 device-service 创建迁移脚本**

```bash
cd backend/device-service/src/main/resources/db/migration
touch V1.2.0__add_alarm_config.sql
```

**Step 2：编写 SQL**

```sql
-- V1.2.0__add_alarm_config.sql
CREATE TABLE IF NOT EXISTS alarm_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,  -- ⚠️ 关联 tenant-service 的租户
    alarm_name VARCHAR(100) NOT NULL,
    -- ...
);
```

**Step 3：重启 device-service**

```bash
cd backend/device-service
mvn spring-boot:run  # Flyway 自动执行 V1.2.0 脚本
```

**Step 4：跨服务数据一致性（关键问题）**

```java
// ❌ 问题：如何保证 tenant_id 存在？

// 方式 1：应用层校验（推荐）
public void createAlarmConfig(AlarmConfigDTO dto) {
    // 调用 tenant-service 检查租户是否存在
    Tenant tenant = tenantServiceClient.getTenant(dto.getTenantId());
    if (tenant == null) {
        throw new BusinessException("租户不存在");
    }

    // 创建告警配置
    alarmConfigMapper.insert(dto);
}

// 方式 2：分布式事务（Seata）
@GlobalTransactional
public void createTenantWithAlarmConfig(TenantDTO tenantDTO, AlarmConfigDTO alarmDTO) {
    // 1. tenant-service 创建租户
    tenantService.create(tenantDTO);

    // 2. device-service 创建告警配置
    alarmConfigService.create(alarmDTO);
}
```

### 4.5 优缺点

#### 优点

- ✅ **服务独立**：符合微服务原则
- ✅ **数据隔离**：安全性高
- ✅ **独立部署**：服务可以独立部署、扩展
- ✅ **版本号独立**：不会冲突
- ✅ **故障隔离**：一个数据库挂了，不影响其他服务
- ✅ **扩展性好**：可以独立扩展数据库

#### 缺点

- ❌ **跨服务事务复杂**：需要分布式事务（Seata）
- ❌ **运维成本高**：需要维护多个数据库
- ❌ **开发复杂度增加**：需要处理跨服务数据一致性
- ❌ **查询复杂**：跨库查询需要使用 API 或数据同步

---

## 5. 混合方案（推荐）

### 5.1 阶段 1：MVP 阶段（当前）- 共享数据库

```
当前状态：
- 所有服务连接 openiot 数据库
- 采用方案 1：集中式迁移管理

配置：
✅ infrastructure/sql/migrations/        （保留）
❌ backend/tenant-service/db/migration/  （删除）
✅ tenant-service 启用 Flyway
✅ 其他服务禁用 Flyway
```

**优点**：
- ✅ 快速迭代，适合 MVP
- ✅ 运维成本低
- ✅ 跨服务事务简单

### 5.2 阶段 2：服务拆分 - 独立数据库

```
未来状态：
- 每个服务独立数据库
- 采用方案 2：分布式迁移管理

配置：
✅ backend/tenant-service/db/migration/  （恢复）
✅ backend/device-service/db/migration/  （新增）
✅ backend/data-service/db/migration/    （新增）
✅ 所有服务启用 Flyway
❌ infrastructure/sql/migrations/         （废弃或归档）
```

**迁移步骤**：
1. 创建独立数据库
2. 拆分迁移脚本
3. 数据迁移
4. 修改配置
5. 测试验证

---

## 6. 迁移实战指南

### 6.1 从共享数据库迁移到独立数据库

#### Step 1：分析表依赖关系

```bash
# 查看当前所有表
psql -U openiot -d openiot -c "\dt"

# 分析表依赖
psql -U openiot -d openiot -c "
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY';
"
```

#### Step 2：创建独立数据库

```sql
-- 创建 3 个独立数据库
CREATE DATABASE openiot_tenant;
CREATE DATABASE openiot_device;
CREATE DATABASE openiot_data;

-- 创建用户和权限
CREATE USER openiot_tenant WITH PASSWORD 'tenant123';
CREATE USER openiot_device WITH PASSWORD 'device123';
CREATE USER openiot_data WITH PASSWORD 'data123';

GRANT ALL PRIVILEGES ON DATABASE openiot_tenant TO openiot_tenant;
GRANT ALL PRIVILEGES ON DATABASE openiot_device TO openiot_device;
GRANT ALL PRIVILEGES ON DATABASE openiot_data TO openiot_data;
```

#### Step 3：拆分迁移脚本

**原始脚本**：`infrastructure/sql/migrations/V1.0.0__init_schema.sql`

```sql
-- 包含所有表
CREATE TABLE tenant (...);
CREATE TABLE sys_user (...);
CREATE TABLE device (...);
CREATE TABLE device_trajectory (...);
```

**拆分后**：

```bash
# tenant-service
backend/tenant-service/src/main/resources/db/migration/V1.0.0__init_tenant_schema.sql
```
```sql
CREATE TABLE tenant (...);
CREATE TABLE sys_user (...);
```

```bash
# device-service
backend/device-service/src/main/resources/db/migration/V1.0.0__init_device_schema.sql
```
```sql
CREATE TABLE device (...);
```

```bash
# data-service
backend/data-service/src/main/resources/db/migration/V1.0.0__init_data_schema.sql
```
```sql
CREATE TABLE device_trajectory (...);
```

#### Step 4：数据迁移

```sql
-- 从 openiot 迁移数据到 openiot_tenant
INSERT INTO openiot_tenant.tenant
SELECT * FROM openiot.tenant;

INSERT INTO openiot_tenant.sys_user
SELECT * FROM openiot.sys_user;

-- 从 openiot 迁移数据到 openiot_device
INSERT INTO openiot_device.device
SELECT * FROM openiot.device;

-- 从 openiot 迁移数据到 openiot_data
INSERT INTO openiot_data.device_trajectory
SELECT * FROM openiot.device_trajectory;
```

#### Step 5：修改配置

```yaml
# tenant-service/application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot_tenant
    username: openiot_tenant
    password: tenant123
  flyway:
    enabled: true
    locations: classpath:db/migration
```

```yaml
# device-service/application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/openiot_device
    username: openiot_device
    password: device123
  flyway:
    enabled: true
    locations: classpath:db/migration
```

#### Step 6：处理跨服务数据一致性

**方式 1：应用层校验（推荐）**

```java
// device-service 创建设备时，检查租户是否存在
public void createDevice(DeviceDTO dto) {
    // 调用 tenant-service API 检查租户
    Tenant tenant = tenantClient.getTenant(dto.getTenantId());
    if (tenant == null) {
        throw new BusinessException("租户不存在");
    }

    // 创建设备
    deviceMapper.insert(dto);
}
```

**方式 2：分布式事务（Seata）**

```java
@GlobalTransactional
public void createTenantWithDevice(TenantDTO tenantDTO, DeviceDTO deviceDTO) {
    // 1. tenant-service 创建租户
    tenantService.create(tenantDTO);

    // 2. device-service 创建设备
    deviceService.create(deviceDTO);
}
```

---

## 7. 决策树

```
开始
  │
  ├─ 是否需要跨服务事务？
  │   ├─ 是 → 方案 1（共享数据库）
  │   └─ 否 → 继续
  │
  ├─ 团队规模？
  │   ├─ 小于 5 人 → 方案 1（共享数据库）
  │   └─ 大于 5 人 → 继续
  │
  ├─ 是否需要数据隔离？
  │   ├─ 否 → 方案 1（共享数据库）
  │   └─ 是 → 方案 2（独立数据库）
  │
  ├─ 是否有专业 DBA？
  │   ├─ 否 → 方案 1（共享数据库）
  │   └─ 是 → 方案 2（独立数据库）
  │
  └─ 项目阶段？
      ├─ MVP → 方案 1（共享数据库）
      └─ 成熟期 → 方案 2（独立数据库）
```

---

## 8. 总结建议

### 当前阶段（MVP）

**推荐方案 1**：共享数据库 + 集中式迁移

**配置**：
- ✅ 保留 `infrastructure/sql/migrations/`
- ❌ 删除 `backend/tenant-service/src/main/resources/db/migration/`
- ✅ tenant-service 启用 Flyway
- ✅ 其他服务禁用 Flyway

**原因**：
- ✅ 快速迭代，适合 MVP
- ✅ 运维成本低
- ✅ 团队规模小，不需要数据隔离

### 未来阶段（服务拆分）

**推荐方案 2**：独立数据库 + 分布式迁移

**配置**：
- ✅ 各服务维护自己的 `db/migration/` 目录
- ✅ 所有服务启用 Flyway
- ✅ 每个服务独立数据库

**迁移时机**：
- 团队规模扩大（超过 10 人）
- 需要数据隔离（多租户、安全要求）
- 服务需要独立部署和扩展

---

## 9. FAQ

### Q1：旧目录 `backend/tenant-service/src/main/resources/db/migration/` 要删除吗？

**A**：
- **当前阶段**：✅ 删除（避免混淆）
- **未来阶段**：✅ 恢复（独立数据库时使用）

### Q2：版本号冲突怎么办？

**A**：
- **方案 1**：不会冲突（统一管理）
- **方案 2**：不会冲突（每个服务独立版本号）

### Q3：跨服务数据一致性怎么保证？

**A**：
- **方案 1**：本地事务（简单）
- **方案 2**：分布式事务（Seata）+ 应用层校验

### Q4：如何从方案 1 迁移到方案 2？

**A**：参考第 6 节"迁移实战指南"

---

**文档版本**：v1.0.0
**最后更新**：2026-03-03
**维护者**：OpenIoT Team
