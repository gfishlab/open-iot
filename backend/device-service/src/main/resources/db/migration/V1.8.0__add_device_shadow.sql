-- =============================================
-- V1.8.0: 设备影子表
-- =============================================

CREATE TABLE IF NOT EXISTS device_shadow (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    device_id     BIGINT       NOT NULL,
    reported      JSONB        DEFAULT '{}',
    desired       JSONB        DEFAULT '{}',
    delta         JSONB        DEFAULT '{}',
    version       BIGINT       DEFAULT 0,
    reported_time TIMESTAMP,
    desired_time  TIMESTAMP,
    metadata      JSONB        DEFAULT '{}',
    status        CHAR(1)      DEFAULT '1',
    del_flag      CHAR(1)      DEFAULT '0',
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT
);

COMMENT ON TABLE device_shadow IS '设备影子表';
COMMENT ON COLUMN device_shadow.id IS '主键';
COMMENT ON COLUMN device_shadow.tenant_id IS '租户ID';
COMMENT ON COLUMN device_shadow.device_id IS '设备ID';
COMMENT ON COLUMN device_shadow.reported IS '设备上报属性（JSONB）';
COMMENT ON COLUMN device_shadow.desired IS '期望属性（JSONB）';
COMMENT ON COLUMN device_shadow.delta IS '差异值（desired - reported）';
COMMENT ON COLUMN device_shadow.version IS '乐观锁版本号';
COMMENT ON COLUMN device_shadow.reported_time IS '最后 reported 更新时间';
COMMENT ON COLUMN device_shadow.desired_time IS '最后 desired 更新时间';
COMMENT ON COLUMN device_shadow.metadata IS '元数据（JSONB）';
COMMENT ON COLUMN device_shadow.status IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN device_shadow.del_flag IS '删除标志：0-正常 1-删除';

-- 每设备一条影子
CREATE UNIQUE INDEX IF NOT EXISTS uk_shadow_device
    ON device_shadow(device_id, del_flag);
-- 租户查询索引
CREATE INDEX IF NOT EXISTS idx_shadow_tenant
    ON device_shadow(tenant_id, del_flag);
