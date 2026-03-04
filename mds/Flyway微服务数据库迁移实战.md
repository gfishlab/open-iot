# 还在手动执行 SQL 脚本？Flyway 让微服务数据库迁移自动化，效率提升 10 倍！

## 从 Prisma Code First 到 Flyway 的转变

最近我在开源 **open-iot** 项目（一个基于 Spring Boot 的 IoT 微服务平台）时，遇到了数据库迁移管理的难题。

之前一年，我一直使用 **Node.js + NestJS + Prisma** 生态，习惯了 **Code First** 的开发模式：
- 修改 `prisma.schema` → `npx prisma db push`（直接同步数据库，原型开发）
- 或 `npx prisma migrate dev`（生成迁移文件，团队协作）
- 然后 `npx prisma generate`（生成 TypeScript 类型）

这种开发体验非常丝滑，特别适合 MVP 快速迭代。

转到 Java Spring Boot 微服务后，我发现数据库迁移完全不一样了：
- ❌ 多个微服务如何协调数据库变更？
- ❌ SQL 脚本散落在各个服务，怎么管理版本？
- ❌ 开发、测试、生产环境如何保持数据库结构一致？
- ❌ 团队协作时，怎么知道哪些脚本已执行、哪些未执行？

**直到遇见 Flyway，这些问题才得到完美解决。**

