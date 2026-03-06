# Data Model: IoT Platform Core Functionality

**Date**: 2026-03-06
**Feature**: 003-iot-core-platform

## Overview

本文档定义 IoT 平台核心功能的数据库模式设计，遵循以下原则：
- **多租户隔离**：所有业务表包含 `tenant_id` 字段
- **共享数据库**：所有微服务共享 PostgreSQL 数据库，由 tenant-service 管理 Flyway 迁移
- **审计字段**：所有表包含 `create_time`, `update_time`, `create_by`, `update_by`, `delete_flag`
- **软删除**：使用 `delete_flag` 字段（'0'=未删除，'1'=已删除）

---

## Entity Relationship Diagram

```
Tenant (sys_tenant)
  └─ Product (product)
       ├─ Thing Model (JSON field in product)
       ├─ Parse Rule (parse_rule)
       ├─ Mapping Rule (mapping_rule)
       ├─ Forward Rule (forward_rule)
       ├─ Alarm Rule (alarm_rule)
       └─ Device (device)
            ├─ Device Property (device_property)
            ├─ Device Event (device_event)
            ├─ Device Service Invoke (device_service_invoke)
            └─ Alarm Record (alarm_record)
```

---

## Database Tables

### 1. product (产品表)

**用途**：设备的产品模板，定义设备类别和通用配置

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_key | VARCHAR(50) | NOT NULL, UNIQUE(tenant_id, product_key) | 产品密钥（租户内唯一，如 PROD_A1B2C3） |
| product_name | VARCHAR(100) | NOT NULL | 产品名称 |
| product_type | VARCHAR(20) | DEFAULT 'DEVICE' | 产品类型（DEVICE/GATEWAY） |
| protocol_type | VARCHAR(20) | NOT NULL | 协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM） |
| node_type | VARCHAR(20) | DEFAULT 'DIRECT' | 节点类型（DIRECT/GATEWAY） |
| data_format | VARCHAR(20) | DEFAULT 'JSON' | 数据格式（JSON/XML/BINARY/CUSTOM） |
| thing_model | JSONB | | 物模型定义（JSON格式） |
| status | CHAR(1) | DEFAULT '1' | 状态（'1'=启用，'0'=禁用） |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记（'0'=未删除，'1'=已删除） |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `uk_tenant_product_key` UNIQUE (tenant_id, product_key)
- `idx_product_tenant` (tenant_id, delete_flag)

**Thing Model JSON Structure**:

```json
{
  "properties": [
    {
      "identifier": "temperature",
      "name": "温度",
      "type": "float",
      "unit": "°C",
      "min": -40,
      "max": 80,
      "required": true
    },
    {
      "identifier": "humidity",
      "name": "湿度",
      "type": "float",
      "unit": "%",
      "min": 0,
      "max": 100,
      "required": false
    }
  ],
  "events": [
    {
      "identifier": "high_temperature_alert",
      "name": "高温告警",
      "type": "alert",
      "severity": "warning",
      "outputParams": [
        {"identifier": "temperature", "name": "温度", "type": "float"}
      ]
    }
  ],
  "services": [
    {
      "identifier": "reboot",
      "name": "重启设备",
      "inputParams": [],
      "outputParams": [
        {"identifier": "success", "name": "执行结果", "type": "boolean"}
      ]
    },
    {
      "identifier": "set_threshold",
      "name": "设置阈值",
      "inputParams": [
        {"identifier": "threshold_value", "name": "阈值", "type": "float"}
      ],
      "outputParams": [
        {"identifier": "success", "name": "执行结果", "type": "boolean"}
      ]
    }
  ]
}
```

---

### 2. device (设备表，修改)

**用途**：设备实例，关联产品

**新增字段**：

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| product_id | BIGINT | FOREIGN KEY (product.id) | 产品ID |

**修改后的完整字段**：

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | FOREIGN KEY (product.id) | 产品ID |
| device_key | VARCHAR(100) | NOT NULL, UNIQUE | 设备密钥（全局唯一，UUID） |
| device_secret | VARCHAR(255) | NOT NULL | 设备密钥（BCrypt哈希） |
| device_name | VARCHAR(100) | NOT NULL | 设备名称 |
| device_status | VARCHAR(20) | DEFAULT 'OFFLINE' | 设备状态（ONLINE/OFFLINE） |
| last_comm_time | TIMESTAMP | | 最后通信时间 |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `uk_device_key` UNIQUE (device_key)
- `idx_device_tenant` (tenant_id, delete_flag)
- `idx_device_product` (product_id)

