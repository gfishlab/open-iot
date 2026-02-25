# Data Model: Open-IoT MVP 核心功能

**Feature**: 001-mvp-core
**Created**: 2026-02-25
**Purpose**: 定义核心数据实体、关系和存储结构

---

## 实体关系图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────────┐
│   Tenant    │       │   Device    │       │ DeviceTrajectory│
│  (租户)     │       │   (设备)    │       │   (设备轨迹)     │
├─────────────┤       ├─────────────┤       ├─────────────────┤
│ id          │◄──┐   │ id          │◄──┐   │ id              │
│ tenantCode  │   │   │ tenantId    │───┘   │ tenantId        │
│ tenantName  │   │   │ deviceCode  │       │ deviceId        │───┐
│ contactEmail│   │   │ deviceName  │       │ latitude        │   │
│ status      │   │   │ deviceToken │       │ longitude       │   │
│ deleteFlag  │   │   │ protocolType│       │ speed           │   │
│ createTime  │   │   │ status      │       │ heading         │   │
│ updateTime  │   │   │ deleteFlag  │       │ eventTime       │   │
│ createBy    │   │   │ createTime  │       │ createTime      │   │
│ updateBy    │   │   │ updateTime  │       └─────────────────┘   │
└─────────────┘   │   │ createBy    │                             │
                  │   │ updateBy    │                             │
                  │   └─────────────┘                             │
                  │                                               │
                  └───────────────────────────────────────────────┘

┌─────────────────┐
│   RawEvent      │
│  (原始事件)      │
│  [MongoDB]      │
├─────────────────┤
│ _id             │
│ tenantId        │
│ deviceId        │
│ eventType       │
│ protocol        │
│ rawPayload      │
│ timestamp       │
│ processed       │
│ processResult   │
│ createTime      │
└─────────────────┘

┌─────────────────┐
│   DeadLetter    │
│  (死信队列)      │
│  [MongoDB]      │
├─────────────────┤
│ _id             │
│ originalEventId │
│ tenantId        │
│ deviceId        │
│ rawPayload      │
│ failureReason   │
│ retryCount      │
│ createTime      │
│ lastRetryTime   │
└─────────────────┘
```

---

## PostgreSQL 表定义

### tenant（租户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| tenant_code | VARCHAR(50) | NOT NULL, UNIQUE | 租户编码 |
| tenant_name | VARCHAR(100) | NOT NULL | 租户名称 |
| contact_email | VARCHAR(100) | | 联系邮箱 |
| status | CHAR(1) | DEFAULT '1' | 状态：0-禁用，1-启用 |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记：0-正常，1-已删除 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**:
- `uk_tenant_code` UNIQUE (tenant_code)
- `idx_tenant_status` (status, delete_flag)

---

### device（设备表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID（FK → tenant.id） |
| device_code | VARCHAR(50) | NOT NULL | 设备编码 |
| device_name | VARCHAR(100) | | 设备名称 |
| device_token | VARCHAR(100) | NOT NULL | 设备认证Token |
| protocol_type | VARCHAR(20) | NOT NULL | 协议类型：MQTT/TCP/HTTP |
| status | CHAR(1) | DEFAULT '1' | 状态：0-禁用，1-启用 |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**:
- `uk_tenant_device` UNIQUE (tenant_id, device_code)
- `idx_device_tenant` (tenant_id)
- `idx_device_token` (device_token)

---

### device_trajectory（设备轨迹表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| device_id | BIGINT | NOT NULL | 设备ID |
| latitude | DECIMAL(10,7) | | 纬度 |
| longitude | DECIMAL(10,7) | | 经度 |
| speed | DECIMAL(5,2) | | 速度(km/h) |
| heading | DECIMAL(5,2) | | 航向角(度) |
| event_time | TIMESTAMP | NOT NULL | 事件时间 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**:
- `idx_trajectory_tenant_device_time` (tenant_id, device_id, event_time)
- `idx_trajectory_time` (event_time)

---

### sys_user（系统用户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| tenant_id | BIGINT | | 租户ID（NULL表示平台管理员） |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码（加密） |
| real_name | VARCHAR(50) | | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | 角色：ADMIN/TENANT_ADMIN |
| status | CHAR(1) | DEFAULT '1' | 状态 |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**索引**:
- `uk_username` UNIQUE (username)
- `idx_user_tenant` (tenant_id)

---

## MongoDB 集合定义

### raw_events（原始事件集合）

```javascript
{
  "_id": ObjectId,
  "eventId": String,           // UUID，事件唯一标识
  "tenantId": String,          // 租户ID
  "deviceId": String,          // 设备ID
  "eventType": String,         // 事件类型：TELEMETRY/STATUS/ALARM
  "protocol": String,          // 协议：MQTT/TCP/HTTP
  "rawPayload": String,        // 原始载荷（Base64编码）
  "parsedPayload": Object,     // 解析后的载荷（可选）
  "timestamp": Date,           // 事件时间
  "processed": Boolean,        // 是否已处理
  "processResult": String,     // 处理结果：SUCCESS/FAILED
  "createTime": Date,
  "updateTime": Date
}

