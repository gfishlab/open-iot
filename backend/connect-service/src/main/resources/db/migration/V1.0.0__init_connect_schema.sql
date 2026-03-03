-- ========================================
-- 连接服务数据库初始化脚本
-- Version: 1.0.0
-- Description: 创建设备连接、会话相关表
-- ========================================

-- 创建设备会话表
CREATE TABLE IF NOT EXISTS device_session (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    connection_type VARCHAR(20) NOT NULL,
    client_ip VARCHAR(50),
    client_port INT,
    connect_time TIMESTAMP NOT NULL,
    disconnect_time TIMESTAMP,
    status CHAR(1) DEFAULT '1',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE device_session IS '设备会话表';
COMMENT ON COLUMN device_session.id IS '主键';
COMMENT ON COLUMN device_session.tenant_id IS '租户ID';
COMMENT ON COLUMN device_session.device_id IS '设备ID';
COMMENT ON COLUMN device_session.session_id IS '会话ID';
COMMENT ON COLUMN device_session.connection_type IS '连接类型：TCP/WEBSOCKET/MQTT';
COMMENT ON COLUMN device_session.client_ip IS '客户端IP';
COMMENT ON COLUMN device_session.client_port IS '客户端端口';
COMMENT ON COLUMN device_session.connect_time IS '连接时间';
COMMENT ON COLUMN device_session.disconnect_time IS '断开时间';
COMMENT ON COLUMN device_session.status IS '状态：0-已断开，1-已连接';
COMMENT ON COLUMN device_session.create_time IS '创建时间';
COMMENT ON COLUMN device_session.update_time IS '更新时间';

-- 创建连接日志表
CREATE TABLE IF NOT EXISTS connection_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT,
    session_id VARCHAR(100),
    event_type VARCHAR(20) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    client_ip VARCHAR(50),
    detail TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE connection_log IS '连接日志表';
COMMENT ON COLUMN connection_log.id IS '主键';
COMMENT ON COLUMN connection_log.tenant_id IS '租户ID';
COMMENT ON COLUMN connection_log.device_id IS '设备ID';
COMMENT ON COLUMN connection_log.session_id IS '会话ID';
COMMENT ON COLUMN connection_log.event_type IS '事件类型：CONNECT/DISCONNECT/AUTH_FAIL/TIMEOUT';
COMMENT ON COLUMN connection_log.event_time IS '事件时间';
COMMENT ON COLUMN connection_log.client_ip IS '客户端IP';
COMMENT ON COLUMN connection_log.detail IS '详细信息';
COMMENT ON COLUMN connection_log.create_time IS '创建时间';

-- ========================================
-- 创建索引
-- ========================================

-- 会话表索引
CREATE INDEX IF NOT EXISTS idx_session_tenant_device ON device_session(tenant_id, device_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_session_id ON device_session(session_id);
CREATE INDEX IF NOT EXISTS idx_session_status ON device_session(status);
CREATE INDEX IF NOT EXISTS idx_session_connect_time ON device_session(connect_time DESC);

-- 连接日志索引
CREATE INDEX IF NOT EXISTS idx_log_tenant_device ON connection_log(tenant_id, device_id);
CREATE INDEX IF NOT EXISTS idx_log_event_time ON connection_log(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_log_event_type ON connection_log(event_type);

-- ========================================
-- 说明
-- ========================================

-- 注意：
-- 1. tenant_id 字段关联 openiot_tenant 数据库的 tenant 表
-- 2. device_id 字段关联 openiot_device 数据库的 device 表
-- 3. 跨数据库关联需要通过应用层实现（API 调用）
-- 4. 不能使用外键约束（跨数据库）