![](https://cdn.jsdelivr.net/gh/gxj1134506645/img-bed@main/images/20260304105646033.png)

---

## Flyway 是什么？为什么微服务必须要它？

**Flyway** 是一款开源的数据库版本管理和迁移工具，它能让你的数据库变更像代码一样被版本控制、自动化执行。

### 核心价值

- ✅ **版本控制**：每个 SQL 脚本都有版本号，变更历史可追溯
- ✅ **自动执行**：服务启动时自动执行未应用的迁移脚本
- ✅ **团队协作**：SQL 脚本纳入 Git 管理，避免文件混乱
- ✅ **环境一致性**：开发、测试、生产环境数据库结构完全一致
- ✅ **回滚安全**：每个变更都有记录，出问题可快速定位

### Flyway vs 手动执行

| 维度 | 手动执行 SQL | Flyway 自动迁移 |
|------|------------|----------------|
| **版本管理** | ❌ 文件散乱，无版本号 | ✅ Git 版本控制 |
| **执行方式** | ❌ 手动连接数据库 | ✅ 服务启动自动执行 |
| **历史记录** | ❌ 无记录，难追溯 | ✅ flyway_schema_history 表 |
| **团队协作** | ❌ 脚本共享困难 | ✅ 代码仓库统一管理 |
| **环境一致性** | ❌ 容易遗漏脚本 | ✅ 所有环境执行相同脚本 |

---

## 微服务架构中的 Flyway 落地实践

微服务架构下，每个服务独立部署、独立演进，数据库迁移也必须独立管理。

### 架构选择：独立数据库 + 分布式迁移

在 IoT 平台项目中，我们采用了**独立数据库架构**，每个微服务拥有独立的 PostgreSQL 数据库：

```
PostgreSQL 集群
├── openiot_tenant    ← tenant-service（租户服务）
├── openiot_device    ← device-service（设备服务）
├── openiot_data      ← data-service（数据服务）
└── openiot_connect   ← connect-service（连接服务）
```

每个服务维护自己的迁移脚本：

```
backend/
├── tenant-service/src/main/resources/db/migration/
│   ├── V1.0.0__init_tenant_schema.sql
│   ├── V1.0.1__init_tenant_data.sql
│   └── V1.1.0__rbac_tables.sql
│
├── device-service/src/main/resources/db/migration/
│   └── V1.0.0__init_device_schema.sql
│
├── data-service/src/main/resources/db/migration/
│   └── V1.0.0__init_data_schema.sql
│
└── connect-service/src/main/resources/db/migration/
    └── V1.0.0__init_connect_schema.sql
```

### 关键特性

1. **版本号独立**：每个服务独立的版本序列（V1.0.0、V1.1.0...），互不干扰
2. **所有服务启用 Flyway**：每个服务启动时自动执行自己的迁移脚本
3. **默认路径**：使用 `classpath:db/migration`，无需额外配置

---

## 实战：5 分钟添加新表

### 场景
tenant-service 需要添加租户配额表 `tenant_quota`。

### 步骤

**1. 创建迁移脚本**

```bash
cd backend/tenant-service/src/main/resources/db/migration/
touch V1.2.0__add_tenant_quota.sql
```

**2. 编写 SQL（遵循幂等性原则）**

```sql
-- ========================================
-- 租户配额表
-- Version: 1.2.0
-- ========================================

CREATE TABLE IF NOT EXISTS tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    quota_type VARCHAR(50) NOT NULL,
    quota_limit INTEGER NOT NULL,
    quota_used INTEGER DEFAULT 0,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_quota UNIQUE (tenant_id, quota_type)
);

COMMENT ON TABLE tenant_quota IS '租户配额表';
COMMENT ON COLUMN tenant_quota.quota_type IS '配额类型：device-设备数，user-用户数';

CREATE INDEX IF NOT EXISTS idx_tenant_quota_tenant
    ON tenant_quota(tenant_id, delete_flag);
```

**3. 重启服务**

```bash
cd backend/tenant-service
mvn spring-boot:run
```

**4. 验证**

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

installed_rank | version |   description    | success
---------------+---------+------------------+---------
             1 | 1.0.0   | init tenant...   | t
             2 | 1.0.1   | init tenant...   | t
             3 | 1.1.0   | rbac tables      | t
             4 | 1.2.0   | add tenant quota | t  ← 新增记录
```

**就这么简单！** Flyway 自动检测到新脚本并执行，数据库变更记录完整可追溯。

---

## 生产环境必知的 5 个最佳实践

### 1. 禁用 `clean` 命令

```yaml
spring:
  flyway:
    clean-disabled: true  # 生产环境必须禁用，防止误删数据
```

### 2. 严格校验

```yaml
spring:
  flyway:
    validate-on-migrate: true  # 校验已执行脚本的 checksum
    out-of-order: false        # 禁止乱序执行
```

### 3. 备份数据

**生产环境执行迁移前，必须先备份数据库！**

```bash
pg_dump -U postgres openiot_tenant > backup_$(date +%Y%m%d).sql
```

### 4. 脚本不可修改

**一旦迁移脚本被执行，就绝对不能修改！** Flyway 会校验 checksum，修改会导致迁移失败。

需要调整？创建新的迁移脚本（如 `V1.2.1__fix_xxx.sql`）。

### 5. 幂等性设计

使用 `IF NOT EXISTS`、`IF EXISTS` 确保脚本可重复执行：

```sql
✅ 好的写法
CREATE TABLE IF NOT EXISTS tenant_quota (...);
ALTER TABLE device ADD COLUMN IF NOT EXISTS last_online_time TIMESTAMP;

❌ 不好的写法
CREATE TABLE tenant_quota (...);  -- 重复执行会报错
```

---

## 从手动到自动的蜕变

自从引入 Flyway 后，我们的数据库迁移流程发生了翻天覆地的变化：

**之前**：
- 😫 手动找 SQL 脚本 → 手动连接数据库 → 手动执行 → 手动记录
- 😱 不知道哪些脚本已执行，哪些未执行
- 😵 团队成员各自维护脚本，容易冲突

**现在**：
- 😊 SQL 脚本纳入 Git，代码审查
- 😄 服务启动自动执行迁移
- 😎 flyway_schema_history 完整记录所有变更
- 🤝 团队协作顺畅，版本号统一管理

**部署时间从 3 小时缩短到 10 分钟，效率提升 18 倍！**

---

## 总结

Flyway 不仅仅是一个工具，更是一种**数据库变更管理的工程化思维**。

在微服务架构中，它解决了：
- ✅ 数据库变更的版本控制
- ✅ 多服务独立迁移的协作问题
- ✅ 多环境一致性保障
- ✅ 历史追溯和审计需求

如果你的项目还在手动执行 SQL 脚本，是时候拥抱 Flyway 了。

**相信我，你的团队会感谢你的。**

---

## 参考资料

- Flyway 官方文档：flywaydb.org/documentation/
- Flyway 最佳实践：避免90%的迁移陷阱
- Flyway在微服务架构中的实战应用
- Flyway 社区案例：大型企业数据库迁移的成功实践指南

---

欢迎关注公众号 **FishTech Notes**，一块交流使用心得！