---

### 3. parse_rule (解析规则表)

**用途**：定义原始设备数据的解析规则

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | NOT NULL, FOREIGN KEY (product.id) | 产品ID |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| rule_type | VARCHAR(20) | NOT NULL | 规则类型（JSON/JAVASCRIPT/REGEX/BINARY） |
| rule_config | JSONB | NOT NULL | 规则配置（JSON格式） |
| priority | INT | DEFAULT 0 | 优先级（数字越大优先级越高） |
| status | CHAR(1) | DEFAULT '1' | 状态（'1'=启用，'0'=禁用） |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `idx_parse_rule_product` (product_id, priority DESC, delete_flag)

**Rule Config JSON Examples**:

**JSON Field Mapping**:
```json
{
  "fieldMappings": [
    {"source": "t", "target": "temperature"},
    {"source": "h", "target": "humidity"}
  ]
}
```

**JavaScript Transformation**:
```json
{
  "script": "return { temperature: data.t * 1.0, humidity: data.h * 1.0 };"
}
```

**Regex Extraction**:
```json
{
  "pattern": "T:(\\d+\\.?\\d*);H:(\\d+\\.?\\d*)",
  "groups": [
    {"groupIndex": 1, "target": "temperature"},
    {"groupIndex": 2, "target": "humidity"}
  ]
}
```

**Binary Parsing**:
```json
{
  "byteOrder": "BIG_ENDIAN",
  "fields": [
    {"offset": 0, "length": 2, "type": "UINT16", "target": "temperature"},
    {"offset": 2, "length": 2, "type": "UINT16", "target": "humidity"}
  ]
}
```

---

### 4. mapping_rule (映射规则表)

**用途**：定义解析后的数据到物模型属性的映射

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | NOT NULL, FOREIGN KEY (product.id) | 产品ID |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| field_mappings | JSONB | NOT NULL | 字段映射配置（JSON格式） |
| status | CHAR(1) | DEFAULT '1' | 状态（'1'=启用，'0'=禁用） |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `idx_mapping_rule_product` (product_id, delete_flag)

**Field Mappings JSON**:
```json
{
  "mappings": [
    {
      "source": "temperature",
      "target": "temperature",
      "transformation": {
        "type": "FORMULA",
        "expression": "value * 1.8 + 32"
      }
    },
    {
      "source": "humidity",
      "target": "humidity",
      "transformation": null
    }
  ]
}
```

---

### 5. forward_rule (转发规则表)

**用途**：定义数据转发到外部系统的规则

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | FOREIGN KEY (product.id) | 产品ID（NULL表示所有产品） |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| rule_sql | TEXT | | 过滤条件（SQL WHERE子句） |
| target_type | VARCHAR(20) | NOT NULL | 目标类型（KAFKA/HTTP/MQTT/INFLUXDB） |
| target_config | JSONB | NOT NULL | 目标配置（JSON格式） |
| status | CHAR(1) | DEFAULT '1' | 状态（'1'=启用，'0'=禁用） |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `idx_forward_rule_tenant` (tenant_id, delete_flag)

**Target Config JSON Examples**:

**Kafka**:
```json
{
  "topic": "device-events-forward",
  "bootstrapServers": "localhost:9092",
  "acks": "all"
}
```

**HTTP Webhook**:
```json
{
  "url": "https://example.com/webhook",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer token123"
  },
  "timeout": 5000
}
```

**MQTT**:
```json
{
  "brokerUrl": "tcp://localhost:1883",
  "topic": "device/data",
  "clientId": "openiot-forwarder",
  "qos": 1
}
```

**InfluxDB**:
```json
{
  "bucket": "device-data",
  "measurement": "device_data"
}
```

---

### 6. alarm_rule (告警规则表)

