-- ========================================
-- Open-IoT 数据库初始化脚本
-- Version: 1.0.0
-- Description: 创建核心业务表
-- ========================================

-- 创建租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
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

COMMENT ON TABLE tenant IS '租户表';
COMMENT ON COLUMN tenant.id IS '主键';
COMMENT ON COLUMN tenant.tenant_code IS '租户编码';
COMMENT ON COLUMN tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN tenant.contact_email IS '联系邮箱';
COMMENT ON COLUMN tenant.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN tenant.delete_flag IS '删除标记：0-正常，1-已删除';

-- 创建系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '主键';
COMMENT ON COLUMN sys_user.tenant_id IS '租户ID（NULL表示平台管理员）';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（加密）';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.role IS '角色：ADMIN-平台管理员，TENANT_ADMIN-租户管理员';
COMMENT ON COLUMN sys_user.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN sys_user.delete_flag IS '删除标记';

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
COMMENT ON COLUMN device.delete_flag IS '删除标记';

-- 创建设备轨迹表
CREATE TABLE IF NOT EXISTS device_trajectory (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    speed DECIMAL(5, 2),
    heading DECIMAL(5, 2),
    event_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE device_trajectory IS '设备轨迹表';
COMMENT ON COLUMN device_trajectory.id IS '主键';
COMMENT ON COLUMN device_trajectory.tenant_id IS '租户ID';
COMMENT ON COLUMN device_trajectory.device_id IS '设备ID';
COMMENT ON COLUMN device_trajectory.latitude IS '纬度';
COMMENT ON COLUMN device_trajectory.longitude IS '经度';
COMMENT ON COLUMN device_trajectory.speed IS '速度(km/h)';
COMMENT ON COLUMN device_trajectory.heading IS '航向角(度)';
COMMENT ON COLUMN device_trajectory.event_time IS '事件时间';

-- ========================================
-- 创建索引
-- ========================================

-- 租户表索引
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant(status, delete_flag);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_user_tenant ON sys_user(tenant_id);

-- 设备表索引
CREATE INDEX IF NOT EXISTS idx_device_tenant ON device(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_device ON device(tenant_id, device_code);
CREATE INDEX IF NOT EXISTS idx_device_token ON device(device_token);

-- 轨迹表索引
CREATE INDEX IF NOT EXISTS idx_trajectory_tenant_device_time
    ON device_trajectory(tenant_id, device_id, event_time);
CREATE INDEX IF NOT EXISTS idx_trajectory_time ON device_trajectory(event_time);