// 索引
db.raw_events.createIndex({ "tenantId": 1, "deviceId": 1, "timestamp": -1 })
db.raw_events.createIndex({ "processed": 1, "timestamp": 1 })
db.raw_events.createIndex({ "eventId": 1 }, { unique: true })
```

### dlq_events（死信队列集合）

```javascript
{
  "_id": ObjectId,
  "originalEventId": String,   // 原始事件ID
  "tenantId": String,
  "deviceId": String,
  "rawPayload": String,        // 原始载荷
  "failureReason": String,     // 失败原因
  "retryCount": Number,        // 重试次数
  "status": String,            // PENDING/RETRYING/RESOLVED
  "createTime": Date,
  "lastRetryTime": Date,
  "resolvedTime": Date         // 解决时间（成功重试后）
}

// 索引
db.dlq_events.createIndex({ "status": 1, "createTime": 1 })
db.dlq_events.createIndex({ "originalEventId": 1 })
```

---

## Redis 数据结构

### 设备在线状态

```
Key: device:status:{tenantId}:{deviceId}
Type: Hash
TTL: 300s (5分钟)

Fields:
  online: "true" | "false"
  lastSeen: timestamp (milliseconds)
  ip: "x.x.x.x"
  protocol: "MQTT" | "TCP"
```

### 实时轨迹缓存

```
Key: device:trajectory:{tenantId}:{deviceId}
Type: Sorted Set (按时间戳排序)
TTL: 3600s (1小时)

Member: JSON字符串
  {
    "lat": 30.1234567,
    "lng": 120.1234567,
    "speed": 25.5,
    "heading": 180.0
  }

Score: timestamp (milliseconds)

限制: 最多保留100个点
```

### 设备Token缓存

```
Key: device:token:{token}
Type: Hash
TTL: 根据Token有效期设置

Fields:
  tenantId: "xxx"
  deviceId: "xxx"
  expireTime: timestamp
```

### 用户会话缓存

```
Key: session:{sessionId}
Type: Hash
TTL: 1800s (30分钟)

Fields:
  userId: "xxx"
  tenantId: "xxx"
  role: "ADMIN" | "TENANT_ADMIN"
```

---

## 数据生命周期

### PostgreSQL 数据

| 表 | 保留策略 | 说明 |
|------|---------|------|
| tenant | 永久 | 核心业务数据 |
| device | 永久 | 核心业务数据 |
| device_trajectory | 90天 | 可按需归档历史数据 |
| sys_user | 永久 | 核心业务数据 |

### MongoDB 数据

| 集合 | 保留策略 | 说明 |
|------|---------|------|
| raw_events | 30天 | 原始数据用于重放，30天后可归档 |
| dlq_events | 永久 | 死信记录用于审计和恢复 |

### Redis 数据

| Key | 保留策略 | 说明 |
|------|---------|------|
| device:status:* | 5分钟 TTL | 无心跳自动过期 |
| device:trajectory:* | 1小时 TTL | 实时数据，超时清除 |
| device:token:* | Token 有效期 | 与Token同步过期 |
| session:* | 30分钟 TTL | 会话超时自动过期 |

---

## Flyway 迁移脚本

### V1.0.0__init_schema.sql

```sql
-- 租户表
CREATE TABLE tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(50) NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100),
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_tenant_code UNIQUE (tenant_code)
);

-- 设备表
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    device_code VARCHAR(50) NOT NULL,
    device_name VARCHAR(100),
    device_token VARCHAR(100) NOT NULL,
    protocol_type VARCHAR(20) NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_tenant_device UNIQUE (tenant_id, device_code)
);

-- 设备轨迹表
CREATE TABLE device_trajectory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    speed DECIMAL(5, 2),
    heading DECIMAL(5, 2),
    event_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 系统用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username UNIQUE (username)
);

-- 创建索引
CREATE INDEX idx_device_tenant ON device(tenant_id);
CREATE INDEX idx_device_token ON device(device_token);
CREATE INDEX idx_trajectory_tenant_device_time ON device_trajectory(tenant_id, device_id, event_time);
CREATE INDEX idx_trajectory_time ON device_trajectory(event_time);
CREATE INDEX idx_user_tenant ON sys_user(tenant_id);

-- 初始化平台管理员
INSERT INTO sys_user (username, password, real_name, role)
VALUES ('admin', '$2a$10$xxx...', '平台管理员', 'ADMIN');
```
