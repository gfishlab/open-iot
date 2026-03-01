# open-iot Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-25

## Active Technologies

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

- 001-mvp-core: Added JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token

<!-- MANUAL ADDITIONS START -->
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

## 分布式规范

### 分布式锁（Redisson）

**使用场景：** 跨服务/跨实例的资源竞争，如设备状态更新、配额检查

**推荐写法：**
```java
@Autowired
private RedissonClient redissonClient;

public void updateDeviceStatus(String deviceId) {
    String lockKey = "device:lock:" + deviceId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 尝试获取锁，等待3秒，持有10秒
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            // 业务逻辑
        } else {
            throw new BusinessException("获取锁失败");
        }
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

**注意事项：**
- 锁 Key 命名：`业务域:锁类型:唯一标识`
- 必须使用 `tryLock` 带超时，避免死锁
- 必须在 `finally` 中释放锁
- 优先使用 `tryLock` 而非 `lock`（避免阻塞过久）

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
<!-- MANUAL ADDITIONS END -->