**用途**：定义告警触发条件

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | FOREIGN KEY (product.id) | 产品ID（NULL表示所有产品） |
| device_id | BIGINT | FOREIGN KEY (device.id) | 设备ID（NULL表示所有设备） |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| alarm_level | VARCHAR(20) | NOT NULL | 告警级别（CRITICAL/WARNING/INFO） |
| condition_expression | TEXT | NOT NULL | 触发条件（Aviator表达式） |
| notify_config | JSONB | | 通知配置（JSON格式） |
| status | CHAR(1) | DEFAULT '1' | 状态（'1'=启用，'0'=禁用） |
| delete_flag | CHAR(1) | DEFAULT '0' | 删除标记 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| create_by | BIGINT | | 创建人ID |
| update_by | BIGINT | | 更新人ID |

**索引**：
- `idx_alarm_rule_tenant` (tenant_id, delete_flag)

**Condition Expression Examples**:
- `temperature > 30`
- `temperature > 30 && humidity < 50`
- `deviceStatus == 'offline'`

**Notify Config JSON**:
```json
{
  "channels": ["EMAIL", "WEBHOOK"],
  "email": {
    "recipients": ["admin@example.com", "ops@example.com"]
  },
  "webhook": {
    "url": "https://example.com/alarm-webhook",
    "method": "POST"
  },
  "aggregation": {
    "enabled": true,
    "windowMinutes": 5
  }
}
```

---

### 7. alarm_record (告警记录表)

**用途**：记录触发的告警

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| alarm_rule_id | BIGINT | NOT NULL, FOREIGN KEY (alarm_rule.id) | 告警规则ID |
| device_id | BIGINT | FOREIGN KEY (device.id) | 设备ID |
| alarm_level | VARCHAR(20) | NOT NULL | 告警级别（CRITICAL/WARNING/INFO） |
| alarm_title | VARCHAR(200) | NOT NULL | 告警标题 |
| alarm_content | TEXT | | 告警内容 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | 状态（ACTIVE/ACKNOWLEDGED/RECOVERED） |
| trigger_time | TIMESTAMP | NOT NULL | 触发时间 |
| recover_time | TIMESTAMP | | 恢复时间 |
| ack_time | TIMESTAMP | | 确认时间 |
| ack_by | BIGINT | | 确认人ID |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- `idx_alarm_record_tenant` (tenant_id, trigger_time DESC)
- `idx_alarm_record_device` (device_id, trigger_time DESC)
- `idx_alarm_record_status` (status, trigger_time DESC)

---

### 8. device_service_invoke (设备服务调用表)

**用途**：记录设备服务调用（RPC）

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| device_id | BIGINT | NOT NULL, FOREIGN KEY (device.id) | 设备ID |
| service_identifier | VARCHAR(100) | NOT NULL | 服务标识符 |
| invoke_id | VARCHAR(50) | NOT NULL, UNIQUE | 调用ID（全局唯一，UUID） |
| invoke_type | VARCHAR(20) | DEFAULT 'SYNC' | 调用类型（SYNC/ASYNC） |
| input_data | JSONB | | 输入参数（JSON格式） |
| output_data | JSONB | | 输出结果（JSON格式） |
| status | VARCHAR(20) | DEFAULT 'PENDING' | 状态（PENDING/SUCCESS/FAILED/TIMEOUT） |
| error_message | TEXT | | 错误信息 |
| invoke_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 调用时间 |
| complete_time | TIMESTAMP | | 完成时间 |

**索引**：
- `uk_invoke_id` UNIQUE (invoke_id)
- `idx_service_invoke_device` (device_id, invoke_time DESC)
- `idx_service_invoke_status` (status, invoke_time)

**Input/Output Data JSON Examples**:

**Input**:
```json
{
  "threshold_value": 50
}
```

**Output**:
```json
{
  "success": true
}
```

---

### 9. device_property (设备属性表，新增)

**用途**：存储设备当前属性值（PostgreSQL），历史数据存储在 InfluxDB

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| device_id | BIGINT | NOT NULL, FOREIGN KEY (device.id) | 设备ID |
| property_identifier | VARCHAR(100) | NOT NULL | 属性标识符 |
| property_value | TEXT | NOT NULL | 属性值（JSON序列化） |
| data_type | VARCHAR(20) | NOT NULL | 数据类型（INT/FLOAT/STRING/BOOLEAN/...） |
| update_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- `uk_device_property` UNIQUE (device_id, property_identifier)
- `idx_property_update_time` (update_time)

