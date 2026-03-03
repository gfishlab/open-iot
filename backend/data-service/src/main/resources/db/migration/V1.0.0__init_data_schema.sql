-- ========================================
-- 数据服务数据库初始化脚本
-- Version: 1.0.0
-- Description: 创建设备轨迹、事件相关表
-- ========================================

-- 创建设备轨迹表
CREATE TABLE IF NOT EXISTS device_trajectory (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    speed DECIMAL(5, 2),
    heading DECIMAL(5, 2),
    altitude DECIMAL(8, 2),
    accuracy DECIMAL(5, 2),
    event_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE device_trajectory IS '设备轨迹表';
COMMENT ON COLUMN device_trajectory.id IS '主键';
COMMENT ON COLUMN device_trajectory.tenant_id IS '租户ID';
COMMENT ON COLUMN device_trajectory.device_id IS '设备ID';
COMMENT ON COLUMN device_trajectory.latitude IS '纬度';
COMMENT ON COLUMN device_trajectory.longitude IS '经度';
COMMENT ON COLUMN device_trajectory.speed IS '速度(km/h)';
COMMENT ON COLUMN device_trajectory.heading IS '航向角(度)';
COMMENT ON COLUMN device_trajectory.altitude IS '海拔高度(m)';
COMMENT ON COLUMN device_trajectory.accuracy IS '精度(m)';
COMMENT ON COLUMN device_trajectory.event_time IS '事件时间';
COMMENT ON COLUMN device_trajectory.create_time IS '创建时间';

-- ========================================
-- 创建索引
-- ========================================

-- 轨迹表索引
CREATE INDEX IF NOT EXISTS idx_trajectory_tenant_device_time
    ON device_trajectory(tenant_id, device_id, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_trajectory_time ON device_trajectory(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_trajectory_device ON device_trajectory(device_id, event_time DESC);

-- ========================================
-- 分区表配置（可选，按时间分区）
-- ========================================

-- 注意：
-- 1. 设备轨迹数据量大，建议按月或按季度分区
-- 2. 分区策略：按 event_time 范围分区
-- 3. 示例（按月分区）：
--    CREATE TABLE device_trajectory_202603 PARTITION OF device_trajectory
--    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- ========================================
-- 说明
-- ========================================

-- 注意：
-- 1. tenant_id 关联 openiot_tenant.tenant 表
-- 2. device_id 关联 openiot_device.device 表
-- 3. 跨数据库关联需要通过应用层实现
-- 4. 原始事件数据存储在 MongoDB，PostgreSQL 只存储结构化轨迹数据
