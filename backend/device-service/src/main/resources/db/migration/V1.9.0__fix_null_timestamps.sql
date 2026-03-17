-- 修复已有数据的 create_time、update_time 为 null 的问题
UPDATE product SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL;
UPDATE product SET update_time = CURRENT_TIMESTAMP WHERE update_time IS NULL;
UPDATE device SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL;
UPDATE device SET update_time = CURRENT_TIMESTAMP WHERE update_time IS NULL;
UPDATE alert_record SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL;
UPDATE alert_record SET update_time = CURRENT_TIMESTAMP WHERE update_time IS NULL;
