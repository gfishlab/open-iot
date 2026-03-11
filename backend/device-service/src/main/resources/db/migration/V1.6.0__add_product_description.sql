-- V1.6.0 为产品表添加描述字段
-- Author: Claude
-- Date: 2026-03-11

-- 添加 description 列
ALTER TABLE product ADD COLUMN IF NOT EXISTS description TEXT;

-- 添加列注释
COMMENT ON COLUMN product.description IS '产品描述';
