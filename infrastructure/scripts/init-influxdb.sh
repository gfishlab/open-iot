#!/bin/bash
# InfluxDB 2.x 初始化脚本
# 用于手动创建 InfluxDB bucket 和 retention policy

set -e

INFLUXDB_URL="http://localhost:8086"
ADMIN_TOKEN=""  # 从 InfluxDB UI 获取后填入
ORG="openiot"
BUCKET="device-data"
RETENTION_DAYS=90

echo "==================================="
echo "InfluxDB 2.x 初始化脚本"
echo "==================================="

# 检查 influx CLI 是否安装
if ! command -v influx &> /dev/null; then
    echo "错误: influx CLI 未安装"
    echo "请从 https://docs.influxdata.com/influxdb/v2/tools/influx-cli/ 下载并安装"
    exit 1
fi

# 检查 Admin Token 是否配置
if [ -z "$ADMIN_TOKEN" ]; then
    echo "错误: ADMIN_TOKEN 未设置"
    echo "请访问 http://localhost:8086 获取 API Token，然后设置环境变量:"
    echo "  export ADMIN_TOKEN=your-token-here"
    exit 1
fi

# 配置 influx CLI
influx config create \
  -n openiot \
  -u "$INFLUXDB_URL" \
  -t "$ADMIN_TOKEN" \
  -o "$ORG"

echo "✓ InfluxDB CLI 配置完成"

# 创建 bucket（90 天保留策略）
influx bucket create \
  -n "$BUCKET" \
  -o "$ORG" \
  -r "${RETENTION_DAYS}d" \
  --skip-verify

echo "✓ Bucket '$BUCKET' 创建完成（保留策略: ${RETENTION_DAYS}天）"

# 创建只读 Token（可选）
# influx auth create --org openiot --read-bucket $(influx bucket list --name device-data --json | jq -r '.[0].id')

echo "==================================="
echo "InfluxDB 初始化完成！"
echo "==================================="
echo ""
echo "Bucket 信息:"
echo "  名称: $BUCKET"
echo "  组织: $ORG"
echo "  保留: ${RETENTION_DAYS} 天"
echo ""
echo "下一步:"
echo "  1. 在 application.yml 中配置 InfluxDB 连接:"
echo "     influxdb:"
echo "       url: $INFLUXDB_URL"
echo "       token: \$INFLUXDB_TOKEN"
echo "       org: $ORG"
echo "       bucket: $BUCKET"
echo "  2. 设置环境变量:"
echo "     export INFLUXDB_TOKEN=\$ADMIN_TOKEN"
