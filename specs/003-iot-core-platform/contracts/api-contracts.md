# API Contracts: IoT Platform Core Functionality

**Date**: 2026-03-06
**Feature**: 003-iot-core-platform
**Version**: 1.0.0

## Overview

本文档定义 IoT 平台核心功能的 RESTful API 契约，遵循以下规范：
- **协议**：HTTP/1.1 (生产环境建议 HTTPS)
- **数据格式**：JSON
- **认证**：Sa-Token (用户端) / DeviceKey + DeviceSecret (设备端)
- **多租户**：通过 HTTP Header `X-Tenant-Id` 传递（由 Gateway 注入）
- **字符编码**：UTF-8
- **统一响应格式**：`ApiResponse<T>`

---

## Common Data Structures

### ApiResponse<T> (统一响应格式)

```json
{
  "code": 200,
  "message": "操作成功",
  "data": T,
  "timestamp": 1709692800000
}
```

**字段说明**：
- `code`: 状态码（200=成功，400=参数错误，401=未认证，403=无权限，500=服务器错误）
- `message`: 提示信息
- `data`: 业务数据（泛型）
- `timestamp`: 响应时间戳（毫秒）

### PageResponse<T> (分页响应)

```json
{
  "records": [T],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

### Error Response (错误响应)

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "errors": [
      {"field": "productName", "message": "产品名称不能为空"}
    ]
  },
  "timestamp": 1709692800000
}
```

---

## Authentication Headers

### User Authentication (用户认证)

```
Authorization: Bearer {token}
X-Tenant-Id: {tenantId}
X-User-Id: {userId}
X-User-Role: {role}
```

**注**：`X-Tenant-Id`, `X-User-Id`, `X-User-Role` 由 Gateway 从 Token 解析后注入，下游服务从 Header 读取。

### Device Authentication (设备认证)

设备通过 MQTT/HTTP 上报数据时，使用以下认证方式：

**MQTT**:
- Username: `{deviceKey}`
- Password: `{deviceSecret}`

**HTTP**:
```json
{
  "deviceKey": "550e8400-e29b-41d4-a716-446655440000",
  "deviceSecret": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": 1709692800000,
  "nonce": "abc123",
  "signature": "md5(deviceKey + timestamp + nonce + deviceSecret)"
}
```

---

## API Endpoints

### 1. Product Management (产品管理)

#### 1.1 创建产品

**Endpoint**: `POST /api/products`

**Request Body**:
```json
{
  "productName": "智能温度传感器",
  "productType": "DEVICE",
  "protocolType": "MQTT",
  "nodeType": "DIRECT",
  "dataFormat": "JSON"
}
```

**Response**: `ApiResponse<ProductVO>`

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "tenantId": 1,
    "productKey": "PROD_A1B2C3",
    "productName": "智能温度传感器",
    "productType": "DEVICE",
    "protocolType": "MQTT",
    "nodeType": "DIRECT",
    "dataFormat": "JSON",
    "thingModel": null,
    "status": "1",
    "createTime": "2026-03-06T12:00:00",
    "updateTime": "2026-03-06T12:00:00"
  },
  "timestamp": 1709692800000
}
```

#### 1.2 获取产品详情

**Endpoint**: `GET /api/products/{id}`

**Response**: `ApiResponse<ProductVO>`

#### 1.3 更新产品

**Endpoint**: `PUT /api/products/{id}`

**Request Body**:
```json
{
  "productName": "智能温度传感器 v2",
  "status": "1"
}
```

**Response**: `ApiResponse<ProductVO>`

#### 1.4 删除产品

**Endpoint**: `DELETE /api/products/{id}`

**Response**: `ApiResponse<Void>`

#### 1.5 获取产品列表

**Endpoint**: `GET /api/products`

**Query Parameters**:
- `page`: 页码（默认 1）
- `pageSize`: 每页数量（默认 20）
- `productName`: 产品名称（模糊查询）
- `protocolType`: 协议类型

**Response**: `ApiResponse<PageResponse<ProductVO>>`

#### 1.6 获取产品下的设备列表

**Endpoint**: `GET /api/products/{id}/devices`

**Query Parameters**:
- `page`: 页码
- `pageSize`: 每页数量
- `deviceStatus`: 设备状态（ONLINE/OFFLINE）

**Response**: `ApiResponse<PageResponse<DeviceVO>>`

---

### 2. Thing Model Management (物模型管理)

#### 2.1 更新物模型

**Endpoint**: `PUT /api/products/{id}/thing-model`

**Request Body**:
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
    }
  ]
}
```

**Response**: `ApiResponse<ThingModelVO>`

#### 2.2 获取物模型

**Endpoint**: `GET /api/products/{id}/thing-model`

