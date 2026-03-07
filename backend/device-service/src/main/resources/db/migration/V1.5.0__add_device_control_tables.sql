-- ========================================
-- 设备控制相关表
-- Version: 1.5.0
-- Description: 服务调用、命令下发、属性设置记录
-- ========================================

-- ========================================
-- 1. 服务调用记录表
-- ========================================
CREATE TABLE IF NOT EXISTS service_call_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    service_identifier VARCHAR(50) NOT NULL,  -- 服务标识符
    service_name VARCHAR(100) NOT NULL,         -- 服务名称
    input_params JSONB,                          -- 输入参数
    output_params JSONB,                         -- 输出参数
    call_type VARCHAR(20) NOT NULL,              -- 调用方式：sync-同步，async-异步
    status VARCHAR(20) NOT NULL,                 -- 状态：pending-待调用，calling-调用中，success-成功，failed-失败，timeout-超时
    error_message TEXT,                           -- 错误消息
    call_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 调用时间
    finish_time TIMESTAMP,                        -- 完成时间
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE service_call_record IS '服务调用记录表';
COMMENT ON COLUMN service_call_record.service_identifier IS '服务标识符';
COMMENT ON COLUMN service_call_record.input_params IS '输入参数（JSON）';
COMMENT ON COLUMN service_call_record.output_params IS '输出参数（JSON）';
COMMENT ON COLUMN service_call_record.status IS '状态：pending-待调用，calling-调用中，success-成功，failed-失败，timeout-超时';

-- 索引
CREATE INDEX IF NOT EXISTS idx_service_call_device ON service_call_record(device_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_service_call_status ON service_call_record(status, create_time DESC);

-- ========================================
-- 2. 属性设置记录表
-- ========================================
CREATE TABLE IF NOT EXISTS property_set_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    property_identifier VARCHAR(50) NOT NULL, -- 属性标识符
    property_name VARCHAR(100) NOT NULL,        -- 属性名称
    old_value JSONB,                              -- 原值
    new_value JSONB NOT NULL,                     -- 新值
    set_type VARCHAR(20) NOT NULL,               -- 设置类型：user-用户设置，system-系统设置
    status VARCHAR(20) NOT NULL,                 -- 状态：pending-待设置，success-成功，failed-失败
    error_message TEXT,                           -- 错误消息
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE property_set_record IS '属性设置记录表';
COMMENT ON COLUMN property_set_record.old_value IS '原值';
COMMENT ON COLUMN property_set_record.new_value IS '新值';
COMMENT ON COLUMN property_set_record.set_type IS '设置类型：user-用户设置，system-系统设置';
COMMENT ON COLUMN property_set_record.status IS '状态：pending-待设置，success-成功，failed-失败';

-- 索引
CREATE INDEX IF NOT EXISTS idx_property_set_device ON property_set_record(device_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_property_set_property ON property_set_record(property_identifier, create_time DESC);

-- ========================================
-- 3. 命令下发记录表
-- ========================================
CREATE TABLE IF NOT EXISTS command_send_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    command_identifier VARCHAR(50) NOT NULL,   -- 命令标识符
    command_name VARCHAR(100) NOT NULL,         -- 命令名称
    command_params JSONB NOT NULL,               -- 命令参数
    status VARCHAR(20) NOT NULL,                 -- 状态：pending-待下发，sent-已发送，success-成功，failed-失败，timeout-超时
    response_data JSONB,                         -- 响应数据
    error_message TEXT,                           -- 错误消息
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 下发时间
    response_time TIMESTAMP,                      -- 响应时间
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE command_send_record IS '命令下发记录表';
COMMENT ON COLUMN command_send_record.command_identifier IS '命令标识符';
COMMENT ON COLUMN command_send_record.command_params IS '命令参数（JSON）';
COMMENT ON COLUMN command_send_record.response_data IS '响应数据（JSON）';
COMMENT ON COLUMN command_send_record.status IS '状态：pending-待下发，sent-已发送，success-成功，failed-失败，timeout-超时';

-- 索引
CREATE INDEX IF NOT EXISTS idx_command_send_device ON command_send_record(device_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_command_send_status ON command_send_record(status, create_time DESC);
