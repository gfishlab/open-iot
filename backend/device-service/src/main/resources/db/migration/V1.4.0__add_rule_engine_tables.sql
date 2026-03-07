-- ========================================
-- 规则引擎相关表
-- Version: 1.4.0
-- Description: 规则定义、条件、动作、告警
-- ========================================

-- ========================================
-- 1. 规则表
-- ========================================
CREATE TABLE IF NOT EXISTS rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,          -- 规则名称
    rule_type VARCHAR(20) NOT NULL,           -- 规则类型：device-设备规则，product-产品规则
    target_id BIGINT NOT NULL,                -- 目标ID（设备ID或产品ID）
    status CHAR(1) DEFAULT '1',               -- 状态：0-禁用，1-启用
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE rule IS '规则表';
COMMENT ON COLUMN rule.id IS '主键';
COMMENT ON COLUMN rule.tenant_id IS '租户ID';
COMMENT ON COLUMN rule.rule_name IS '规则名称';
COMMENT ON COLUMN rule.rule_type IS '规则类型：device-设备规则，product-产品规则';
COMMENT ON COLUMN rule.target_id IS '目标ID（设备ID或产品ID）';
COMMENT ON COLUMN rule.status IS '状态：0-禁用，1-启用';

-- 索引
CREATE INDEX IF NOT EXISTS idx_rule_target ON rule(rule_type, target_id, delete_flag);
CREATE INDEX IF NOT EXISTS idx_rule_tenant ON rule(tenant_id, delete_flag);

-- ========================================
-- 2. 规则条件表
-- ========================================
CREATE TABLE IF NOT EXISTS rule_condition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    property_identifier VARCHAR(50) NOT NULL, -- 属性标识符（如 temperature）
    operator VARCHAR(20) NOT NULL,            -- 操作符：gt, lt, eq, gte, lte, between
    threshold_value VARCHAR(100),             -- 阈值（JSON格式，支持单值和范围）
    condition_order INT DEFAULT 0,            -- 条件顺序（多个条件时使用）
    logic_relation VARCHAR(10) DEFAULT 'AND', -- 逻辑关系：AND, OR
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE rule_condition IS '规则条件表';
COMMENT ON COLUMN rule_condition.property_identifier IS '属性标识符';
COMMENT ON COLUMN rule_condition.operator IS '操作符：gt-大于，lt-小于，eq-等于，gte-大于等于，lte-小于等于，between-区间';
COMMENT ON COLUMN rule_condition.threshold_value IS '阈值（JSON）：单值或范围 {"min": 0, "max": 100}';
COMMENT ON COLUMN rule_condition.condition_order IS '条件顺序';
COMMENT ON COLUMN rule_condition.logic_relation IS '逻辑关系：AND, OR';

-- 索引
CREATE INDEX IF NOT EXISTS idx_rule_condition_rule ON rule_condition(rule_id, delete_flag);

-- ========================================
-- 3. 规则动作表
-- ========================================
CREATE TABLE IF NOT EXISTS rule_action (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,        -- 动作类型：alert-告警，webhook-回调，device-设备控制
    action_config JSONB NOT NULL,            -- 动作配置（JSON格式）
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE rule_action IS '规则动作表';
COMMENT ON COLUMN rule_action.action_type IS '动作类型：alert-告警，webhook-Webhook回调，device-设备控制';
COMMENT ON COLUMN rule_action.action_config IS '动作配置（JSON）';

-- 示例配置：
-- alert类型：{"level": "critical", "message": "温度过高告警"}
-- webhook类型：{"url": "http://example.com/alert", "method": "POST"}
-- device类型：{"deviceId": 123, "service": "switch", "params": {"status": "off"}}

-- 索引
CREATE INDEX IF NOT EXISTS idx_rule_action_rule ON rule_action(rule_id, delete_flag);

-- ========================================
-- 4. 告警记录表
-- ========================================
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    alert_level VARCHAR(20) NOT NULL,        -- 告警级别：info, warning, critical
    alert_title VARCHAR(200) NOT NULL,      -- 告警标题
    alert_content TEXT,                      -- 告警内容
    alert_data JSONB,                        -- 告警数据（触发时的数据）
    status VARCHAR(20) DEFAULT 'pending',    -- 处理状态：pending-待处理，processing-处理中，resolved-已解决，ignored-已忽略
    handled_time TIMESTAMP,                 -- 处理时间
    handled_by BIGINT,                       -- 处理人ID
    delete_flag CHAR(1) DEFAULT '0',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE alert_record IS '告警记录表';
COMMENT ON COLUMN alert_record.alert_level IS '告警级别：info-信息，warning-警告，critical-严重';
COMMENT ON COLUMN alert_record.status IS '处理状态：pending-待处理，processing-处理中，resolved-已解决，ignored-已忽略';
COMMENT ON COLUMN alert_record.alert_data IS '告警数据（触发时的原始数据）';

-- 索引
CREATE INDEX IF NOT EXISTS idx_alert_device ON alert_record(device_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_alert_rule ON alert_record(rule_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_alert_status ON alert_record(status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_alert_time ON alert_record(create_time DESC);

-- ========================================
-- 使用示例
-- ========================================
-- 示例1：创建温度过高告警规则
-- INSERT INTO rule (tenant_id, rule_name, rule_type, target_id) VALUES (1, '温度过高告警', 'device', 3);
-- INSERT INTO rule_condition (tenant_id, rule_id, property_identifier, operator, threshold_value)
-- VALUES (1, 1, 'temperature', 'gt', '50');
-- INSERT INTO rule_action (tenant_id, rule_id, action_type, action_config)
-- VALUES (1, 1, 'alert', '{"level": "critical", "message": "温度超过50度"}');

-- 示例2：创建温湿度综合告警规则（多个条件）
-- INSERT INTO rule (tenant_id, rule_name, rule_type, target_id) VALUES (1, '温湿度异常告警', 'product', 2);
-- INSERT INTO rule_condition (tenant_id, rule_id, property_identifier, operator, threshold_value, condition_order, logic_relation)
-- VALUES
--   (1, 2, 'temperature', 'gt', '40', 1, 'AND'),
--   (1, 2, 'humidity', 'lt', '20', 2, 'AND');
-- INSERT INTO rule_action (tenant_id, rule_id, action_type, action_config)
-- VALUES (1, 2, 'alert', '{"level": "warning", "message": "温度过高或湿度过低"}');