**Response**: `ApiResponse<ThingModelVO>`

---

### 3. Parse Rule Management (解析规则管理)

#### 3.1 创建解析规则

**Endpoint**: `POST /api/parse-rules`

**Request Body**:
```json
{
  "productId": 1,
  "ruleName": "JSON字段映射规则",
  "ruleType": "JSON",
  "ruleConfig": {
    "fieldMappings": [
      {"source": "t", "target": "temperature"},
      {"source": "h", "target": "humidity"}
    ]
  },
  "priority": 10
}
```

**Response**: `ApiResponse<ParseRuleVO>`

#### 3.2 获取解析规则

**Endpoint**: `GET /api/parse-rules/{id}`

**Response**: `ApiResponse<ParseRuleVO>`

#### 3.3 更新解析规则

**Endpoint**: `PUT /api/parse-rules/{id}`

**Request Body**: 同创建

**Response**: `ApiResponse<ParseRuleVO>`

#### 3.4 删除解析规则

**Endpoint**: `DELETE /api/parse-rules/{id}`

**Response**: `ApiResponse<Void>`

#### 3.5 测试解析规则

**Endpoint**: `POST /api/parse-rules/{id}/test`

**Request Body**:
```json
{
  "rawData": "{\"t\": 25.5, \"h\": 60.2}"
}
```

**Response**: `ApiResponse<ParsedDataVO>`

```json
{
  "code": 200,
  "message": "测试成功",
  "data": {
    "parsedData": {
      "temperature": 25.5,
      "humidity": 60.2
    },
    "executionTime": 5
  },
  "timestamp": 1709692800000
}
```

---

### 4. Mapping Rule Management (映射规则管理)

#### 4.1 创建映射规则

**Endpoint**: `POST /api/mapping-rules`

**Request Body**:
```json
{
  "productId": 1,
  "ruleName": "温度华氏转摄氏",
  "fieldMappings": {
    "mappings": [
      {
        "source": "temperature",
        "target": "temperature",
        "transformation": {
          "type": "FORMULA",
          "expression": "(value - 32) * 5 / 9"
        }
      }
    ]
  }
}
```

**Response**: `ApiResponse<MappingRuleVO>`

#### 4.2 获取映射规则

**Endpoint**: `GET /api/mapping-rules/{id}`

**Response**: `ApiResponse<MappingRuleVO>`

#### 4.3 更新映射规则

**Endpoint**: `PUT /api/mapping-rules/{id}`

**Response**: `ApiResponse<MappingRuleVO>`

#### 4.4 删除映射规则

**Endpoint**: `DELETE /api/mapping-rules/{id}`

**Response**: `ApiResponse<Void>`

#### 4.5 测试映射规则

**Endpoint**: `POST /api/mapping-rules/{id}/test`

**Request Body**:
```json
{
  "parsedData": {
    "temperature": 77
  }
}
```

**Response**: `ApiResponse<MappedDataVO>`

```json
{
  "code": 200,
  "message": "测试成功",
  "data": {
    "mappedData": {
      "temperature": 25
    }
  },
  "timestamp": 1709692800000
}
```

---

### 5. Forward Rule Management (转发规则管理)

#### 5.1 创建转发规则

**Endpoint**: `POST /api/forward-rules`

**Request Body**:
```json
{
  "productId": 1,
  "ruleName": "转发到Kafka",
  "ruleSql": "WHERE temperature > 20",
  "targetType": "KAFKA",
  "targetConfig": {
    "topic": "device-events-forward",
    "bootstrapServers": "localhost:9092",
    "acks": "all"
  }
}
```

**Response**: `ApiResponse<ForwardRuleVO>`

#### 5.2 获取转发规则

**Endpoint**: `GET /api/forward-rules/{id}`

**Response**: `ApiResponse<ForwardRuleVO>`

#### 5.3 更新转发规则

**Endpoint**: `PUT /api/forward-rules/{id}`

**Response**: `ApiResponse<ForwardRuleVO>`

#### 5.4 删除转发规则

**Endpoint**: `DELETE /api/forward-rules/{id}`

**Response**: `ApiResponse<Void>`

#### 5.5 测试转发规则

**Endpoint**: `POST /api/forward-rules/{id}/test`

**Request Body**:
```json
{
  "deviceData": {
    "temperature": 25,
    "humidity": 60
  }
}
```

**Response**: `ApiResponse<TestResultVO>`

```json
{
  "code": 200,
  "message": "测试成功",
  "data": {
    "matched": true,
    "forwarded": true,
    "targetResponse": "Message sent to Kafka topic device-events-forward"
  },
  "timestamp": 1709692800000
}
```

---

### 6. Alarm Rule Management (告警规则管理)

#### 6.1 创建告警规则

