-- ========================================
-- Open-IoT 平台核心功能权限
-- Version: 1.2.1
-- Description: 添加 IoT 平台核心功能相关权限
-- ========================================

-- ========================================
-- 1. 产品管理权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'product', '产品管理', 'MODULE', '/product', 10, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'product:view', '查看产品', 'BUTTON', '/api/products/*', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'product:create', '创建产品', 'BUTTON', '/api/products', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'product:update', '更新产品', 'BUTTON', '/api/products/*', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'product:delete', '删除产品', 'BUTTON', '/api/products/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'thing_model:view', '查看物模型', 'BUTTON', '/api/products/*/thing-model', 5, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'product'), 'thing_model:edit', '编辑物模型', 'BUTTON', '/api/products/*/thing-model', 6, '1');

-- ========================================
-- 2. 解析规则权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'parse_rule', '解析规则', 'MODULE', '/rule/parse', 20, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'parse_rule'), 'parse_rule:view', '查看解析规则', 'BUTTON', '/api/parse-rules/*', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'parse_rule'), 'parse_rule:create', '创建解析规则', 'BUTTON', '/api/parse-rules', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'parse_rule'), 'parse_rule:update', '更新解析规则', 'BUTTON', '/api/parse-rules/*', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'parse_rule'), 'parse_rule:delete', '删除解析规则', 'BUTTON', '/api/parse-rules/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'parse_rule'), 'parse_rule:test', '测试解析规则', 'BUTTON', '/api/parse-rules/*/test', 5, '1');

-- ========================================
-- 3. 映射规则权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'mapping_rule', '映射规则', 'MODULE', '/rule/mapping', 30, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'mapping_rule'), 'mapping_rule:view', '查看映射规则', 'BUTTON', '/api/mapping-rules/*', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'mapping_rule'), 'mapping_rule:create', '创建映射规则', 'BUTTON', '/api/mapping-rules', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'mapping_rule'), 'mapping_rule:update', '更新映射规则', 'BUTTON', '/api/mapping-rules/*', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'mapping_rule'), 'mapping_rule:delete', '删除映射规则', 'BUTTON', '/api/mapping-rules/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'mapping_rule'), 'mapping_rule:test', '测试映射规则', 'BUTTON', '/api/mapping-rules/*/test', 5, '1');

-- ========================================
-- 4. 转发规则权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'forward_rule', '转发规则', 'MODULE', '/rule/forward', 40, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'forward_rule'), 'forward_rule:view', '查看转发规则', 'BUTTON', '/api/forward-rules/*', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'forward_rule'), 'forward_rule:create', '创建转发规则', 'BUTTON', '/api/forward-rules', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'forward_rule'), 'forward_rule:update', '更新转发规则', 'BUTTON', '/api/forward-rules/*', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'forward_rule'), 'forward_rule:delete', '删除转发规则', 'BUTTON', '/api/forward-rules/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'forward_rule'), 'forward_rule:test', '测试转发规则', 'BUTTON', '/api/forward-rules/*/test', 5, '1');

-- ========================================
-- 5. 告警规则权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
(0, 'alarm_rule', '告警规则', 'MODULE', '/alarm/rule', 50, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_rule:view', '查看告警规则', 'BUTTON', '/api/alarm-rules/*', 1, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_rule:create', '创建告警规则', 'BUTTON', '/api/alarm-rules', 2, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_rule:update', '更新告警规则', 'BUTTON', '/api/alarm-rules/*', 3, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_rule:delete', '删除告警规则', 'BUTTON', '/api/alarm-rules/*', 4, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_record:view', '查看告警记录', 'BUTTON', '/api/alarm-records', 5, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'alarm_rule'), 'alarm_record:acknowledge', '确认告警', 'BUTTON', '/api/alarm-records/*/acknowledge', 6, '1');

-- ========================================
-- 6. 设备服务调用权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device_service:invoke', '调用设备服务', 'BUTTON', '/api/devices/*/services/*', 10, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device_service:view', '查看服务调用记录', 'BUTTON', '/api/service-invocations/*', 11, '1');

-- ========================================
-- 7. 历史数据查询权限
-- ========================================
INSERT INTO sys_permission (parent_id, permission_code, permission_name, resource_type, resource_path, sort_order, status) VALUES
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device_property:history', '查询属性历史', 'BUTTON', '/api/devices/*/properties/history', 12, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device_event:history', '查询事件历史', 'BUTTON', '/api/devices/*/events/history', 13, '1'),
((SELECT id FROM sys_permission WHERE permission_code = 'device'), 'device_data:export', '导出设备数据', 'BUTTON', '/api/devices/*/data/export', 14, '1');

-- ========================================
-- 8. 为 TENANT_ADMIN 角色分配权限
-- ========================================
-- 产品管理权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'TENANT_ADMIN'),
    id
FROM sys_permission
WHERE permission_code IN (
    'product', 'product:view', 'product:create', 'product:update', 'product:delete',
    'thing_model:view', 'thing_model:edit',
    'parse_rule', 'parse_rule:view', 'parse_rule:create', 'parse_rule:update', 'parse_rule:delete', 'parse_rule:test',
    'mapping_rule', 'mapping_rule:view', 'mapping_rule:create', 'mapping_rule:update', 'mapping_rule:delete', 'mapping_rule:test',
    'forward_rule', 'forward_rule:view', 'forward_rule:create', 'forward_rule:update', 'forward_rule:delete', 'forward_rule:test',
    'alarm_rule', 'alarm_rule:view', 'alarm_rule:create', 'alarm_rule:update', 'alarm_rule:delete',
    'alarm_record:view', 'alarm_record:acknowledge',
    'device_service:invoke', 'device_service:view',
    'device_property:history', 'device_event:history', 'device_data:export'
);

-- ========================================
-- 9. 为 TENANT_USER 角色分配查看权限
-- ========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'TENANT_USER'),
    id
FROM sys_permission
WHERE permission_code IN (
    'product', 'product:view',
    'thing_model:view',
    'parse_rule', 'parse_rule:view', 'parse_rule:test',
    'mapping_rule', 'mapping_rule:view', 'mapping_rule:test',
    'forward_rule', 'forward_rule:view',
    'alarm_rule', 'alarm_rule:view', 'alarm_record:view',
    'device_service:view',
    'device_property:history', 'device_event:history', 'device_data:export'
);
