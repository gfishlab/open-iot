-- ========================================
-- Open-IoT RBAC 权限模型表
-- Version: 1.1.0
-- Description: 创建严格 RBAC 权限模型相关表
-- ========================================

-- ========================================
-- 1. 角色表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,                          -- 租户ID（NULL表示平台级角色）
    role_code VARCHAR(50) NOT NULL,            -- 角色编码
    role_name VARCHAR(100) NOT NULL,           -- 角色名称
    description VARCHAR(255),                  -- 角色描述
    sort_order INT DEFAULT 0,                  -- 排序
    status CHAR(1) DEFAULT '1',                -- 状态：0-禁用，1-启用
    delete_flag CHAR(1) DEFAULT '0',           -- 删除标记
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '主键';
COMMENT ON COLUMN sys_role.tenant_id IS '租户ID（NULL表示平台级角色）';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.sort_order IS '排序';
COMMENT ON COLUMN sys_role.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN sys_role.delete_flag IS '删除标记';

-- ========================================
-- 2. 权限表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,                -- 父权限ID（用于树形结构）
    permission_code VARCHAR(100) NOT NULL,     -- 权限编码
    permission_name VARCHAR(100) NOT NULL,     -- 权限名称
    resource_type VARCHAR(20) NOT NULL,        -- 资源类型：MODULE/MENU/BUTTON/API
    resource_path VARCHAR(255),                -- 资源路径（API路径或菜单路径）
    icon VARCHAR(50),                          -- 图标
    sort_order INT DEFAULT 0,                  -- 排序
    status CHAR(1) DEFAULT '1',                -- 状态
    delete_flag CHAR(1) DEFAULT '0',           -- 删除标记
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

COMMENT ON TABLE sys_permission IS '权限表';
COMMENT ON COLUMN sys_permission.id IS '主键';
COMMENT ON COLUMN sys_permission.parent_id IS '父权限ID';
COMMENT ON COLUMN sys_permission.permission_code IS '权限编码';
COMMENT ON COLUMN sys_permission.permission_name IS '权限名称';
COMMENT ON COLUMN sys_permission.resource_type IS '资源类型：MODULE/MENU/BUTTON/API';
COMMENT ON COLUMN sys_permission.resource_path IS '资源路径';
COMMENT ON COLUMN sys_permission.icon IS '图标';
COMMENT ON COLUMN sys_permission.sort_order IS '排序';
COMMENT ON COLUMN sys_permission.status IS '状态';

-- ========================================
-- 3. 用户角色关联表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                   -- 用户ID
    role_id BIGINT NOT NULL,                   -- 角色ID
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- ========================================
-- 4. 角色权限关联表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,                   -- 角色ID
    permission_id BIGINT NOT NULL,             -- 权限ID
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限ID';

-- ========================================
-- 5. 创建索引
-- ========================================

-- 角色表索引
CREATE INDEX IF NOT EXISTS idx_role_tenant ON sys_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_code ON sys_role(role_code);

-- 权限表索引
CREATE INDEX IF NOT EXISTS idx_permission_parent ON sys_permission(parent_id);
CREATE INDEX IF NOT EXISTS idx_permission_type ON sys_permission(resource_type);

-- 用户角色表索引
CREATE INDEX IF NOT EXISTS idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role ON sys_user_role(role_id);

-- 角色权限表索引
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission ON sys_role_permission(permission_id);

-- ========================================
-- 6. 插入预置角色
-- ========================================

-- 平台管理员角色
INSERT INTO sys_role (tenant_id, role_code, role_name, description, sort_order, status)
VALUES (NULL, 'ADMIN', '平台管理员', '拥有系统所有权限', 1, '1');

-- 租户管理员角色
INSERT INTO sys_role (tenant_id, role_code, role_name, description, sort_order, status)
VALUES (NULL, 'TENANT_ADMIN', '租户管理员', '管理租户内用户和设备', 2, '1');

-- 租户普通用户角色
INSERT INTO sys_role (tenant_id, role_code, role_name, description, sort_order, status)
VALUES (NULL, 'TENANT_USER', '租户普通用户', '仅查看权限', 3, '1');

-- ========================================
-- 7. 插入预置权限
-- ========================================

-- 模块级权限
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, sort_order) VALUES
(0, 'system', '系统管理', 'MODULE', 1),
(0, 'tenant', '租户管理', 'MODULE', 2),
(0, 'user', '用户管理', 'MODULE', 3),
(0, 'device', '设备管理', 'MODULE', 4),
(0, 'data', '数据管理', 'MODULE', 5),
(0, 'monitor', '监控中心', 'MODULE', 6);

-- 获取模块ID
DO $$
DECLARE
    v_system_id BIGINT;
    v_tenant_id BIGINT;
    v_user_id BIGINT;
    v_device_id BIGINT;
    v_data_id BIGINT;
    v_monitor_id BIGINT;
