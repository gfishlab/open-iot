-- ========================================
-- Open-IoT 独立数据库初始化脚本
-- Version: 1.0.0
-- Description: 为每个微服务创建独立数据库
-- ========================================

-- ========================================
-- 1. 创建数据库
-- ========================================

-- 租户服务数据库
CREATE DATABASE openiot_tenant
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE openiot_tenant IS '租户服务数据库 - 管理租户、用户、权限';

-- 设备服务数据库
CREATE DATABASE openiot_device
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE openiot_device IS '设备服务数据库 - 管理设备、告警配置';

-- 数据服务数据库
CREATE DATABASE openiot_data
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE openiot_data IS '数据服务数据库 - 管理设备轨迹、原始事件';

-- 连接服务数据库
CREATE DATABASE openiot_connect
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE openiot_connect IS '连接服务数据库 - 管理设备连接、会话';

-- ========================================
-- 2. 创建用户和权限
-- ========================================

-- 租户服务用户
CREATE USER openiot_tenant WITH PASSWORD 'tenant123';
GRANT ALL PRIVILEGES ON DATABASE openiot_tenant TO openiot_tenant;

-- 设备服务用户
CREATE USER openiot_device WITH PASSWORD 'device123';
GRANT ALL PRIVILEGES ON DATABASE openiot_device TO openiot_device;

-- 数据服务用户
CREATE USER openiot_data WITH PASSWORD 'data123';
GRANT ALL PRIVILEGES ON DATABASE openiot_data TO openiot_data;

-- 连接服务用户
CREATE USER openiot_connect WITH PASSWORD 'connect123';
GRANT ALL PRIVILEGES ON DATABASE openiot_connect TO openiot_connect;

-- ========================================
-- 3. 授权 Schema 权限
-- ========================================

-- 需要在每个数据库中执行以下授权语句
-- \connect openiot_tenant
-- GRANT ALL ON SCHEMA public TO openiot_tenant;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot_tenant;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot_tenant;

-- ========================================
-- 说明
-- ========================================

-- 数据库架构：
-- openiot_tenant (tenant-service)
--   ├── tenant          租户表
--   ├── sys_user        系统用户表
--   └── RBAC 相关表     权限、角色、菜单等
--
-- openiot_device (device-service)
--   ├── device          设备表
--   ├── device_config   设备配置表
--   └── alarm_config    告警配置表
--
-- openiot_data (data-service)
--   ├── device_trajectory  设备轨迹表
--   └── raw_event          原始事件表（MongoDB）
--
-- openiot_connect (connect-service)
--   ├── device_session  设备会话表
--   └── connection_log  连接日志表

-- 执行顺序：
-- 1. 在 PostgreSQL 中以超级用户身份执行此脚本
-- 2. 在各数据库中执行 Schema 授权
-- 3. 各服务启动时，Flyway 自动创建表结构
