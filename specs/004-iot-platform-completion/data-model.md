# Data Model: IoT 平台功能补全

**Feature**: 004-iot-platform-completion
**Date**: 2026-03-16

---

## 新增实体

### 1. firmware_version（固件版本表）

**数据库**：openiot_device
**迁移脚本**：V1.7.0__add_ota_tables.sql

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| product_id | BIGINT | NOT NULL | 关联产品 |
| firmware_name | VARCHAR(100) | | 固件名称 |
| version | VARCHAR(50) | NOT NULL | 版本号（semver） |
| file_path | VARCHAR(500) | NOT NULL | 固件包相对路径 |
| file_size | BIGINT | | 文件大小（字节） |
| file_md5 | VARCHAR(32) | | MD5 校验和 |
| file_sha256 | VARCHAR(64) | | SHA256 校验和 |
| description | TEXT | | 版本说明 |
| status | CHAR(1) | DEFAULT '1' | 1-启用 0-禁用 |
| del_flag | CHAR(1) | DEFAULT '0' | 0-正常 1-删除 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| create_by | BIGINT | | 创建人 |
| update_by | BIGINT | | 更新人 |

**索引**：
- `UNIQUE (product_id, version, del_flag)` — 同产品版本号唯一
- `(tenant_id, del_flag)` — 租户查询

---

### 2. ota_upgrade_task（OTA 升级任务表）

**数据库**：openiot_device

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| task_name | VARCHAR(100) | | 任务名称 |
| product_id | BIGINT | NOT NULL | 目标产品 |
| firmware_version_id | BIGINT | NOT NULL | 目标固件版本 |
| upgrade_scope | VARCHAR(20) | DEFAULT 'ALL' | ALL/SELECTED |
| target_device_ids | JSONB | | 指定设备ID列表 |
| total_count | INT | DEFAULT 0 | 总设备数 |
| success_count | INT | DEFAULT 0 | 成功数 |
| failed_count | INT | DEFAULT 0 | 失败数 |
| task_status | VARCHAR(20) | DEFAULT 'created' | created/running/paused/completed/cancelled |
| strategy | VARCHAR(20) | DEFAULT 'immediate' | immediate/scheduled/staged |
| scheduled_time | TIMESTAMP | | 定时执行时间 |
| batch_size | INT | DEFAULT 100 | 分批大小 |
| retry_count | INT | DEFAULT 3 | 最大重试次数 |
| status | CHAR(1) | DEFAULT '1' | 1-启用 0-禁用 |
| del_flag | CHAR(1) | DEFAULT '0' | 0-正常 1-删除 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| create_by | BIGINT | | 创建人 |
| update_by | BIGINT | | 更新人 |

**索引**：
- `(tenant_id, task_status, del_flag)` — 按状态查询任务
- `(product_id, del_flag)` — 按产品查询

---

### 3. ota_device_status（OTA 设备升级状态表）

**数据库**：openiot_device

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| task_id | BIGINT | NOT NULL | 升级任务ID |
| device_id | BIGINT | NOT NULL | 设备ID |
| firmware_version_id | BIGINT | NOT NULL | 目标固件版本 |
| upgrade_status | VARCHAR(20) | NOT NULL DEFAULT 'pending' | pending/pushing/downloading/installing/success/failed |
| progress | INT | DEFAULT 0 | 进度百分比 0-100 |
| current_version | VARCHAR(50) | | 当前固件版本 |
| target_version | VARCHAR(50) | | 目标固件版本 |
| downloaded_bytes | BIGINT | DEFAULT 0 | 已下载字节数（断点续传） |
| error_code | VARCHAR(50) | | 错误码 |
| error_message | TEXT | | 错误详情 |
| retry_count | INT | DEFAULT 0 | 已重试次数 |
| start_time | TIMESTAMP | | 开始时间 |
| finish_time | TIMESTAMP | | 完成时间 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |

**索引**：
- `UNIQUE (task_id, device_id)` — 任务+设备唯一
- `(task_id, upgrade_status)` — 按状态统计
- `(device_id, create_time DESC)` — 设备升级历史

**状态机**：
```
pending → pushing → downloading → installing → success
  |          |           |             |
  +--------->+-----------+------------>+---> failed
                         |
                     (retry < max)
                         |
                     downloading (重试)
```

---

### 4. device_shadow（设备影子表）

**数据库**：openiot_device
**迁移脚本**：V1.8.0__add_device_shadow.sql

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| device_id | BIGINT | NOT NULL | 设备ID |
| reported | JSONB | DEFAULT '{}' | 设备上报属性 |
| desired | JSONB | DEFAULT '{}' | 期望属性 |
| delta | JSONB | DEFAULT '{}' | 差异（desired - reported） |
| version | BIGINT | DEFAULT 0 | 乐观锁版本号 |
| reported_time | TIMESTAMP | | 最后 reported 更新时间 |
| desired_time | TIMESTAMP | | 最后 desired 更新时间 |
| metadata | JSONB | DEFAULT '{}' | 元数据 |
| status | CHAR(1) | DEFAULT '1' | 1-启用 0-禁用 |
| del_flag | CHAR(1) | DEFAULT '0' | 0-正常 1-删除 |
| create_time | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| update_time | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| create_by | BIGINT | | 创建人 |
| update_by | BIGINT | | 更新人 |

**索引**：
- `UNIQUE (device_id, del_flag)` — 每设备一条影子
- `(tenant_id, del_flag)` — 租户查询

**乐观锁更新 SQL**：
```sql
UPDATE device_shadow
SET reported = :reported, delta = :delta,
    version = version + 1, reported_time = NOW(), update_time = NOW()
WHERE device_id = :deviceId AND version = :expectedVersion AND del_flag = '0'
```

**Redis 缓存**：
- Key: `device:shadow:{deviceId}`
- TTL: 1 小时，活跃设备上报时自动续期

---

## 实体关系

```
Product (1) ──── (N) FirmwareVersion
Product (1) ──── (N) Device
Device  (1) ──── (1) DeviceShadow
FirmwareVersion (1) ──── (N) OtaUpgradeTask
OtaUpgradeTask  (1) ──── (N) OtaDeviceStatus
Device          (1) ──── (N) OtaDeviceStatus
```
