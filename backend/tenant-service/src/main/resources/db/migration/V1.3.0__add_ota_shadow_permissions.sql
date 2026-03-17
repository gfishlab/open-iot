-- ========================================
-- Open-IoT OTA & Device Shadow Permissions
-- Version: 1.3.0
-- Description: Add OTA upgrade and device shadow permissions
-- ========================================

-- ========================================
-- 1. OTA Upgrade Permissions
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'ota', 'OTA升级管理', 'MODULE', '/ota', 60, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:firmware:view', '查看固件版本', 'BUTTON', '/api/v1/ota/firmware', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:firmware:upload', '上传固件', 'BUTTON', '/api/v1/ota/firmware', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:firmware:download', '下载固件', 'BUTTON', '/api/v1/ota/firmware/*/download', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:firmware:delete', '删除固件', 'BUTTON', '/api/v1/ota/firmware/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:task:view', '查看升级任务', 'BUTTON', '/api/v1/ota/tasks', 5, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:task:create', '创建升级任务', 'BUTTON', '/api/v1/ota/tasks', 6, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'ota'), 'ota:task:cancel', '取消升级任务', 'BUTTON', '/api/v1/ota/tasks/*/cancel', 7, '1');

-- ========================================
-- 2. Device Shadow Permissions
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device:shadow:view', '查看设备影子', 'BUTTON', '/api/v1/devices/*/shadow', 20, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device:shadow:desired', '设置期望属性', 'BUTTON', '/api/v1/devices/*/shadow/desired', 21, '1');

-- ========================================
-- 3. Assign OTA permissions to TENANT_ADMIN
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'TENANT_ADMIN'),
    id
FROM sys_permission
WHERE permission_code IN (
    'ota', 'ota:firmware:view', 'ota:firmware:upload', 'ota:firmware:download', 'ota:firmware:delete',
    'ota:task:view', 'ota:task:create', 'ota:task:cancel',
    'device:shadow:view', 'device:shadow:desired'
);

-- ========================================
-- 4. Assign view-only permissions to TENANT_USER
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'TENANT_USER'),
    id
FROM sys_permission
WHERE permission_code IN (
    'ota', 'ota:firmware:view', 'ota:task:view',
    'device:shadow:view'
);
