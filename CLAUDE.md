# open-iot Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-25

## Active Technologies
- JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.2.3 (003-iot-core-platform)

### 后端
- JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token
- 分布式锁：Redisson
- 分布式事务：Alibaba Seata（AT 模式）

### 前端
- Vue 3 + Vite + Element Plus
- UI设计工具：UI/UX Pro Max skill（已安装）
- 状态管理：Pinia

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

### 后端命令
# Add commands for JDK 21 (LTS，支持虚拟线程)

### 前端命令
# UI设计：使用UI/UX Pro Max skill进行前端界面设计和代码生成
# 启动前端开发服务器：cd frontend && npm run dev

## Code Style

JDK 21 (LTS，支持虚拟线程): Follow standard conventions

## Recent Changes
- 003-iot-core-platform: Added JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.2.3

- 001-mvp-core: Added JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token

<!-- MANUAL ADDITIONS START -->

## 数据库架构设计

### 独立数据库架构

本项目采用**独立数据库架构**，每个微服务拥有独立的 PostgreSQL 数据库：

| 服务 | 数据库名 | 说明 |
|------|---------|------|
| tenant-service | `openiot_tenant` | 租户、用户、RBAC 权限表 |
| device-service | `openiot_device` | 设备、产品、属性、事件、服务调用表 |
| data-service | `openiot_data` | 设备轨迹、历史数据表 |
| connect-service | `openiot_connect` | 设备连接会话表 |

每个服务独立管理自己的 Flyway 迁移脚本，位于 `src/main/resources/db/migration/` 目录。

### PostgreSQL MCP 配置

项目已配置 PostgreSQL MCP 服务，支持直接通过 MCP 操作数据库：

- **mcp__postgres-tenant__query**: 操作 `openiot_tenant` 数据库
- **mcp__postgres-device__query**: 操作 `openiot_device` 数据库
- **mcp__postgres-data__query**: 操作 `openiot_data` 数据库
- **mcp__postgres-connect__query**: 操作 `openiot_connect` 数据库

**使用示例**：
```
# 查询设备数据库中的表
mcp__postgres-device__query: "SELECT * FROM device LIMIT 10"
```

### Flyway 配置规范

每个服务独立配置 Flyway：

```yaml
# application.yml
spring.flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
```

### 迁移脚本命名规范

**格式**：`V<版本号>__<描述>.sql`

**示例**：
```
V1.0.0__init_schema.sql          # 初始化表结构
V1.0.1__init_data.sql            # 初始化数据
V1.1.0__add_device_config.sql    # 添加设备配置表
V1.1.1__add_index_for_device.sql # 添加设备表索引
V1.2.0__add_alarm_table.sql      # 添加告警表
```

**注意事项**：
- ✅ 版本号必须递增，不能重复
- ✅ 使用双下划线 `__` 分隔版本号和描述
- ✅ 描述使用下划线命名（snake_case）
- ✅ 脚本一旦执行，**不可修改**（Flyway 会校验 checksum）
- ❌ 禁止删除已执行的迁移脚本
- ❌ 禁止修改已执行的迁移脚本内容

### 新增表的流程

1. **在 tenant-service 中创建迁移脚本**：
   ```bash
   # 进入 tenant-service 迁移目录
   cd backend/tenant-service/src/main/resources/db/migration

   # 创建新的迁移脚本（版本号递增）
   touch V1.3.0__add_new_table.sql
   ```

2. **编写 SQL DDL**：
   ```sql
   -- V1.3.0__add_new_table.sql
   CREATE TABLE IF NOT EXISTS alarm_config (
       id BIGSERIAL PRIMARY KEY,
       tenant_id BIGINT NOT NULL,
       alarm_name VARCHAR(100) NOT NULL,
       status CHAR(1) DEFAULT '1',
       delete_flag CHAR(1) DEFAULT '0',
       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       create_by BIGINT,
       update_by BIGINT
   );

   COMMENT ON TABLE alarm_config IS '告警配置表';
   COMMENT ON COLUMN alarm_config.id IS '主键';
   -- ... 其他注释

   -- 创建索引
   CREATE INDEX IF NOT EXISTS idx_alarm_config_tenant
       ON alarm_config(tenant_id, delete_flag);
   ```

