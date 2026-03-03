-- ========================================
-- 租户服务初始化数据
-- Version: 1.0.1
-- Description: 插入默认租户和管理员用户
-- ========================================

-- 插入平台管理员（密码：admin123，使用 BCrypt 加密）
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag, create_time)
VALUES
    (NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKljKGyC', '系统管理员', 'ADMIN', '1', '0', CURRENT_TIMESTAMP);

-- 插入测试租户
INSERT INTO tenant (tenant_code, tenant_name, contact_email, status, delete_flag, create_time)
VALUES
    ('TEST001', '测试租户', 'test@example.com', '1', '0', CURRENT_TIMESTAMP);

-- 插入租户管理员（关联测试租户）
INSERT INTO sys_user (tenant_id, username, password, real_name, role, status, delete_flag, create_time)
SELECT
    id,
    'tenant_admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKljKGyC',
    '租户管理员',
    'TENANT_ADMIN',
    '1',
    '0',
    CURRENT_TIMESTAMP
FROM tenant
WHERE tenant_code = 'TEST001';
