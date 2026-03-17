-- =============================================
-- V1.7.0: OTA 升级相关表
-- 包含：firmware_version, ota_upgrade_task, ota_device_status
-- =============================================

-- 1. 固件版本表
CREATE TABLE IF NOT EXISTS firmware_version (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    firmware_name VARCHAR(100),
    version     VARCHAR(50)  NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_size   BIGINT,
    file_md5    VARCHAR(32),
    file_sha256 VARCHAR(64),
    description TEXT,
    status      CHAR(1)      DEFAULT '1',
    del_flag    CHAR(1)      DEFAULT '0',
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT
);

COMMENT ON TABLE firmware_version IS 'OTA 固件版本表';
COMMENT ON COLUMN firmware_version.id IS '主键';
COMMENT ON COLUMN firmware_version.tenant_id IS '租户ID';
COMMENT ON COLUMN firmware_version.product_id IS '关联产品ID';
COMMENT ON COLUMN firmware_version.firmware_name IS '固件名称';
COMMENT ON COLUMN firmware_version.version IS '版本号（semver）';
COMMENT ON COLUMN firmware_version.file_path IS '固件包相对存储路径';
COMMENT ON COLUMN firmware_version.file_size IS '文件大小（字节）';
COMMENT ON COLUMN firmware_version.file_md5 IS 'MD5 校验和';
COMMENT ON COLUMN firmware_version.file_sha256 IS 'SHA256 校验和';
COMMENT ON COLUMN firmware_version.description IS '版本说明';
COMMENT ON COLUMN firmware_version.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN firmware_version.del_flag IS '删除标志：0-正常 1-删除';

-- 同产品版本号唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_firmware_product_version
    ON firmware_version(product_id, version, del_flag);
-- 租户查询索引
CREATE INDEX IF NOT EXISTS idx_firmware_tenant
    ON firmware_version(tenant_id, del_flag);

-- 2. OTA 升级任务表
CREATE TABLE IF NOT EXISTS ota_upgrade_task (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    task_name           VARCHAR(100),
    product_id          BIGINT       NOT NULL,
    firmware_version_id BIGINT       NOT NULL,
    upgrade_scope       VARCHAR(20)  DEFAULT 'ALL',
    target_device_ids   JSONB,
    total_count         INT          DEFAULT 0,
    success_count       INT          DEFAULT 0,
    failed_count        INT          DEFAULT 0,
    task_status         VARCHAR(20)  DEFAULT 'created',
    strategy            VARCHAR(20)  DEFAULT 'immediate',
    scheduled_time      TIMESTAMP,
    batch_size          INT          DEFAULT 100,
    retry_count         INT          DEFAULT 3,
    status              CHAR(1)      DEFAULT '1',
    del_flag            CHAR(1)      DEFAULT '0',
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE ota_upgrade_task IS 'OTA 升级任务表';
COMMENT ON COLUMN ota_upgrade_task.id IS '主键';
COMMENT ON COLUMN ota_upgrade_task.tenant_id IS '租户ID';
COMMENT ON COLUMN ota_upgrade_task.task_name IS '任务名称';
COMMENT ON COLUMN ota_upgrade_task.product_id IS '目标产品ID';
COMMENT ON COLUMN ota_upgrade_task.firmware_version_id IS '目标固件版本ID';
COMMENT ON COLUMN ota_upgrade_task.upgrade_scope IS '升级范围：ALL-全部 SELECTED-指定设备';
COMMENT ON COLUMN ota_upgrade_task.target_device_ids IS '指定设备ID列表（JSONB数组）';
COMMENT ON COLUMN ota_upgrade_task.total_count IS '总设备数';
COMMENT ON COLUMN ota_upgrade_task.success_count IS '成功数';
COMMENT ON COLUMN ota_upgrade_task.failed_count IS '失败数';
COMMENT ON COLUMN ota_upgrade_task.task_status IS '任务状态：created/running/paused/completed/cancelled';
COMMENT ON COLUMN ota_upgrade_task.strategy IS '推送策略：immediate/scheduled/staged';
COMMENT ON COLUMN ota_upgrade_task.scheduled_time IS '定时执行时间';
COMMENT ON COLUMN ota_upgrade_task.batch_size IS '分批大小';
COMMENT ON COLUMN ota_upgrade_task.retry_count IS '最大重试次数';
COMMENT ON COLUMN ota_upgrade_task.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN ota_upgrade_task.del_flag IS '删除标志：0-正常 1-删除';

-- 按状态查询任务
CREATE INDEX IF NOT EXISTS idx_ota_task_tenant_status
    ON ota_upgrade_task(tenant_id, task_status, del_flag);
-- 按产品查询
CREATE INDEX IF NOT EXISTS idx_ota_task_product
    ON ota_upgrade_task(product_id, del_flag);

-- 3. OTA 设备升级状态表
CREATE TABLE IF NOT EXISTS ota_device_status (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    task_id             BIGINT       NOT NULL,
    device_id           BIGINT       NOT NULL,
    firmware_version_id BIGINT       NOT NULL,
    upgrade_status      VARCHAR(20)  NOT NULL DEFAULT 'pending',
    progress            INT          DEFAULT 0,
    current_version     VARCHAR(50),
    target_version      VARCHAR(50),
    downloaded_bytes    BIGINT       DEFAULT 0,
    error_code          VARCHAR(50),
    error_message       TEXT,
    retry_count         INT          DEFAULT 0,
    start_time          TIMESTAMP,
    finish_time         TIMESTAMP,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ota_device_status IS 'OTA 设备升级状态表';
COMMENT ON COLUMN ota_device_status.id IS '主键';
COMMENT ON COLUMN ota_device_status.tenant_id IS '租户ID';
COMMENT ON COLUMN ota_device_status.task_id IS '升级任务ID';
COMMENT ON COLUMN ota_device_status.device_id IS '设备ID';
COMMENT ON COLUMN ota_device_status.firmware_version_id IS '目标固件版本ID';
COMMENT ON COLUMN ota_device_status.upgrade_status IS '升级状态：pending/pushing/downloading/installing/success/failed';
COMMENT ON COLUMN ota_device_status.progress IS '进度百分比 0-100';
COMMENT ON COLUMN ota_device_status.current_version IS '当前固件版本';
COMMENT ON COLUMN ota_device_status.target_version IS '目标固件版本';
COMMENT ON COLUMN ota_device_status.downloaded_bytes IS '已下载字节数（断点续传）';
COMMENT ON COLUMN ota_device_status.error_code IS '错误码';
COMMENT ON COLUMN ota_device_status.error_message IS '错误详情';
COMMENT ON COLUMN ota_device_status.retry_count IS '已重试次数';
COMMENT ON COLUMN ota_device_status.start_time IS '开始时间';
COMMENT ON COLUMN ota_device_status.finish_time IS '完成时间';

-- 任务+设备唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_ota_device_task_device
    ON ota_device_status(task_id, device_id);
-- 按状态统计
CREATE INDEX IF NOT EXISTS idx_ota_device_task_status
    ON ota_device_status(task_id, upgrade_status);
-- 设备升级历史
CREATE INDEX IF NOT EXISTS idx_ota_device_history
    ON ota_device_status(device_id, create_time DESC);
