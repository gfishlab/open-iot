-- ========================================
-- Open-IoT 平台核心功能数据库表
-- Version: 1.1.0
-- Description: 创建产品管理、规则引擎、告警管理等核心表
-- ========================================

-- ========================================
-- 1. 产品表
-- ========================================
CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_key VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_type VARCHAR(20) DEFAULT 'DEVICE',
    protocol_type VARCHAR(20) NOT NULL,
    node_type VARCHAR(20) DEFAULT 'DIRECT',
    data_format VARCHAR(20) DEFAULT 'JSON',
    thing_model JSONB,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_tenant_product_key UNIQUE (tenant_id, product_key)
);

COMMENT ON TABLE product IS '产品表';
COMMENT ON COLUMN product.id IS '主键';
COMMENT ON COLUMN product.tenant_id IS '租户ID';
COMMENT ON COLUMN product.product_key IS '产品密钥（租户内唯一）';
COMMENT ON COLUMN product.product_name IS '产品名称';
COMMENT ON COLUMN product.product_type IS '产品类型（DEVICE/GATEWAY）';
COMMENT ON COLUMN product.protocol_type IS '协议类型（MQTT/HTTP/CoAP/LwM2M/CUSTOM）';
COMMENT ON COLUMN product.node_type IS '节点类型（DIRECT/GATEWAY）';
COMMENT ON COLUMN product.data_format IS '数据格式（JSON/XML/BINARY/CUSTOM）';
COMMENT ON COLUMN product.thing_model IS '物模型定义（JSON格式）';
COMMENT ON COLUMN product.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN product.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_product_tenant ON product(tenant_id, delete_flag);

-- ========================================
-- 2. 修改设备表，添加 product_id 字段
-- ========================================
ALTER TABLE device ADD COLUMN IF NOT EXISTS product_id BIGINT;

-- 添加外键约束（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_device_product'
        AND table_name = 'device'
    ) THEN
        ALTER TABLE device ADD CONSTRAINT fk_device_product
        FOREIGN KEY (product_id) REFERENCES product(id);
    END IF;
END;
$$;

-- 创建索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_device_product ON device(product_id);

COMMENT ON COLUMN device.product_id IS '产品ID';

-- ========================================
-- 3. 解析规则表
-- ========================================
CREATE TABLE IF NOT EXISTS parse_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    rule_config JSONB NOT NULL,
    priority INT DEFAULT 0,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_parse_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE parse_rule IS '解析规则表';
COMMENT ON COLUMN parse_rule.id IS '主键';
COMMENT ON COLUMN parse_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN parse_rule.product_id IS '产品ID';
COMMENT ON COLUMN parse_rule.rule_name IS '规则名称';
COMMENT ON COLUMN parse_rule.rule_type IS '规则类型（JSON/JAVASCRIPT/REGEX/BINARY）';
COMMENT ON COLUMN parse_rule.rule_config IS '规则配置（JSON格式）';
COMMENT ON COLUMN parse_rule.priority IS '优先级（数字越大优先级越高）';
COMMENT ON COLUMN parse_rule.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN parse_rule.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_parse_rule_product ON parse_rule(product_id, priority DESC, delete_flag);

-- ========================================
-- 4. 映射规则表
-- ========================================
CREATE TABLE IF NOT EXISTS mapping_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    field_mappings JSONB NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_mapping_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE mapping_rule IS '映射规则表';
COMMENT ON COLUMN mapping_rule.id IS '主键';
COMMENT ON COLUMN mapping_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN mapping_rule.product_id IS '产品ID';
COMMENT ON COLUMN mapping_rule.rule_name IS '规则名称';
COMMENT ON COLUMN mapping_rule.field_mappings IS '字段映射配置（JSON格式）';
COMMENT ON COLUMN mapping_rule.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN mapping_rule.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_mapping_rule_product ON mapping_rule(product_id, delete_flag);

-- ========================================
-- 5. 转发规则表
-- ========================================
CREATE TABLE IF NOT EXISTS forward_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    rule_name VARCHAR(100) NOT NULL,
    rule_sql TEXT,
    target_type VARCHAR(20) NOT NULL,
    target_config JSONB NOT NULL,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_forward_rule_product FOREIGN KEY (product_id) REFERENCES product(id)
);