BEGIN
    SELECT id INTO v_system_id FROM sys_permission WHERE permission_code = 'system';
    SELECT id INTO v_tenant_id FROM sys_permission WHERE permission_code = 'tenant';
    SELECT id INTO v_user_id FROM sys_permission WHERE permission_code = 'user';
    SELECT id INTO v_device_id FROM sys_permission WHERE permission_code = 'device';
    SELECT id INTO v_data_id FROM sys_permission WHERE permission_code = 'data';
    SELECT id INTO v_monitor_id FROM sys_permission WHERE permission_code = 'monitor';

    -- 系统管理权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_system_id, 'system:config', '系统配置', 'MENU', '/system/config'),
    (v_system_id, 'system:monitor', '系统监控', 'MENU', '/system/monitor');

    -- 租户管理权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_tenant_id, 'tenant:view', '查看租户', 'MENU', '/tenant/list'),
    (v_tenant_id, 'tenant:create', '创建租户', 'BUTTON', 'btn:tenant:create'),
    (v_tenant_id, 'tenant:update', '编辑租户', 'BUTTON', 'btn:tenant:update'),
    (v_tenant_id, 'tenant:delete', '删除租户', 'BUTTON', 'btn:tenant:delete');

    -- 用户管理权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_user_id, 'user:view', '查看用户', 'MENU', '/user/list'),
    (v_user_id, 'user:create', '创建用户', 'BUTTON', 'btn:user:create'),
    (v_user_id, 'user:update', '编辑用户', 'BUTTON', 'btn:user:update'),
    (v_user_id, 'user:delete', '删除用户', 'BUTTON', 'btn:user:delete');

    -- 设备管理权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_device_id, 'device:view', '查看设备', 'MENU', '/device/list'),
    (v_device_id, 'device:create', '创建设备', 'BUTTON', 'btn:device:create'),
    (v_device_id, 'device:update', '编辑设备', 'BUTTON', 'btn:device:update'),
    (v_device_id, 'device:delete', '删除设备', 'BUTTON', 'btn:device:delete');

    -- 数据管理权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_data_id, 'data:view', '查看数据', 'MENU', '/data/list'),
    (v_data_id, 'data:export', '导出数据', 'BUTTON', 'btn:data:export'),
    (v_data_id, 'data:replay', '数据重放', 'BUTTON', 'btn:data:replay');

    -- 监控中心权限
    INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path) VALUES
    (v_monitor_id, 'monitor:device', '设备监控', 'MENU', '/monitor/device'),
    (v_monitor_id, 'monitor:trajectory', '轨迹监控', 'MENU', '/monitor/trajectory');
END $$;

-- ========================================
-- 8. 分配角色权限
-- ========================================

DO $$
DECLARE
    v_admin_role_id BIGINT;
    v_tenant_admin_role_id BIGINT;
    v_tenant_user_role_id BIGINT;
BEGIN
    SELECT id INTO v_admin_role_id FROM sys_role WHERE role_code = 'ADMIN';
    SELECT id INTO v_tenant_admin_role_id FROM sys_role WHERE role_code = 'TENANT_ADMIN';
    SELECT id INTO v_tenant_user_role_id FROM sys_role WHERE role_code = 'TENANT_USER';

    -- 平台管理员拥有所有权限
    INSERT INTO sys_role_permission (role_id, permission_id)
    SELECT v_admin_role_id, id FROM sys_permission WHERE delete_flag = '0';

    -- 租户管理员权限（不含租户管理和系统管理）
    INSERT INTO sys_role_permission (role_id, permission_id)
    SELECT v_tenant_admin_role_id, id
    FROM sys_permission
    WHERE delete_flag = '0'
      AND permission_code IN (
        'user', 'user:view', 'user:create', 'user:update', 'user:delete',
        'device', 'device:view', 'device:create', 'device:update', 'device:delete',
        'data', 'data:view', 'data:export', 'data:replay',
        'monitor', 'monitor:device', 'monitor:trajectory'
      );

    -- 租户普通用户权限（仅查看）
    INSERT INTO sys_role_permission (role_id, permission_id)
    SELECT v_tenant_user_role_id, id
    FROM sys_permission
    WHERE delete_flag = '0'
      AND permission_code IN (
        'device', 'device:view',
        'data', 'data:view',
        'monitor', 'monitor:device', 'monitor:trajectory'
      );
END $$;

-- ========================================
-- 9. 为现有用户分配角色（迁移旧数据）
-- ========================================

-- 将 sys_user.role 字段迁移到 sys_user_role 表
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = u.role
WHERE u.delete_flag = '0';

-- 注意：保留 sys_user.role 字段作为兼容，后续版本可删除
