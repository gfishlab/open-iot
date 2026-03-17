# API Contracts: IoT 平台功能补全

**Feature**: 004-iot-platform-completion
**Date**: 2026-03-16

---

## 1. MQTT Webhook 端点 (connect-service)

### POST /api/v1/mqtt/webhook/connected
设备上线事件回调（EMQX Rule Engine → connect-service）

**Request Body**:
```json
{
  "event": "client.connected",
  "clientid": "device-key-xxx",
  "username": "product-key-xxx",
  "peerhost": "192.168.1.100",
  "keepalive": 60,
  "proto_ver": 4,
  "connected_at": 1709012345678
}
```

**Response**: `200 OK`

---

### POST /api/v1/mqtt/webhook/disconnected
设备离线事件回调

**Request Body**:
```json
{
  "event": "client.disconnected",
  "clientid": "device-key-xxx",
  "username": "product-key-xxx",
  "reason": "normal",
  "disconnected_at": 1709012345678
}
```

**Response**: `200 OK`

---

### POST /api/v1/mqtt/webhook/message
设备消息发布事件回调

**Request Body**:
```json
{
  "event": "message.publish",
  "clientid": "device-key-xxx",
  "username": "product-key-xxx",
  "topic": "/devices/device-key-xxx/properties/report",
  "payload": "{\"temperature\": 26.5, \"humidity\": 65.0}",
  "qos": 1,
  "timestamp": 1709012345678
}
```

**Response**: `200 OK`

---

## 2. OTA 升级 API (device-service)

### POST /api/v1/ota/firmware
上传固件包

**Request**: `multipart/form-data`
- `file`: 固件文件
- `productId`: Long
- `version`: String
- `description`: String (optional)

**Response**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "productId": 100,
    "version": "1.2.0",
    "filePath": "firmware/1/100/1.2.0/firmware.bin",
    "fileSize": 5242880,
    "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
    "fileSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
}
```

---

### GET /api/v1/ota/firmware?productId={id}&page={p}&size={s}
查询固件版本列表

**Response**:
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 5,
    "current": 1,
    "size": 10
  }
}
```

---

### POST /api/v1/ota/tasks
创建 OTA 升级任务

**Request Body**:
```json
{
  "taskName": "温度传感器V1.2升级",
  "productId": 100,
  "firmwareVersionId": 1,
  "upgradeScope": "SELECTED",
  "targetDeviceIds": [201, 202, 203],
  "strategy": "immediate",
  "batchSize": 50
}
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "id": 10,
    "taskName": "温度传感器V1.2升级",
    "taskStatus": "created",
    "totalCount": 3
  }
}
```

---

### GET /api/v1/ota/tasks/{taskId}/devices?status={status}&page={p}&size={s}
查询任务下的设备升级状态

**Response**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "deviceId": 201,
        "deviceName": "sensor-001",
        "upgradeStatus": "downloading",
        "progress": 45,
        "currentVersion": "1.1.0",
        "targetVersion": "1.2.0"
      }
    ],
    "total": 3
  }
}
```

---

### GET /api/v1/ota/firmware/{firmwareId}/download
固件下载（支持 HTTP Range 断点续传）

**Request Headers**:
- `Range: bytes=1048576-` (可选，断点续传)
- `X-Device-Token: xxx` (设备认证)

**Response** (完整下载): `200 OK` + `Content-Type: application/octet-stream`

**Response** (断点续传): `206 Partial Content`
- `Content-Range: bytes 1048576-2097151/5242880`
- `Content-Length: 1048576`

---

## 3. 设备影子 API (device-service)

### GET /api/v1/devices/{deviceId}/shadow
查询设备影子

**Response**:
```json
{
  "code": 200,
  "data": {
    "deviceId": 201,
    "reported": {"temperature": 22, "humidity": 60},
    "desired": {"temperature": 25},
    "delta": {"temperature": 25},
    "version": 42,
    "reportedTime": "2026-03-16T10:30:00",
    "desiredTime": "2026-03-16T11:00:00"
  }
}
```

---

### PUT /api/v1/devices/{deviceId}/shadow/desired
设置设备期望属性（乐观锁）

**Request Body**:
```json
{
  "desired": {"temperature": 25, "reportInterval": 10},
  "version": 42
}
```

**Response** (成功):
```json
{
  "code": 200,
  "data": {
    "reported": {"temperature": 22, "humidity": 60},
    "desired": {"temperature": 25, "reportInterval": 10},
    "delta": {"temperature": 25, "reportInterval": 10},
    "version": 43
  }
}
```

**Response** (版本冲突 409):
```json
{
  "code": 409,
  "msg": "版本冲突，请重新获取最新影子后重试",
  "data": {
    "currentVersion": 43,
    "reported": {"temperature": 22},
    "desired": {"temperature": 26}
  }
}
```

---

## 4. OpenFeign 服务间接口

### TenantFeignClient (tenant-service)

```
GET /api/v1/tenants/{tenantId}          → TenantDTO
GET /api/v1/users/{userId}              → UserDTO
```

### DeviceFeignClient (device-service)

```
GET /api/v1/devices/{deviceId}          → DeviceDTO
GET /api/v1/products/{productId}        → ProductDTO
GET /api/v1/devices/{deviceId}/shadow   → DeviceShadowDTO
```

---

## 5. Gateway 熔断降级端点

### GET /fallback/{serviceName}
熔断降级统一响应

**Response** (503):
```json
{
  "code": 503,
  "msg": "{serviceName}暂时不可用，请稍后重试",
  "data": null,
  "timestamp": "2026-03-16T12:00:00"
}
```

---

## 6. Kafka 事件扩展

### 新增 EventType

| eventType | 方向 | 说明 |
|-----------|------|------|
| `OTA_UPGRADE` | platform → device | OTA 升级通知 |
| `OTA_PROGRESS` | device → platform | OTA 进度上报 |
| `SHADOW_DELTA` | platform → device | 影子 delta 下发 |
| `SHADOW_GET` | device → platform | 设备请求影子 |
| `SHADOW_REPORTED` | device → platform | 设备上报 reported |

### OTA_UPGRADE Payload

```json
{
  "firmwareVersionId": 1,
  "targetVersion": "1.2.0",
  "downloadUrl": "/api/v1/ota/firmware/1/download",
  "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
  "fileSize": 5242880
}
```