---

### 10. device_event (设备事件表，新增)

**用途**：存储设备上报的事件

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| device_id | BIGINT | NOT NULL, FOREIGN KEY (device.id) | 设备ID |
| event_identifier | VARCHAR(100) | NOT NULL | 事件标识符 |
| event_type | VARCHAR(20) | NOT NULL | 事件类型（INFO/WARNING/ERROR） |
| event_data | JSONB | | 事件数据（JSON格式） |
| event_time | TIMESTAMP | NOT NULL | 事件时间 |
| create_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- `idx_device_event_device` (device_id, event_time DESC)
- `idx_device_event_time` (event_time)

---

## Migration Script

**文件路径**：`backend/tenant-service/src/main/resources/db/migration/V1.3.0__add_product_and_rule_tables.sql`

```sql
-- 1. 创建产品表
CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_key VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_type VARCHAR(20) DEFAULT 'DEVICE',
    protocol_type VARCHAR(20) NOT NULL,
    node_type VARCHAR(20) DEFAULT 'DIRECT',
    data_format VARCHAR(20) DEFAULT 'JSON',
    thing_model JSONB,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_tenant_product_key UNIQUE (tenant_id, product_key)
);

COMMENT ON TABLE product IS '产品表';
COMMENT ON COLUMN product.id IS '主键';
COMMENT ON COLUMN product.tenant_id IS '租户ID';
COMMENT ON COLUMN product.product_key IS '产品密钥（租户内唯一）';
COMMENT ON COLUMN product.product_name IS '产品名称';
COMMENT ON COLUMN product.product_type IS '产品类型（DEVICE/GATEWAY）';
COMMENT ON COLUMN product.protocol_type IS '协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）';
COMMENT ON COLUMN product.node_type IS '节点类型（DIRECT/GATEWAY）';
COMMENT ON COLUMN product.data_format IS '数据格式（JSON/XML/BINARY/CUSTOM）';
COMMENT ON COLUMN product.thing_model IS '物模型定义（JSON格式）';
COMMENT ON COLUMN product.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN product.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_product_tenant ON product(tenant_id, delete_flag);

-- 2. 修改设备表，添加 product_id 字段
ALTER TABLE device ADD COLUMN product_id BIGINT;
ALTER TABLE device ADD CONSTRAINT fk_device_product FOREIGN KEY (product_id) REFERENCES product(id);
CREATE INDEX idx_device_product ON device(product_id);

COMMENT ON COLUMN device.product_id IS '产品ID';

-- 3. 创建解析规则表
CREATE TABLE IF NOT EXISTS parse_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    rule_config JSONB NOT NULL,
    priority INT DEFAULT 0,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_parse_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE parse_rule IS '解析规则表';
COMMENT ON COLUMN parse_rule.rule_type IS '规则类型（JSON/JAVASCRIPT/REGEX/BINARY）';

CREATE INDEX idx_parse_rule_product ON parse_rule(product_id, priority DESC, delete_flag);

-- 4. 创建映射规则表
CREATE TABLE IF NOT EXISTS mapping_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    field_mappings JSONB NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_mapping_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE mapping_rule IS '映射规则表';

CREATE INDEX idx_mapping_rule_product ON mapping_rule(product_id, delete_flag);

-- 5. 创建转发规则表
CREATE TABLE IF NOT EXISTS forward_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    rule_name VARCHAR(100) NOT NULL,
    rule_sql TEXT,
    target_type VARCHAR(20) NOT NULL,
    target_config JSONB NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_forward_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE forward_rule IS '转发规则表';
COMMENT ON COLUMN forward_rule.target_type IS '目标类型（KAFKA/HTTP/MQTT/INFLUXDB）';

CREATE INDEX idx_forward_rule_tenant ON forward_rule(tenant_id, delete_flag);

-- 6. 创建告警规则表
CREATE TABLE IF NOT EXISTS alarm_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    device_id BIGINT,
    rule_name VARCHAR(100) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    condition_expression TEXT NOT NULL,
    notify_config JSONB,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_alarm_rule_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_alarm_rule_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE alarm_rule IS '告警规则表';
COMMENT ON COLUMN alarm_rule.alarm_level IS '告警级别（CRITICAL/WARNING/INFO）';

CREATE INDEX idx_alarm_rule_tenant ON alarm_rule(tenant_id, delete_flag);

-- 7. 创建告警记录表
CREATE TABLE IF NOT EXISTS alarm_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    alarm_rule_id BIGINT NOT NULL,
    device_id BIGINT,
    alarm_level VARCHAR(20) NOT NULL,
    alarm_title VARCHAR(200) NOT NULL,
    alarm_content TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    trigger_time TIMESTAMP NOT NULL,
    recover_time TIMESTAMP,
    ack_time TIMESTAMP,
    ack_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alarm_record_rule FOREIGN KEY (alarm_rule_id) REFERENCES alarm_rule(id),
    CONSTRAINT fk_alarm_record_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE alarm_record IS '告警记录表';
COMMENT ON COLUMN alarm_record.status IS '状态（ACTIVE/ACKNOWLEDGED/RECOVERED）';

CREATE INDEX idx_alarm_record_tenant ON alarm_record(tenant_id, trigger_time DESC);
CREATE INDEX idx_alarm_record_device ON alarm_record(device_id, trigger_time DESC);
CREATE INDEX idx_alarm_record_status ON alarm_record(status, trigger_time DESC);

-- 8. 创建设备服务调用表
CREATE TABLE IF NOT EXISTS device_service_invoke (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    service_identifier VARCHAR(100) NOT NULL,
    invoke_id VARCHAR(50) NOT NULL UNIQUE,
    invoke_type VARCHAR(20) DEFAULT 'SYNC',
    input_data JSONB,
    output_data JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    invoke_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complete_time TIMESTAMP,
    CONSTRAINT fk_service_invoke_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE device_service_invoke IS '设备服务调用表';
COMMENT ON COLUMN device_service_invoke.invoke_type IS '调用类型（SYNC/ASYNC）';
COMMENT ON COLUMN device_service_invoke.status IS '状态（PENDING/SUCCESS/FAILED/TIMEOUT）';

CREATE UNIQUE INDEX uk_invoke_id ON device_service_invoke(invoke_id);
CREATE INDEX idx_service_invoke_device ON device_service_invoke(device_id, invoke_time DESC);
CREATE INDEX idx_service_invoke_status ON device_service_invoke(status, invoke_time);

-- 9. 创建设备属性表
CREATE TABLE IF NOT EXISTS device_property (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    property_identifier VARCHAR(100) NOT NULL,
    property_value TEXT NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_property_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT uk_device_property UNIQUE (device_id, property_identifier)
);

COMMENT ON TABLE device_property IS '设备属性表（当前状态）';

CREATE INDEX idx_property_update_time ON device_property(update_time);

-- 10. 创建设备事件表
CREATE TABLE IF NOT EXISTS device_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    event_identifier VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_data JSONB,
    event_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_event_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE device_event IS '设备事件表';
COMMENT ON COLUMN device_event.event_type IS '事件类型（INFO/WARNING/ERROR）';

CREATE INDEX idx_device_event_device ON device_event(device_id, event_time DESC);
CREATE INDEX idx_device_event_time ON device_event(event_time);
```

---

## Summary

| 表名 | 用途 | 关键特性 |
|------|------|----------|
| product | 产品表 | 租户内唯一 product_key，thing_model JSONB 字段 |
| device (修改) | 设备表 | 新增 product_id 字段，关联产品 |
| parse_rule | 解析规则表 | 支持 JSON/JS/Regex/Binary 四种类型，优先级排序 |
| mapping_rule | 映射规则表 | 字段映射 + 转换函数 |
| forward_rule | 转发规则表 | 支持 Kafka/HTTP/MQTT/InfluxDB 四种目标 |
| alarm_rule | 告警规则表 | Aviator 表达式，支持多级告警 |
| alarm_record | 告警记录表 | 记录告警生命周期（ACTIVE/ACK/RECOVERED） |
| device_service_invoke | 设备服务调用表 | 记录 RPC 调用，支持同步/异步 |
| device_property | 设备属性表 | 存储当前属性值（历史数据在 InfluxDB） |
| device_event | 设备事件表 | 记录设备上报的事件 |

所有表遵循多租户隔离、软删除、审计字段规范，符合项目宪法原则。