**Endpoint**: `POST /api/alarm-rules`

**Request Body**:
```json
{
  "productId": 1,
  "deviceId": null,
  "ruleName": "高温告警",
  "alarmLevel": "WARNING",
  "conditionExpression": "temperature > 30",
  "notifyConfig": {
    "channels": ["EMAIL", "WEBHOOK"],
    "email": {
      "recipients": ["admin@example.com"]
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
}
```

**Response**: `ApiResponse<AlarmRuleVO>`

#### 6.2 获取告警规则

**Endpoint**: `GET /api/alarm-rules/{id}`

**Response**: `ApiResponse<AlarmRuleVO>`

#### 6.3 更新告警规则

**Endpoint**: `PUT /api/alarm-rules/{id}`

**Response**: `ApiResponse<AlarmRuleVO>`

#### 6.4 删除告警规则

**Endpoint**: `DELETE /api/alarm-rules/{id}`

**Response**: `ApiResponse<Void>`

#### 6.5 查询告警记录

**Endpoint**: `GET /api/alarm-records`

**Query Parameters**:
- `page`: 页码
- `pageSize`: 每页数量
- `deviceId`: 设备ID
- `alarmLevel`: 告警级别
- `status`: 状态（ACTIVE/ACKNOWLEDGED/RECOVERED）
- `startTime`: 开始时间
- `endTime`: 结束时间

**Response**: `ApiResponse<PageResponse<AlarmRecordVO>>`

#### 6.6 确认告警

**Endpoint**: `POST /api/alarm-records/{id}/acknowledge`

**Response**: `ApiResponse<AlarmRecordVO>`

---

### 7. Device Service Invocation (设备服务调用)

#### 7.1 调用设备服务

**Endpoint**: `POST /api/devices/{id}/services/{serviceIdentifier}`

**Request Body**:
```json
{
  "invokeType": "SYNC",
  "inputData": {
    "threshold_value": 50
  }
}
```

**Response**: `ApiResponse<ServiceInvokeVO>`

```json
{
  "code": 200,
  "message": "调用成功",
  "data": {
    "invokeId": "550e8400-e29b-41d4-a716-446655440002",
    "status": "SUCCESS",
    "outputData": {
      "success": true
    },
    "invokeTime": "2026-03-06T12:00:00",
    "completeTime": "2026-03-06T12:00:01"
  },
  "timestamp": 1709692800000
}
```

#### 7.2 查询服务调用状态

**Endpoint**: `GET /api/service-invocations/{invocationId}`

**Response**: `ApiResponse<ServiceInvokeVO>`

---

### 8. Historical Data Query (历史数据查询)

#### 8.1 查询设备属性历史

**Endpoint**: `GET /api/devices/{id}/properties/history`

**Query Parameters**:
- `property`: 属性标识符
- `startTime`: 开始时间（ISO 8601）
- `endTime`: 结束时间（ISO 8601）
- `interval`: 聚合间隔（RAW/MINUTE/HOUR/DAY）

**Response**: `ApiResponse<List<PropertyHistoryVO>>`

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "property": "temperature",
      "value": 25.5,
      "time": "2026-03-06T12:00:00"
    },
    {
      "property": "temperature",
      "value": 26.0,
      "time": "2026-03-06T12:01:00"
    }
  ],
  "timestamp": 1709692800000
}
```

#### 8.2 查询设备事件历史

**Endpoint**: `GET /api/devices/{id}/events/history`

**Query Parameters**:
- `eventIdentifier`: 事件标识符
- `startTime`: 开始时间
- `endTime`: 结束时间

**Response**: `ApiResponse<List<EventHistoryVO>>`

#### 8.3 导出设备数据

**Endpoint**: `GET /api/devices/{id}/data/export`

**Query Parameters**:
- `startTime`: 开始时间
- `endTime`: 结束时间
- `format`: 导出格式（CSV/JSON）

**Response**: 文件下载（Content-Type: text/csv 或 application/json）

---

## HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | 成功 |
| 201 | Created | 资源创建成功 |
| 204 | No Content | 删除成功（无返回内容） |
| 400 | Bad Request | 参数校验失败 |
| 401 | Unauthorized | 未认证或 Token 无效 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如重复创建） |
| 500 | Internal Server Error | 服务器内部错误 |

---

## Rate Limiting

- **用户 API**: 1000 requests/minute/user
- **设备 API**: 100 requests/second/device

**Rate Limit Headers**:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1709692860
```

---

## Versioning

API 使用 URL 路径版本控制：`/api/v1/products`（当前版本省略 v1）

向后兼容的修改（新增字段、新增接口）不升级版本。
不兼容的修改（删除字段、修改行为）升级版本。

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-06 | 初始版本 |
