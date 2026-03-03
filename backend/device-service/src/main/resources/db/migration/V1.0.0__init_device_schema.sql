-- ========================================
-- 设备服务数据库初始化脚本
-- Version: 1.0.0
-- Description: 创建设备相关表
-- ========================================

-- 创建设备表
CREATE TABLE IF NOT EXISTS device (
    id BIGSERIAL PRIMARY KEY,
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
    update_by BIGINT
);

COMMENT ON TABLE device IS '设备表';
COMMENT ON COLUMN device.id IS '主键';
COMMENT ON COLUMN device.tenant_id IS '租户ID';
COMMENT ON COLUMN device.device_code IS '设备编码';
COMMENT ON COLUMN device.device_name IS '设备名称';
COMMENT ON COLUMN device.device_token IS '设备认证Token';
COMMENT ON COLUMN device.protocol_type IS '协议类型：MQTT/TCP/HTTP';
COMMENT ON COLUMN device.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN device.delete_flag IS '删除标记：0-正常，1-已删除';
COMMENT ON COLUMN device.create_time IS '创建时间';
COMMENT ON COLUMN device.update_time IS '更新时间';
COMMENT ON COLUMN device.create_by IS '创建人';
COMMENT ON COLUMN device.update_by IS '更新人';

-- ========================================
-- 创建索引
-- ========================================

-- 设备表索引
CREATE INDEX IF NOT EXISTS idx_device_tenant ON device(tenant_id, delete_flag);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_device ON device(tenant_id, device_code);
CREATE INDEX IF NOT EXISTS idx_device_token ON device(device_token);
CREATE INDEX IF NOT EXISTS idx_device_status ON device(status, delete_flag);

-- ========================================
-- 说明
-- ========================================

-- 注意：
-- 1. tenant_id 字段关联 openiot_tenant 数据库的 tenant 表
-- 2. 跨数据库关联需要通过应用层实现（API 调用）
-- 3. 不能使用外键约束（跨数据库）
