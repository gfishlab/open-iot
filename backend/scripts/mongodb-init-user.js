-- MongoDB 用户初始化脚本
-- 使用方法：在 MongoDB Shell 或数据库管理工具中执行

-- 切换到 admin 数据库
use admin;

-- 创建 openiot 用户（如果不存在）
db.createUser({
  user: "openiot",
  pwd: "openiot123",
  roles: [
    { role: "readWrite", db: "openiot" },
    { role: "dbAdmin", db: "openiot" },
    { role: "userAdmin", db: "openiot" }
  ]
});

-- 切换到 openiot 数据库
use openiot;

-- 创建测试集合（可选）
db.createCollection("device_raw_event");

-- 验证用户权限
db.device_raw_event.find().limit(1);

-- 查看用户信息
use admin;
db.getUser("openiot");