COMMENT ON TABLE forward_rule IS '转发规则表';
COMMENT ON COLUMN forward_rule.id IS '主键';
COMMENT ON COLUMN forward_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN forward_rule.product_id IS '产品ID（NULL表示所有产品）';
COMMENT ON COLUMN forward_rule.rule_name IS '规则名称';
COMMENT ON COLUMN forward_rule.rule_sql IS '过滤条件（SQL WHERE子句）';
COMMENT ON COLUMN forward_rule.target_type IS '目标类型（KAFKA/HTTP/MQTT/INFLUXDB）';
COMMENT ON COLUMN forward_rule.target_config IS '目标配置（JSON格式）';
COMMENT ON COLUMN forward_rule.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN forward_rule.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_forward_rule_tenant ON forward_rule(tenant_id, delete_flag);

-- ========================================
-- 6. 告警规则表
-- ========================================
CREATE TABLE IF NOT EXISTS alarm_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    device_id BIGINT,
    rule_name VARCHAR(100) NOT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    condition_expression TEXT NOT NULL,
    notify_config JSONB,
    status CHAR(1) DEFAULT '1',
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT fk_alarm_rule_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_alarm_rule_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE alarm_rule IS '告警规则表';
COMMENT ON COLUMN alarm_rule.id IS '主键';
COMMENT ON COLUMN alarm_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN alarm_rule.product_id IS '产品ID（NULL表示所有产品）';
COMMENT ON COLUMN alarm_rule.device_id IS '设备ID（NULL表示所有设备）';
COMMENT ON COLUMN alarm_rule.rule_name IS '规则名称';
COMMENT ON COLUMN alarm_rule.alarm_level IS '告警级别（CRITICAL/WARNING/INFO）';
COMMENT ON COLUMN alarm_rule.condition_expression IS '触发条件（Aviator表达式）';
COMMENT ON COLUMN alarm_rule.notify_config IS '通知配置（JSON格式）';
COMMENT ON COLUMN alarm_rule.status IS '状态（1=启用，0=禁用）';
COMMENT ON COLUMN alarm_rule.delete_flag IS '删除标记（0=未删除，1=已删除）';

CREATE INDEX idx_alarm_rule_tenant ON alarm_rule(tenant_id, delete_flag);

-- ========================================
-- 7. 告警记录表
-- ========================================
CREATE TABLE IF NOT EXISTS alarm_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    alarm_rule_id BIGINT NOT NULL,
    device_id BIGINT,
    alarm_level VARCHAR(20) NOT NULL,
    alarm_title VARCHAR(200) NOT NULL,
    alarm_content TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    trigger_time TIMESTAMP NOT NULL,
    recover_time TIMESTAMP,
    ack_time TIMESTAMP,
    ack_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alarm_record_rule FOREIGN KEY (alarm_rule_id) REFERENCES alarm_rule(id),
    CONSTRAINT fk_alarm_record_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE alarm_record IS '告警记录表';
COMMENT ON COLUMN alarm_record.id IS '主键';
COMMENT ON COLUMN alarm_record.tenant_id IS '租户ID';
COMMENT ON COLUMN alarm_record.alarm_rule_id IS '告警规则ID';
COMMENT ON COLUMN alarm_record.device_id IS '设备ID';
COMMENT ON COLUMN alarm_record.alarm_level IS '告警级别（CRITICAL/WARNING/INFO）';
COMMENT ON COLUMN alarm_record.alarm_title IS '告警标题';
COMMENT ON COLUMN alarm_record.alarm_content IS '告警内容';
COMMENT ON COLUMN alarm_record.status IS '状态（ACTIVE/ACKNOWLEDGED/RECOVERED）';
COMMENT ON COLUMN alarm_record.trigger_time IS '触发时间';
COMMENT ON COLUMN alarm_record.recover_time IS '恢复时间';
COMMENT ON COLUMN alarm_record.ack_time IS '确认时间';
COMMENT ON COLUMN alarm_record.ack_by IS '确认人ID';

