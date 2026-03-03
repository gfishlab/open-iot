-- ========================================
-- 租户服务数据库初始化脚本
-- Version: 1.0.0
-- Description: 创建租户、用户相关表
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
COMMENT ON COLUMN tenant.create_time IS '创建时间';
COMMENT ON COLUMN tenant.update_time IS '更新时间';
COMMENT ON COLUMN tenant.create_by IS '创建人';
COMMENT ON COLUMN tenant.update_by IS '更新人';

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
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.create_by IS '创建人';
COMMENT ON COLUMN sys_user.update_by IS '更新人';

-- ========================================
-- 创建索引
-- ========================================

-- 租户表索引
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant(status, delete_flag);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_status ON sys_user(status, delete_flag);