3. **重启 tenant-service**（Flyway 自动执行迁移）

4. **验证迁移结果**：
   ```sql
   -- 查询 Flyway 迁移历史
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;

   -- 验证表是否创建成功
   \d alarm_config
   ```

### 环境配置

#### **开发环境（dev）**
```yaml
spring.flyway:
  enabled: true
  clean-disabled: false  # 允许清理（仅开发环境）
```

#### **生产环境（prod）**
```yaml
spring.flyway:
  enabled: true
  clean-disabled: true   # 禁止清理，防止误删数据
  validate-on-migrate: true  # 严格校验
```

### 最佳实践

1. **集中管理**：所有表的 DDL 都在 tenant-service 中管理，避免分散
2. **版本控制**：迁移脚本纳入 Git 版本管理
3. **向后兼容**：修改表结构时，确保向后兼容（避免破坏现有功能）
4. **测试先行**：在开发环境测试迁移脚本，确认无误后再应用到生产
5. **备份数据**：生产环境执行迁移前，先备份数据库
6. **索引优化**：新建表时，同时创建必要的索引
7. **注释完整**：每个表和字段都要添加 COMMENT

### 禁止操作

- ❌ 在 device-service、data-service 中启用 Flyway
- ❌ 在代码中直接执行 DDL（CREATE TABLE、ALTER TABLE 等）
- ❌ 修改已执行的迁移脚本
- ❌ 删除 `flyway_schema_history` 表中的记录
- ❌ 在生产环境使用 `flyway.clean()`

---

## 数据库操作规范

### MyBatis Plus 使用规范

- **单表操作 MUST 优先使用 Lambda 语法糖 API**（`LambdaQueryWrapper`、`LambdaUpdateWrapper`）
- 禁止硬编码字段名字符串，使用 `User::getName` 而非 `"name"`
- 复杂查询可使用 XML 或 `@Select` 注解，但单表 CRUD 必须用 Lambda

**推荐写法：**
```java
// 查询
lambdaQuery()
    .eq(Device::getTenantId, tenantId)
    .eq(Device::getStatus, 1)
    .list();

// 更新
lambdaUpdate()
    .eq(Device::getId, deviceId)
    .set(Device::getStatus, 0)
    .update();

// 删除
lambdaUpdate()
    .eq(Device::getTenantId, tenantId)
    .remove();
```

**禁止写法：**
```java
// 硬编码字段名
new QueryWrapper<Device>().eq("tenant_id", tenantId);

// 字符串拼接
wrapper.apply("tenant_id = " + tenantId);
```

---

## Redis 使用规范

### RedisTemplate vs Redisson 分工

**RedisTemplate**（简单缓存操作）：
- String 操作：缓存对象、计数器、分布式 ID
- Hash 操作：存储对象属性、购物车
- List 操作：消息队列、最新列表
- Set 操作：标签、关注关系
- ZSet 操作：排行榜、延时队列

**Redisson**（分布式高级功能）：
- **分布式锁**：RLock（推荐用于跨服务资源竞争）
- **分布式对象**：RMap、RList、RSet 等
- **布隆过滤器**：RBloomFilter
- **限流器**：RRateLimiter
- **发布订阅**：RTopic

### 序列化配置

✅ **已配置 Jackson JSON 序列化**：
- Key：String 序列化（可读性强）
- Value：Jackson JSON 序列化（支持完整对象存储）
- 支持 Java 8 时间类型（LocalDateTime、LocalDate 等）
- **无需手动序列化，直接存储 POJO 对象**

### RedisTemplate 使用规范

**推荐写法：**
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 或使用工具类
@Autowired
private RedisUtil redisUtil;