CREATE INDEX idx_alarm_record_tenant ON alarm_record(tenant_id, trigger_time DESC);
CREATE INDEX idx_alarm_record_device ON alarm_record(device_id, trigger_time DESC);
CREATE INDEX idx_alarm_record_status ON alarm_record(status, trigger_time DESC);

-- ========================================
-- 8. 设备服务调用表
-- ========================================
CREATE TABLE IF NOT EXISTS device_service_invoke (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    service_identifier VARCHAR(100) NOT NULL,
    invoke_id VARCHAR(50) NOT NULL,
    invoke_type VARCHAR(20) DEFAULT 'SYNC',
    input_data JSONB,
    output_data JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    invoke_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complete_time TIMESTAMP,
    CONSTRAINT uk_invoke_id UNIQUE (invoke_id),
    CONSTRAINT fk_service_invoke_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE device_service_invoke IS '设备服务调用表';
COMMENT ON COLUMN device_service_invoke.id IS '主键';
COMMENT ON COLUMN device_service_invoke.tenant_id IS '租户ID';
COMMENT ON COLUMN device_service_invoke.device_id IS '设备ID';
COMMENT ON COLUMN device_service_invoke.service_identifier IS '服务标识符';
COMMENT ON COLUMN device_service_invoke.invoke_id IS '调用ID（全局唯一）';
COMMENT ON COLUMN device_service_invoke.invoke_type IS '调用类型（SYNC/ASYNC）';
COMMENT ON COLUMN device_service_invoke.input_data IS '输入参数（JSON格式）';
COMMENT ON COLUMN device_service_invoke.output_data IS '输出结果（JSON格式）';
COMMENT ON COLUMN device_service_invoke.status IS '状态（PENDING/SUCCESS/FAILED/TIMEOUT）';
COMMENT ON COLUMN device_service_invoke.error_message IS '错误信息';
COMMENT ON COLUMN device_service_invoke.invoke_time IS '调用时间';
COMMENT ON COLUMN device_service_invoke.complete_time IS '完成时间';

CREATE INDEX idx_service_invoke_device ON device_service_invoke(device_id, invoke_time DESC);
CREATE INDEX idx_service_invoke_status ON device_service_invoke(status, invoke_time);

-- ========================================
-- 9. 设备属性表
-- ========================================
CREATE TABLE IF NOT EXISTS device_property (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    property_identifier VARCHAR(100) NOT NULL,
    property_value TEXT NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_property_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT uk_device_property UNIQUE (device_id, property_identifier)
);

COMMENT ON TABLE device_property IS '设备属性表（当前状态）';
COMMENT ON COLUMN device_property.id IS '主键';
COMMENT ON COLUMN device_property.tenant_id IS '租户ID';
COMMENT ON COLUMN device_property.device_id IS '设备ID';
COMMENT ON COLUMN device_property.property_identifier IS '属性标识符';
COMMENT ON COLUMN device_property.property_value IS '属性值（JSON序列化）';
COMMENT ON COLUMN device_property.data_type IS '数据类型（INT/FLOAT/STRING/BOOLEAN/...）';
COMMENT ON COLUMN device_property.update_time IS '更新时间';

CREATE INDEX idx_property_update_time ON device_property(update_time);

-- ========================================
-- 10. 设备事件表
-- ========================================
CREATE TABLE IF NOT EXISTS device_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    event_identifier VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_data JSONB,
    event_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_event_device FOREIGN KEY (device_id) REFERENCES device(id)
);

COMMENT ON TABLE device_event IS '设备事件表';
COMMENT ON COLUMN device_event.id IS '主键';
COMMENT ON COLUMN device_event.tenant_id IS '租户ID';
COMMENT ON COLUMN device_event.device_id IS '设备ID';
COMMENT ON COLUMN device_event.event_identifier IS '事件标识符';
COMMENT ON COLUMN device_event.event_type IS '事件类型（INFO/WARNING/ERROR）';
COMMENT ON COLUMN device_event.event_data IS '事件数据（JSON格式）';
COMMENT ON COLUMN device_event.event_time IS '事件时间';
COMMENT ON COLUMN device_event.create_time IS '创建时间';

CREATE INDEX idx_device_event_device ON device_event(device_id, event_time DESC);
CREATE INDEX idx_device_event_time ON device_event(event_time);
