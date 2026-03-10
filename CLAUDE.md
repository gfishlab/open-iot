# open-iot Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-25

## Active Technologies
- JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.2.3 (003-iot-core-platform)

### 后端
- JDK 21 (LTS，支持虚拟线程) + Spring Boot 3.x, Spring Cloud Alibaba, Kafka, Netty, MyBatis Plus, Sa-Token
- 分布式锁：Redisson
- 分布式事务：Alibaba Seata（AT 模式）

### 前端
- Vue 3 + Vite + TypeScript
- UI 框架：Element Plus
- CSS 框架：Tailwind CSS
- 状态管理：Pinia
- HTTP 客户端：Axios
- UI设计工具：UI/UX Pro Max skill（已安装）

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

---

## 前端 UI 设计规范

### 玻璃拟态设计风格 (Glassmorphism)

本项目前端采用**轻量玻璃拟态 + 渐变动画**设计风格，为用户提供现代化、流畅的视觉体验。

#### 设计原则

1. **玻璃拟态效果 (Glassmorphism)**
   - 半透明背景：`rgba(255, 255, 255, 0.1)` + `backdrop-filter: blur(12px)`
   - 微妙边框：半透明白边框 + 悬停发光效果
   - 悬停反馈：背景加深 + 边框发光 + 阴影增强
   - 圆角统一：12px (卡片), 8px (按钮), 4px (标签)

2. **渐变动画**
   - 过渡时长：200-300ms (流畅不卡顿)
   - 动画类型：颜色过渡、阴影过渡、transform 过渡(谨慎使用)
   - 缓动函数：`cubic-bezier(0.4, 0, 0.2, 1)` 或 `ease`
   - 避免布局抖动：transform 动画使用 `transform: translateZ(0)` 加速