// 1. 存储对象（自动 JSON 序列化）
User user = new User(1L, "张三", "zhangsan@example.com");
redisUtil.set("user:1", user, 3600);  // 缓存 1 小时

// 2. 获取对象（类型安全）
User cachedUser = redisUtil.get("user:1", User.class);

// 3. 计数器
redisUtil.increment("device:online:count");

// 4. Hash 操作
redisUtil.hSet("device:status", "device001", "online");
String status = (String) redisUtil.hGet("device:status", "device001");

// 5. 批量删除
redisUtil.delete(Arrays.asList("key1", "key2", "key3"));
```

**禁止写法：**
```java
// ❌ 手动序列化（已配置 Jackson，无需手动）
redisTemplate.opsForValue().set("user:1", JSON.toJSONString(user));

// ❌ 使用 StringRedisTemplate 存储对象（会导致序列化问题）
@Autowired
private StringRedisTemplate stringRedisTemplate;
```

### Redisson 分布式锁规范

**使用场景：** 跨服务/跨实例的资源竞争，如设备状态更新、配额检查、订单支付

**推荐写法：**
```java
@Autowired
private RedissonClient redissonClient;

public void updateDeviceStatus(String deviceId) {
    // 锁 Key 命名规范：业务域:锁类型:唯一标识
    String lockKey = "device:lock:" + deviceId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 尝试获取锁：等待3秒，持有10秒
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            // 业务逻辑
            Device device = deviceMapper.selectById(deviceId);
            device.setStatus("1");
            deviceMapper.updateById(device);
        } else {
            throw new BusinessException("获取锁失败，请稍后重试");
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException("获取锁被中断", e);
    } finally {
        // 必须在 finally 中释放锁，并检查是否持有锁
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

**注意事项：**
- ✅ 锁 Key 命名：`业务域:锁类型:唯一标识`（如 `device:lock:123`）
- ✅ 必须使用 `tryLock(等待时间, 持有时间, 时间单位)`，避免死锁
- ✅ 必须在 `finally` 中释放锁，并检查 `isHeldByCurrentThread()`
- ✅ 优先使用 `tryLock` 而非 `lock`（避免阻塞过久）
- ✅ 持有时间要合理，避免业务执行超时导致锁自动释放
- ❌ 禁止使用 `synchronized` 或 `ReentrantLock`（单机锁，分布式环境无效）

**常见锁类型：**
```java
// 1. 普通可重入锁
RLock lock = redissonClient.getLock("device:lock:123");

// 2. 公平锁（按请求顺序获取）
RLock fairLock = redissonClient.getFairLock("device:fair:123");

// 3. 读写锁（读多写少场景）
RReadWriteLock rwLock = redissonClient.getReadWriteLock("device:rw:123");
RLock readLock = rwLock.readLock();
RLock writeLock = rwLock.writeLock();

// 4. 联锁（同时锁定多个资源）
RLock lock1 = redissonClient.getLock("lock1");
RLock lock2 = redissonClient.getLock("lock2");
RedissonMultiLock multiLock = new RedissonMultiLock(lock1, lock2);
```

### Redis Key 命名规范

**格式：** `业务域:资源类型:唯一标识[:子资源]`

**示例：**
```java
// 用户相关
user:info:123               // 用户信息
user:token:abc123           // 用户 Token
user:permissions:123        // 用户权限列表

// 设备相关
device:info:device001       // 设备信息
device:status:device001     // 设备状态
device:lock:device001       // 设备分布式锁

// 租户相关
tenant:config:1             // 租户配置
tenant:quota:1              // 租户配额

// 统计相关
stats:device:online:count   // 在线设备数
stats:tenant:1:device:count // 租户设备数
```

### 缓存策略

**缓存穿透防护：**
```java
// 缓存空值，设置较短过期时间
if (user == null) {
    redisUtil.set("user:123", new NullValue(), 60);  // 缓存 1 分钟
}
```

**缓存击穿防护（使用 Redisson 锁）：**
```java
public User getUserWithCache(Long userId) {
    String cacheKey = "user:" + userId;

    // 1. 查询缓存
    User user = redisUtil.get(cacheKey, User.class);
    if (user != null) {
        return user;
    }

    // 2. 获取分布式锁，防止缓存击穿
    RLock lock = redissonClient.getLock("lock:user:cache:" + userId);
    try {
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            // 3. 再次检查缓存（Double Check）
            user = redisUtil.get(cacheKey, User.class);
            if (user != null) {
                return user;
            }

            // 4. 查询数据库
            user = userMapper.selectById(userId);

            // 5. 写入缓存
            if (user != null) {
                redisUtil.set(cacheKey, user, 3600);  // 缓存 1 小时
            }

            return user;
        }
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    return null;
}
```

---

## 分布式规范

### 分布式事务（Seata AT 模式）

**使用场景：** 跨服务的写操作，如设备注册+初始化、租户创建+资源分配

**配置要求：**
- 每个数据库需要 `undo_log` 表
- 全局事务注解：`@GlobalTransactional`

**推荐写法：**
```java
@GlobalTransactional(rollbackFor = Exception.class)
public void createTenantWithResources(TenantDTO dto) {
    // 1. 创建租户（tenant-service）
    tenantService.create(dto);

    // 2. 初始化资源（其他服务）
    resourceService.initDefaultResources(dto.getId());
}
```

**注意事项：**
- 仅在跨服务调用时使用，单服务内用 `@Transactional`
- 全局事务 ID 会自动通过 Seata 上下文传递
- 避免长事务，尽量缩小全局事务范围
- 不支持嵌套 `@GlobalTransactional`

---

## Git 提交规范

### 提交信息格式

所有 Git 提交信息 MUST 使用**中文简体**，遵循以下格式：

```
<类型>: <简短描述>

<详细说明>（可选）
```

### 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | feat: 添加设备管理模块 |
| `fix` | Bug 修复 | fix: 修复租户登录失败问题 |
| `docs` | 文档更新 | docs: 更新 API 接口文档 |
| `refactor` | 代码重构 | refactor: 优化设备查询性能 |
| `test` | 测试相关 | test: 添加设备服务单元测试 |
| `chore` | 构建/工具变更 | chore: 更新依赖版本 |
| `style` | 代码格式调整 | style: 格式化代码缩进 |
| `perf` | 性能优化 | perf: 优化数据库查询性能 |

### 提交规范要求

- ✅ **必须使用中文简体**描述提交信息
- ✅ 标题行不超过 50 个字符
- ✅ 使用祈使语气（"添加"而非"添加了"）
- ✅ 提交内容应聚焦单一改动
- ✅ 复杂改动需添加详细说明
- ❌ 禁止使用英文提交信息
- ❌ 禁止一次性提交过多不相关改动

### 示例

**好的提交：**
```
feat: 添加设备管理列表页面

- 实现设备列表查询功能
- 添加设备新增、编辑、删除操作
- 集成 Element Plus 表格组件
```

**不好的提交：**
```
update code
fix bug
修改了一些东西
```

### Phase 完成提交规范

**每个 Phase 阶段完成后 MUST 自动执行 git commit 和 push**：

1. **提交时机**：Phase 的所有任务完成后，立即提交
2. **提交内容**：包含该 Phase 的所有代码、配置、文档变更
3. **提交格式**：遵循上述提交信息格式，标题注明 Phase 编号
4. **自动推送**：提交后立即 push 到远程仓库

**示例：**
```
feat: 完成 IoT 平台核心功能 Phase 2-3

Phase 2: 基础架构层
- 添加 GraalJS、Aviator、InfluxDB 依赖
- 创建 rule-service 模块
- 创建实体类和 Mapper 接口

Phase 3: 产品-设备层级管理
- ProductService：产品 CRUD
- ProductController：REST API
- DeviceService 增强：产品关联、设备认证
```

**流程：**
1. Phase 任务全部完成 → 2. git add . → 3. git commit → 4. git push

<!-- MANUAL ADDITIONS END -->
