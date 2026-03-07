-- 数据转发配置表
CREATE TABLE IF NOT EXISTS data_forward_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    config_name VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    endpoint_config TEXT,
    filter_condition TEXT,
    transform_rule TEXT,
    batch_size INTEGER DEFAULT 100,
    batch_timeout_ms INTEGER DEFAULT 5000,
    retry_times INTEGER DEFAULT 3,
    retry_interval_ms INTEGER DEFAULT 1000,
    status CHAR(1) DEFAULT '1',
    del_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE data_forward_config IS '数据转发配置表';
COMMENT ON COLUMN data_forward_config.id IS '主键';
COMMENT ON COLUMN data_forward_config.tenant_id IS '租户ID';
COMMENT ON COLUMN data_forward_config.config_name IS '配置名称';
COMMENT ON COLUMN data_forward_config.target_type IS '目标类型：http-kafka-mqtt-influxdb-mongodb';
COMMENT ON COLUMN data_forward_config.endpoint_config IS '端点配置（JSON）';
COMMENT ON COLUMN data_forward_config.filter_condition IS '过滤条件（JSON）';
COMMENT ON COLUMN data_forward_config.transform_rule IS '转换规则（JSON）';
COMMENT ON COLUMN data_forward_config.batch_size IS '批量大小';
COMMENT ON COLUMN data_forward_config.batch_timeout_ms IS '批量超时时间（毫秒）';
COMMENT ON COLUMN data_forward_config.retry_times IS '重试次数';
COMMENT ON COLUMN data_forward_config.retry_interval_ms IS '重试间隔（毫秒）';
COMMENT ON COLUMN data_forward_config.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN data_forward_config.del_flag IS '删除标记：0-正常，1-已删除';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_data_forward_config_tenant ON data_forward_config(tenant_id, del_flag);
CREATE INDEX IF NOT EXISTS idx_data_forward_config_status ON data_forward_config(status, del_flag);

-- 数据转发日志表
CREATE TABLE IF NOT EXISTS data_forward_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    config_id BIGINT NOT NULL,
    device_id BIGINT,
    device_code VARCHAR(100),
    data_type VARCHAR(50),
    target VARCHAR(50),
    forward_status VARCHAR(20),
    response TEXT,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    duration BIGINT,
    forward_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE data_forward_log IS '数据转发日志表';
COMMENT ON COLUMN data_forward_log.id IS '主键';
COMMENT ON COLUMN data_forward_log.tenant_id IS '租户ID';
COMMENT ON COLUMN data_forward_log.config_id IS '转发配置ID';
COMMENT ON COLUMN data_forward_log.device_id IS '设备ID';
COMMENT ON COLUMN data_forward_log.device_code IS '设备编码';
COMMENT ON COLUMN data_forward_log.data_type IS '数据类型：property-event-status';
COMMENT ON COLUMN data_forward_log.target IS '转发目标';
COMMENT ON COLUMN data_forward_log.forward_status IS '转发状态：success-fail-pending';
COMMENT ON COLUMN data_forward_log.response IS '响应内容';
COMMENT ON COLUMN data_forward_log.error_message IS '错误信息';
COMMENT ON COLUMN data_forward_log.retry_count IS '重试次数';
COMMENT ON COLUMN data_forward_log.duration IS '耗时（毫秒）';
COMMENT ON COLUMN data_forward_log.forward_time IS '转发时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_data_forward_log_tenant ON data_forward_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_data_forward_log_config ON data_forward_log(config_id);
CREATE INDEX IF NOT EXISTS idx_data_forward_log_device ON data_forward_log(device_id);
CREATE INDEX IF NOT EXISTS idx_data_forward_log_status ON data_forward_log(forward_status);
CREATE INDEX IF NOT EXISTS idx_data_forward_log_time ON data_forward_log(forward_time);