3. **配色方案**
   - **主色调**：Indigo (#6366f1) → Purple (#8b5cf6) 渐变
   - **背景色**：Slate (#0f172a → #1e293b → #334155)
   - **文字色**：
     - 主文本：Slate-100 (#f1f5f9)
     - 次要文本：Slate-400 (#94a3b8)
     - 短文本：Slate-500 (#64748b)
     - 强调色：Indigo-400 (#a5b4fc) 或 Purple-400 (#c4b5fe)
   - **状态色**：
     - 成功：Emerald (#10b981)
     - 警告：Amber (#f59e0b)
     - 错误：Red (#ef4444)
     - 信息：Blue (#3b82f6)

#### CSS 样式文件

所有玻璃拟态样式定义在 `frontend/src/styles/glassmorphism.css`，已在 `main.ts` 中全局引入。

**可用的 CSS 类：**

| 类名 | 用途 | 示例 |
|------|------|------|
| `.glass-card` | 卡片/容器 | `<el-card class="glass-card">` |
| `.glass-header` | 顶部导航栏 | `<header class="glass-header">` |
| `.glass-button` | 按钮 | `<el-button class="glass-button">` |
| `.glass-tag` | 标签 | `<el-tag class="glass-tag">` |
| `.glass-pagination` | 分页器 | `<el-pagination class="glass-pagination">` |
| `.glass-menu-item` | 菜单项 | `<div class="glass-menu-item">` |
| `.glass-sidebar` | 侧边栏 | `<aside class="glass-sidebar">` |
| `.glass-stat-card` | 统计卡片 | `<div class="glass-stat-card">` |
| `.pulse` | 脉冲动画 | `<div class="pulse">` |

#### CSS 变量

```css
:root {
  --glass-blur: blur(12px);
  --glass-border: 1px solid rgba(255, 255, 255, 0.08);
  --glass-border-hover: 1px solid rgba(99, 102, 241, 0.4);
}
```

#### 使用示例

**1. 玻璃卡片**
```vue
<template>
  <el-card class="glass-card">
    <div class="card-header">
      <span class="card-title">设备列表</span>
      <el-button class="glass-button" type="primary">新增设备</el-button>
    </div>
    <el-table :data="devices" class="glass-table">
      <!-- 表格内容 -->
    </el-table>
  </el-card>
</template>
```

**2. 玻璃按钮**
```vue
<template>
  <el-button class="glass-button" @click="handleSearch">搜索</el-button>
  <el-button class="glass-button" type="primary" @click="handleAdd">新增</el-button>
  <el-button class="glass-button" type="danger" @click="handleDelete">删除</el-button>
</template>
```

**3. 玻璃标签**
```vue
<template>
  <el-tag class="glass-tag" :type="online ? 'success' : 'info'">
    {{ online ? '在线' : '离线' }}
  </el-tag>
</template>
```

**4. 脉冲动画（用于状态指示器）**
```vue
<template>
  <div class="status-indicator pulse"></div>
</template>

<style scoped>
.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
}
</style>
```

#### 布局规范

**列表页面布局填满（重要）**

为确保表格在数据较少时也能填满整个卡片区域，消除底部空白，所有列表页面必须遵循以下布局规范：

```vue
<template>
  <div class="list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">设备列表</span>
          <el-button class="glass-button" type="primary">新增设备</el-button>
        </div>
      </template>

      <el-table :data="items">
        <!-- 表格内容 -->
      </el-table>

      <div class="pagination-wrap">
        <el-pagination class="glass-pagination" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* 页面容器 - 必须设置 height: 100% 和 flex 布局 */
.list-page {
  padding: 20px;
  height: 100%;  /* 填满父容器高度 */
  display: flex;
  flex-direction: column;
}

/* 卡片容器 - 自动扩展填满剩余空间 */
.el-card {
  flex: 1;  /* 自动扩展填满剩余空间 */
  display: flex;
  flex-direction: column;
}

/* 卡片内容区域 - flex 布局 */
.el-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;  /* 关键：允许 flex 子元素收缩 */
}

/* 表格 - 自动扩展并设置最小高度 */
.el-table {
  flex: 1;  /* 表格自动扩展 */
  min-height: 400px;  /* 最小高度保证，即使数据少也填满 */
}

/* 分页容器 - 固定在底部 */
.pagination-wrap {
  margin-top: auto;  /* 分页器固定在底部 */
  flex-shrink: 0;  /* 不会被压缩 */
  display: flex;
  justify-content: flex-end;
}
</style>
```

**关键要点：**

✅ **即使数据很少，表格也会自动扩展填满整个卡片区域**
✅ **分页器始终固定在底部**
✅ **消除了底部空白区域**
✅ **响应式自适应不同屏幕尺寸**

**为什么需要这样设计？**

1. **用户体验**：避免大量空白区域，界面更紧凑美观
2. **视觉一致性**：所有列表页面保持统一的视觉效果
3. **响应式**：在不同屏幕尺寸下都能正常显示
4. **数据量变化**：无论数据多少，布局都保持稳定

**应用到所有列表页面：**

以下页面必须遵循此布局规范：
- `frontend/src/views/product/ProductList.vue`
- `frontend/src/views/device/DeviceList.vue`
- `frontend/src/views/alert/AlertList.vue`
- `frontend/src/views/rule/RuleList.vue`
- `frontend/src/views/tenant/TenantList.vue`
- 其他所有包含表格的列表页面

#### 设计检查清单

在提交前端代码前，请确保：

- [ ] 所有 Element Plus 组件都添加了对应的 `.glass-` 前缀类
- [ ] 按钮添加了 `class="glass-button"`
- [ ] 卡片添加了 `class="glass-card"`
- [ ] 标签添加了 `class="glass-tag"`
- [ ] 分页器添加了 `class="glass-pagination"`
- [ ] 表格容器设置了 `flex: 1` 和 `min-height`
- [ ] 页面容器设置了 `height: 100%` 和 `display: flex`
- [ ] 悬停效果平滑（200-300ms 过渡）
- [ ] 颜色使用项目统一的配色方案
- [ ] 深色模式下文字对比度足够（WCAG AA 标准）

#### 禁止事项

- ❌ 使用纯色背景（不透明）
- ❌ 硬编码颜色值（应使用 CSS 变量或 Tailwind 类）
- ❌ 过度使用 transform 动画（避免布局抖动）
- ❌ 使用 emoji 作为图标（应使用 SVG 图标）
- ❌ 悬停时使用 `scale` 变换（会导致布局偏移）
- ❌ 使用 `cursor: default` 在可点击元素上
- ❌ 在深色模式下使用浅色文字（对比度不足）

#### 性能优化

1. **动画性能**
   - 使用 `transform` 和 `opacity` 进行动画（GPU 加速）
   - 避免动画 `width`、`height`、`margin`、`padding`（触发重排）
   - 使用 `will-change` 提示浏览器优化

2. **模糊效果**
   - `backdrop-filter: blur(12px)` 性能开销中等
   - 仅在悬停时使用模糊效果可提升性能
   - 移动端可降级为半透明背景（不使用模糊）

3. **渐变动画**
   - 使用 `background-position` 动画（性能好）
   - 避免频繁改变 `background-size`

#### 参考资源

- 设计文档：`docs/plans/2026-03-09-glassmorphism-design.md`
- 样式文件：`frontend/src/styles/glassmorphism.css`
- 示例组件：
  - `frontend/src/layouts/MainLayout.vue`
  - `frontend/src/views/product/ProductList.vue`
  - `frontend/src/views/device/DeviceList.vue`
  - `frontend/src/views/alert/AlertList.vue`

---

## 前后端联调规范

### 架构原则

**所有前后端联调 MUST 通过 Gateway 网关服务进行路由，禁止直接调用后端微服务。**

```
┌─────────┐      ┌─────────┐      ┌─────────────────┐
│  前端    │ ──→  │ Gateway │ ──→  │  后端微服务集群   │
│  Vue3   │      │  网关   │      │ tenant/device/  │
└─────────┘      └─────────┘      └─────────────────┘
```

### 为什么必须走网关

1. **统一认证**：Gateway 统一处理 Sa-Token 认证，注入租户信息到请求头
2. **路由分发**：Gateway 负责将请求路由到正确的微服务
3. **租户隔离**：Gateway 自动注入 `X-Tenant-Id`、`X-User-Id`、`X-User-Role` 请求头
4. **安全防护**：Gateway 提供统一的 API 网关安全策略

### 开发环境配置

#### 前端代理配置 (vite.config.ts)

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Gateway 地址
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})
```

#### Gateway 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: tenant-service
          uri: lb://tenant-service
          predicates:
            - Path=/api/v1/auth/**,/api/v1/tenants/**,/api/v1/users/**
        - id: device-service
          uri: lb://device-service
          predicates:
            - Path=/api/v1/products/**,/api/v1/devices/**
```

### 请求流程

```
1. 前端发起请求 → /api/v1/products
2. Vite 代理转发 → http://localhost:8080/api/v1/products
3. Gateway 认证  → 验证 Sa-Token，解析用户信息
4. Gateway 注入  → 添加 X-Tenant-Id, X-User-Id, X-User-Role 请求头
5. Gateway 路由  → 转发到 device-service
6. 后端处理     → TenantContextFilter 从请求头读取租户信息
```

### 禁止事项

- ❌ 前端直接调用 `http://localhost:8081/api/products`（绕过 Gateway）
- ❌ 前端直接调用 `http://localhost:8086/api/v1/auth/login`（绕过 Gateway）
- ❌ 在生产环境暴露微服务端口（只暴露 Gateway 端口）

### 端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | 唯一对外暴露的端口 |
| tenant-service | 8086 | 内部服务，不对外 |
| device-service | 8081 | 内部服务，不对外 |
| data-service | 8082 | 内部服务，不对外 |
| connect-service | 8083 | 内部服务，不对外 |
| rule-service | 8088 | 内部服务，不对外 |
| 前端 (dev) | 5173 | Vite 开发服务器 |

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

## Spring MVC Controller 参数规范

### @RequestParam 和 @PathVariable 必须显式指定 name 属性

**问题背景**：Java 编译器默认不保留参数名到字节码，Spring MVC 在运行时无法通过反射获取参数名，导致 `IllegalArgumentException`。

**错误示例**：
```java
// ❌ 编译后参数名丢失，Spring 无法识别
@GetMapping("/alerts")
public ApiResponse<Page<Alert>> getAlerts(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size) {
```

**正确写法**：
```java
// ✅ 显式指定 name 属性
@GetMapping("/alerts")
public ApiResponse<Page<Alert>> getAlerts(
    @RequestParam(name = "page", defaultValue = "1") int page,
    @RequestParam(name = "size", defaultValue = "10") int size) {
```

**规范要求**：
- ✅ 所有 `@RequestParam` 必须指定 `name` 属性
- ✅ 所有 `@PathVariable` 必须指定 `name` 或 `value` 属性（Spring 6+ 可省略，但建议显式指定）
- ✅ 即使参数名与方法参数名相同，也要显式指定

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

---

## IoT 平台核心功能

### 规则引擎

#### 解析规则

解析规则用于将设备上报的原始数据（二进制、十六进制、自定义格式）转换为标准 JSON 格式。

**支持的脚本类型：**
- **JavaScript (GraalJS)**：使用 JavaScript 语法编写解析脚本
- **Aviator 表达式**：轻量级表达式引擎，适合简单数据转换

**解析规则配置示例：**
```javascript
// JavaScript 解析脚本示例
function parse(payload, metadata) {
    // payload: 原始数据（Buffer 或字符串）
    // metadata: 元数据（设备信息、产品信息等）

    // 解析二进制数据
    var temperature = payload.readInt8(0);
    var humidity = payload.readUInt8(1);

    // 返回标准格式
    return {
        temperature: temperature / 10.0,
        humidity: humidity,
        deviceId: metadata.deviceId,
        timestamp: Date.now()
    };
}
```

**Aviator 表达式示例：**
```java
// 简单表达式：temperature * 0.1
// 条件表达式：temperature > 50 ? 'high' : 'normal'
```

#### 映射规则

映射规则用于将解析后的数据映射到物模型定义的属性、事件。

**映射规则配置示例：**
```json
{
  "propertyMappings": [
    {
      "sourceField": "temperature",
      "targetProperty": "Temperature",
      "dataType": "double",
      "transformExpression": "value / 10.0"
    },
    {
      "sourceField": "humidity",
      "targetProperty": "Humidity",
      "dataType": "int"
    }
  ],
  "eventMappings": [
    {
      "sourceField": "alarmCode",
      "targetEvent": "AlarmEvent",
      "conditionExpression": "alarmCode != 0"
    }
  ]
}
```

### 设备服务调用

设备服务调用允许平台主动调用设备定义的服务（如开关、重启、配置下发等）。

**调用方式：**
- **同步调用**：阻塞等待设备响应，最多等待 30 秒
- **异步调用**：立即返回 invokeId，通过 invokeId 查询执行结果

**API 接口：**

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/devices/{id}/services/{serviceIdentifier}` | POST | 异步调用设备服务 |
| `/api/v1/devices/{id}/services/{serviceIdentifier}/sync` | POST | 同步调用设备服务 |
| `/api/v1/devices/service-invocations/{invocationId}` | GET | 查询调用状态 |

**调用请求示例：**
```json
{
  "inputParams": {
    "speed": 100,
    "direction": "forward"
  },
  "invokeType": "async",
  "timeout": 30
}
```

**调用状态：**
- `pending`：待处理
- `calling`：调用中
- `success`：成功
- `failed`：失败
- `timeout`：超时

### 告警管理

告警管理提供告警规则配置、告警触发、告警处理等功能。

**告警级别：**
- `info`：信息告警
- `warning`：警告告警
- `critical`：严重告警
- `emergency`：紧急告警

**告警状态：**
- `pending`：待处理
- `processing`：处理中
- `resolved`：已解决
- `ignored`：已忽略

**API 接口：**

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/alerts` | GET | 分页查询告警列表 |
| `/api/v1/alerts/{alertId}` | GET | 查询告警详情 |
| `/api/v1/alerts/{alertId}/handle` | PUT | 处理告警 |
| `/api/v1/alerts/batch-handle` | PUT | 批量处理告警 |
| `/api/v1/alerts/statistics` | GET | 告警统计 |
| `/api/v1/alerts/pending` | GET | 查询待处理告警 |

### InfluxDB 时序数据存储

设备上报的属性数据、状态数据使用 InfluxDB 进行时序存储。

**数据模型：**
- `DevicePropertyPoint`：设备属性数据点
- `DeviceStatusPoint`：设备状态数据点
- `DeviceEventPoint`：设备事件数据点

**查询接口：**
```java
// 查询设备属性历史数据
List<DevicePropertyPoint> queryProperties(
    String deviceId,
    String propertyIdentifier,
    Instant start,
    Instant end
);
```

### API 限流配置

为防止恶意请求和系统过载，所有 API 接口默认启用基于 IP 的限流保护。

**配置项：**
```yaml
openiot:
  rate-limit:
    enabled: true                     # 是否启用限流
    permits-per-second: 10            # 每个 IP 每秒最多请求数
    cache-expire-minutes: 10          # IP 缓存过期时间（分钟）
```

**限流响应：**
- HTTP 状态码：429 Too Many Requests
- 响应头：
  - `X-RateLimit-Limit`：每秒最大请求数
  - `X-RateLimit-Remaining`：剩余请求数
  - `Retry-After`：建议重试等待时间（秒）

### Swagger API 文档

所有服务提供 Swagger UI 在线 API 文档。

**访问地址：**
- Device Service: `http://localhost:8081/swagger-ui.html`
- Gateway Service: `http://localhost:8080/swagger-ui.html`

**API 分组：**
1. 产品管理
2. 设备管理
3. 物模型管理
4. 设备控制
5. 告警管理
6. 规则引擎
7. 实时推送（SSE）
8. 数据重放
9. 设备轨迹

<!-- MANUAL ADDITIONS END -->
