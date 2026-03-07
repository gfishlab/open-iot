# Device Service API 文档

## 概述

Device Service（设备服务）是 OpenIoT 平台的核心服务之一，负责设备、产品、物模型、告警、规则引擎等功能的实现。

## 基础信息

- **服务名称**: device-service
- **端口**: 8081
- **API 前缀**: `/api`
- **认证方式**: Sa-Token（请求头携带 `satoken`）
- **租户隔离**: 自动注入 `X-Tenant-Id` 请求头

## API 分组

### 1. 产品管理 API (`01-产品管理`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建产品 | POST | `/api/products` | 创建新产品并生成密钥 |
| 查询产品 | GET | `/api/products/{id}` | 查询产品详情 |
| 更新产品 | PUT | `/api/products/{id}` | 更新产品信息 |
| 删除产品 | DELETE | `/api/products/{id}` | 删除产品 |
| 产品列表 | GET | `/api/products` | 分页查询产品列表 |
| 产品设备 | GET | `/api/products/{id}/devices` | 查询产品关联设备 |
| 产品详情 | GET | `/api/products/{id}/detail` | 查询产品详情（含统计） |
| 更新状态 | PUT | `/api/products/{id}/status` | 启用/禁用产品 |
| 产品统计 | GET | `/api/products/{id}/statistics` | 查询产品统计信息 |

#### 请求示例

**创建产品**
```http
POST /api/products
Content-Type: application/json
satoken: your-token-here

{
  "productName": "智能网关",
  "productCode": "GW001",
  "protocolType": "MQTT",
  "nodeType": "gateway",
  "description": "工业级智能网关"
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "productName": "智能网关",
    "productCode": "GW001",
    "productKey": "prod-key-xxx",
    "productSecret": "prod-secret-xxx",
    "protocolType": "MQTT",
    "nodeType": "gateway",
    "status": "1",
    "createTime": "2026-03-07T12:00:00"
  }
}
```

---

### 2. 设备管理 API (`02-设备管理`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 注册设备 | POST | `/api/devices` | 注册新设备 |
| 查询设备 | GET | `/api/devices/{id}` | 查询设备详情 |
| 更新设备 | PUT | `/api/devices/{id}` | 更新设备信息 |
| 删除设备 | DELETE | `/api/devices/{id}` | 删除设备 |
| 设备列表 | GET | `/api/devices` | 分页查询设备 |
| 设备上线 | POST | `/api/devices/{id}/online` | 设备上线 |
| 设备离线 | POST | `/api/devices/{id}/offline` | 设备离线 |
| 查询状态 | GET | `/api/devices/{id}/status` | 查询设备状态 |

#### 请求示例

**注册设备**
```http
POST /api/devices
Content-Type: application/json

{
  "productId": 1,
  "deviceCode": "device001",
  "deviceName": "1号设备",
  "macAddress": "AA:BB:CC:DD:EE:FF"
}
```

---

### 3. 物模型管理 API (`03-物模型管理`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询物模型 | GET | `/api/products/{id}/thing-model` | 查询产品物模型 |
| 更新物模型 | PUT | `/api/products/{id}/thing-model` | 更新物模型定义 |

#### 物模型结构

```json
{
  "properties": [
    {
      "propertyIdentifier": "temperature",
      "propertyName": "温度",
      "dataType": "float",
      "unit": "℃",
      "readWriteFlag": "rw"
    }
  ],
  "events": [
    {
      "eventIdentifier": "high_temp_alert",
      "eventName": "高温告警",
      "eventType": "alert",
      "outputParams": [
        {
          "paramName": "temperature",
          "paramDataType": "float"
        }
      ]
    }
  ],
  "services": [
    {
      "serviceIdentifier": "restart",
      "serviceName": "重启服务",
      "callType": "async",
      "inputParams": [
        {
          "paramName": "delay",
          "paramDataType": "int"
        }
      ]
    }
  ]
}
```

---

### 4. 设备控制 API (`04-设备控制`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 调用服务 | POST | `/api/v1/devices/{id}/services/{service}/call` | 调用设备服务 |
| 设置属性 | PUT | `/api/v1/devices/{id}/properties/{property}` | 设置设备属性 |
| 发送命令 | POST | `/api/v1/devices/{id}/commands/{command}` | 下发命令 |
| 服务调用记录 | GET | `/api/v1/devices/{id}/services/calls` | 查询服务调用历史 |
| 属性设置记录 | GET | `/api/v1/devices/{id}/properties/sets` | 查询属性设置历史 |
| 命令下发记录 | GET | `/api/v1/devices/{id}/commands/sends` | 查询命令下发历史 |

#### 请求示例

**调用服务**
```http
POST /api/v1/devices/1/services/restart/call
Content-Type: application/json

{
  "inputParams": {
    "delay": 5000
  }
}
```

**设置属性**
```http
PUT /api/v1/devices/1/properties/temperature
Content-Type: application/json

{
  "value": 25.5
}
```

---

### 5. 告警管理 API (`05-告警管理`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 告警列表 | GET | `/api/alerts` | 分页查询告警记录 |
| 处理告警 | PUT | `/api/alerts/{id}/handle` | 处理单个告警 |
| 批量处理 | PUT | `/api/alerts/batch-handle` | 批量处理告警 |
| 告警统计 | GET | `/api/alerts/statistics` | 查询告警统计 |
| 创建规则 | POST | `/api/alert-rules` | 创建告警规则 |
| 删除规则 | DELETE | `/api/alert-rules/{id}` | 删除告警规则 |

#### 告警级别

- `critical`: 严重告警（红色）
- `warning`: 警告告警（橙色）
- `info`: 信息告警（蓝色）

#### 告警状态

- `pending`: 待处理
- `processing`: 处理中
- `resolved`: 已解决
- `ignored`: 已忽略

---

### 6. 规则引擎 API (`06-规则引擎`)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 规则列表 | GET | `/api/rules` | 查询规则列表 |
| 创建规则 | POST | `/api/rules` | 创建规则 |
| 更新规则 | PUT | `/api/rules/{id}` | 更新规则 |
| 删除规则 | DELETE | `/api/rules/{id}` | 删除规则 |
| 启用规则 | PUT | `/api/rules/{id}/status` | 启用/禁用规则 |

#### 规则类型

- `device`: 设备规则（针对单个设备）
- `product`: 产品规则（针对产品下所有设备）

#### 规则表达式示例

```
# 温度超过 50 度
temperature > 50

# 湿度高于 90 且 温度高于 40
humidity > 90 && temperature > 40

# 设备离线
status == 'offline'
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或认证失败 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 统一响应格式

所有接口返回统一格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-03-07T12:00:00"
}
```

---

## Swagger UI 访问

启动服务后访问以下地址查看完整 API 文档：

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs

---

## 测试

### 运行单元测试

```bash
cd backend/device-service
mvn test
```

### 运行集成测试

```bash
mvn verify -Dspring.profiles.active=test
```

---

## 附录

### 协议类型

- `MQTT`: MQTT 协议
- `HTTP`: HTTP 协议
- `CoAP`: CoAP 协议
- `Modbus`: Modbus 协议
- `OPC UA`: OPC UA 协议

### 节点类型

- `device`: 直连设备
- `gateway`: 网关设备
- `sub_device`: 子设备

### 设备状态

- `online`: 在线
- `offline`: 离线
- `inactive`: 未激活
