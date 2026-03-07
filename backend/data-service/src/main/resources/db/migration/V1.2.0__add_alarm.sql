-- 告警规则表
CREATE TABLE IF NOT EXISTS alarm_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    device_id BIGINT,
    rule_name VARCHAR(100) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_condition TEXT,
    duration_seconds INTEGER,
    content_template TEXT,
    recovery_condition TEXT,
    notify_type VARCHAR(50),
    notify_config TEXT,
    silence_enabled BOOLEAN DEFAULT FALSE,
    silence_seconds INTEGER DEFAULT 300,
    status CHAR(1) DEFAULT '1',
    del_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE alarm_rule IS '告警规则表';
COMMENT ON COLUMN alarm_rule.id IS '主键';
COMMENT ON COLUMN alarm_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN alarm_rule.product_id IS '产品ID（为空则适用所有产品）';
COMMENT ON COLUMN alarm_rule.device_id IS '设备ID（为空则适用所有设备）';
COMMENT ON COLUMN alarm_rule.rule_name IS '告警规则名称';
COMMENT ON COLUMN alarm_rule.alarm_level IS '告警级别：info-warning-critical-emergency';
COMMENT ON COLUMN alarm_rule.trigger_type IS '触发条件类型：threshold-expression-rate';
COMMENT ON COLUMN alarm_rule.trigger_condition IS '触发条件配置（JSON）';
COMMENT ON COLUMN alarm_rule.duration_seconds IS '持续时间（秒）';
COMMENT ON COLUMN alarm_rule.content_template IS '告警内容模板';
COMMENT ON COLUMN alarm_rule.recovery_condition IS '恢复条件';
COMMENT ON COLUMN alarm_rule.notify_type IS '通知方式：email-sms-webhook-none';
COMMENT ON COLUMN alarm_rule.notify_config IS '通知配置（JSON）';
COMMENT ON COLUMN alarm_rule.silence_enabled IS '是否启用静默期';
COMMENT ON COLUMN alarm_rule.silence_seconds IS '静默期时长（秒）';
COMMENT ON COLUMN alarm_rule.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN alarm_rule.del_flag IS '删除标记：0-正常，1-已删除';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_alarm_rule_tenant ON alarm_rule(tenant_id, del_flag);
CREATE INDEX IF NOT EXISTS idx_alarm_rule_product ON alarm_rule(product_id);
CREATE INDEX IF NOT EXISTS idx_alarm_rule_device ON alarm_rule(device_id);
CREATE INDEX IF NOT EXISTS idx_alarm_rule_status ON alarm_rule(status, del_flag);

-- 告警记录表
CREATE TABLE IF NOT EXISTS alarm_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    product_id BIGINT,
    device_id BIGINT,
    device_code VARCHAR(100),
    alarm_level VARCHAR(20) NOT NULL,
    alarm_title VARCHAR(200),
    alarm_content TEXT,
    trigger_value VARCHAR(500),
    alarm_status VARCHAR(20) NOT NULL,
    alarm_time TIMESTAMP NOT NULL,
    recover_time TIMESTAMP,
    acknowledge_time TIMESTAMP,
    acknowledged_by BIGINT,
    handleremark TEXT,
    notify_status VARCHAR(20) DEFAULT 'none',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE alarm_record IS '告警记录表';
COMMENT ON COLUMN alarm_record.id IS '主键';
COMMENT ON COLUMN alarm_record.tenant_id IS '租户ID';
COMMENT ON COLUMN alarm_record.rule_id IS '告警规则ID';
COMMENT ON COLUMN alarm_record.product_id IS '产品ID';
COMMENT ON COLUMN alarm_record.device_id IS '设备ID';
COMMENT ON COLUMN alarm_record.device_code IS '设备编码';
COMMENT ON COLUMN alarm_record.alarm_level IS '告警级别';
COMMENT ON COLUMN alarm_record.alarm_title IS '告警标题';
COMMENT ON COLUMN alarm_record.alarm_content IS '告警内容';
COMMENT ON COLUMN alarm_record.trigger_value IS '触发值';
COMMENT ON COLUMN alarm_record.alarm_status IS '告警状态：pending-active-acknowledged-resolved-closed';
COMMENT ON COLUMN alarm_record.alarm_time IS '告警时间';
COMMENT ON COLUMN alarm_record.recover_time IS '恢复时间';
COMMENT ON COLUMN alarm_record.acknowledge_time IS '确认时间';
COMMENT ON COLUMN alarm_record.acknowledged_by IS '确认人ID';
COMMENT ON COLUMN alarm_record.handleremark IS '处理备注';
COMMENT ON COLUMN alarm_record.notify_status IS '通知状态：none-sending-sent-failed';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_alarm_record_tenant ON alarm_record(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alarm_record_rule ON alarm_record(rule_id);
CREATE INDEX IF NOT EXISTS idx_alarm_record_device ON alarm_record(device_id);
CREATE INDEX IF NOT EXISTS idx_alarm_record_status ON alarm_record(alarm_status);
CREATE INDEX IF NOT EXISTS idx_alarm_record_time ON alarm_record(alarm_time);
CREATE INDEX IF NOT EXISTS idx_alarm_record_level ON alarm_record(alarm_level);
