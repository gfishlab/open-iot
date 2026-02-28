# MongoDB 认证失败问题修复指南

## 问题描述

data-service 启动时报错：
```
MongoSecurityException: Exception authenticating MongoCredential
Command failed with error 18 (AuthenticationFailed): 'Authentication failed.'
```

## 解决方案

### 方案一：使用 MongoDB Shell 创建用户（推荐）

1. **打开命令行，连接到 MongoDB**

   ```bash
   mongosh
   ```

2. **执行用户创建脚本**

   方式 1：直接执行脚本文件
   ```bash
   mongosh < backend/scripts/mongodb-init-user.js
   ```

   方式 2：在 MongoDB Shell 中逐行执行
   ```javascript
   use admin;

   db.createUser({
     user: "openiot",
     pwd: "openiot123",
     roles: [
       { role: "readWrite", db: "openiot" },
       { role: "dbAdmin", db: "openiot" },
       { role: "userAdmin", db: "openiot" }
     ]
   });

   use openiot;
   db.createCollection("device_raw_event");
   ```

### 方案二：使用 MongoDB Compass（图形化工具）

1. **打开 MongoDB Compass**
2. **连接到 MongoDB**（默认：mongodb://localhost:27017）
3. **打开 MongoDB Shell**（点击底部 "MONGOSH" 标签）
4. **执行以下命令**：

   ```javascript
   use admin;

   db.createUser({
     user: "openiot",
     pwd: "openiot123",
     roles: [
       { role: "readWrite", db: "openiot" },
       { role: "dbAdmin", db: "openiot" },
       { role: "userAdmin", db: "openiot" }
     ]
   });
   ```

### 方案三：使用 IntelliJ IDEA 数据库工具

1. **打开 IDEA 的 Database 工具窗口**
2. **添加 MongoDB 数据源**
   - Host: localhost
   - Port: 27017
   - Database: admin
3. **打开控制台，执行用户创建脚本**
4. **右键点击 `backend/scripts/mongodb-init-user.js` → Run**

## 验证修复

### 1. 验证用户是否创建成功

在 MongoDB Shell 中执行：

```javascript
use admin;
db.getUser("openiot");
```

应该看到类似输出：
```json
{
  "_id" : "admin.openiot",
  "userId" : ...,
  "user" : "openiot",
  "db" : "admin",
  "roles" : [
    { "role" : "readWrite", "db" : "openiot" },
    { "role" : "dbAdmin", "db" : "openiot" },
    { "role" : "userAdmin", "db" : "openiot" }
  ]
}
```

### 2. 验证连接

使用新用户连接 MongoDB：

```bash
mongosh "mongodb://openiot:openiot123@127.0.0.1:27017/openiot?authSource=admin"
```

如果能成功连接，说明用户配置正确。

### 3. 重启 data-service

```bash
# 重新启动 data-service
# IDEA 中重启，或使用命令：
# mvn spring-boot:run -pl backend/data-service
```

## 配置说明

### application.yml 配置更新

```yaml
spring:
  data:
    mongodb:
      # authSource=admin 表示用户认证信息存储在 admin 数据库
      uri: mongodb://openiot:openiot123@127.0.0.1:27017/openiot?authSource=admin
      auto-index-creation: true
```

**重要参数说明：**
- `authSource=admin`：指定认证数据库为 admin（MongoDB 默认将用户存储在 admin 数据库）
- `openiot`：连接的目标数据库
- `openiot:openiot123`：用户名和密码

## 常见问题

### Q1: 如果用户已存在，如何重置密码？

```javascript
use admin;
db.changeUserPassword("openiot", "openiot123");
```

### Q2: 如何删除用户重新创建？

```javascript
use admin;
db.dropUser("openiot");

// 然后重新执行创建脚本
db.createUser({
  user: "openiot",
  pwd: "openiot123",
  roles: [
    { role: "readWrite", db: "openiot" },
    { role: "dbAdmin", db: "openiot" },
    { role: "userAdmin", db: "openiot" }
  ]
});
```

### Q3: 如何查看 MongoDB 认证方式？

```javascript
use admin;
db.system.users.find({user: "openiot"});
```

### Q4: 如果 MongoDB 没有启用认证怎么办？

编辑 MongoDB 配置文件（通常在 `/etc/mongod.conf` 或 Windows 的安装目录下）：

```yaml
security:
  authorization: enabled
```

然后重启 MongoDB 服务。

## 用户权限说明

本脚本创建的用户具有以下权限：
- **readWrite**：读写 openiot 数据库
- **dbAdmin**：管理 openiot 数据库（创建索引、查看统计等）
- **userAdmin**：管理 openiot 数据库用户

**生产环境建议**：
- 使用更严格的权限控制
- 使用强密码
- 限制 IP 访问
