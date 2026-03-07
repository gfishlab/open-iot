-- PostgreSQL 初始化脚本
-- 创建 IoT 平台所需的数据库

-- 创建租户服务数据库
CREATE DATABASE openiot_tenant;
COMMENT ON DATABASE openiot_tenant IS '租户服务数据库 - 用户、租户、权限';

-- 创建设备服务数据库
CREATE DATABASE openiot_device;
COMMENT ON DATABASE openiot_device IS '设备服务数据库 - 产品、设备、属性、事件';

-- 创建数据服务数据库
CREATE DATABASE openiot_data;
COMMENT ON DATABASE openiot_data IS '数据服务数据库 - 设备轨迹、历史数据';

-- 创建连接服务数据库
CREATE DATABASE openiot_connect;
COMMENT ON DATABASE openiot_connect IS '连接服务数据库 - 设备连接会话';
