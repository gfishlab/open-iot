-- ========================================
-- Open-IoT 独立数据库初始化脚本
-- Version: 1.0.2
-- Description: 为每个微服务创建独立数据库（幂等版）
-- ========================================

-- ========================================
-- 1. 创建数据库（不存在才创建）
-- ========================================

-- 租户服务数据库
SELECT 'CREATE DATABASE openiot_tenant
    ENCODING = ''UTF8''
    LC_COLLATE = ''en_US.utf8''
    LC_CTYPE = ''en_US.utf8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'openiot_tenant')\gexec

COMMENT ON DATABASE openiot_tenant IS '租户服务数据库';

-- 设备服务数据库
SELECT 'CREATE DATABASE openiot_device
    ENCODING = ''UTF8''
    LC_COLLATE = ''en_US.utf8''
    LC_CTYPE = ''en_US.utf8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'openiot_device')\gexec

COMMENT ON DATABASE openiot_device IS '设备服务数据库';

-- 数据服务数据库
SELECT 'CREATE DATABASE openiot_data
    ENCODING = ''UTF8''
    LC_COLLATE = ''en_US.utf8''
    LC_CTYPE = ''en_US.utf8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'openiot_data')\gexec

COMMENT ON DATABASE openiot_data IS '数据服务数据库';

-- 连接服务数据库
SELECT 'CREATE DATABASE openiot_connect
    ENCODING = ''UTF8''
    LC_COLLATE = ''en_US.utf8''
    LC_CTYPE = ''en_US.utf8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'openiot_connect')\gexec

COMMENT ON DATABASE openiot_connect IS '连接服务数据库';

-- ========================================
-- 2. 创建用户
-- ========================================

CREATE USER openiot WITH PASSWORD 'openiot123';

-- ========================================
-- 3. 授予数据库权限
-- ========================================

GRANT ALL PRIVILEGES ON DATABASE openiot_tenant TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_device TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_data TO openiot;
GRANT ALL PRIVILEGES ON DATABASE openiot_connect TO openiot;

-- ========================================
-- 4. Schema 权限授权（必须执行！）
-- ========================================
-- ⚠️ 重要：以下语句需要复制并在每个数据库中单独执行
-- Flyway 和应用程序需要 Schema 权限才能创建表和读写数据

-- \connect openiot_tenant
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_device
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_data
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- \connect openiot_connect
-- GRANT ALL ON SCHEMA public TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openiot;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openiot;

-- ========================================
-- 执行说明
-- ========================================
-- 此脚本需要手动执行，不会被服务自动触发！
--
-- 执行步骤：
-- 1. 以 postgres 超级用户执行此脚本
--    psql -U postgres -f init-databases.sql
--
-- 2. 执行 Schema 权限授权脚本
--    psql -U postgres -f grant-schema-permissions.sql
--
-- 3. 验证数据库连接
--    psql -U openiot -d openiot_tenant -c "SELECT current_database(), current_user;"
--
-- 4. 启动各服务（Flyway 自动创建表结构）
--    cd backend/tenant-service
--    mvn spring-boot:run
--
-- 注意事项：
-- - 如果用户 openiot 已存在，步骤1会报错，可忽略
-- - 必须执行步骤2，否则 Flyway 无法创建表
-- - Schema 权限 ≠ 数据库权限，需要单独授权
