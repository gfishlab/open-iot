-- ========================================
-- Open-IoT 初始数据脚本
-- Version: 1.0.1
-- Description: 初始化平台管理员数据
-- ========================================

-- 插入平台管理员（密码为 admin123，使用 BCrypt 加密）
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag)
VALUES (
    NULL,
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
    '平台管理员',
    'ADMIN',
    '1',
    '0'
);

-- 插入默认租户
INSERT INTO tenant (tenant_code, tenant_name, contact_email, status, delete_flag)
VALUES (
    'default',
    '默认租户',
    'admin@default.com',
    '1',
    '0'
);

-- 为默认租户创建管理员
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag)
SELECT
    t.id,
    'tenant_admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
    '租户管理员',
    'TENANT_ADMIN',
    '1',
    '0'
FROM tenant t WHERE t.tenant_code = 'default';
