#!/bin/bash

# ========================================
# Open-IoT 部署脚本
# 用法: ./scripts/deploy.sh [选项]
#   -e, --env         环境类型 (dev|prod)，默认 dev
#   -s, --services    指定服务（逗号分隔）
#     --infra         仅启动基础设施
#     --observability 仅启动可观测性服务
#   --stop           停止服务
#   --logs           查看日志
#   -h, --help       显示帮助
# ========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 默认参数
ENV="dev"
SERVICES=""
INFRA_ONLY=false
OBSERVABILITY_ONLY=false
STOP_MODE=false
SHOW_LOGS=false

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENV="$2"
            shift 2
            ;;
        -s|--services)
            SERVICES="$2"
            shift 2
            ;;
        --infra)
            INFRA_ONLY=true
            shift
            ;;
        --observability)
            OBSERVABILITY_ONLY=true
            shift
            ;;
        --stop)
            STOP_MODE=true
            shift
            ;;
        --logs)
            SHOW_LOGS=true
            shift
            ;;
        -h|--help)
            echo "用法: ./scripts/deploy.sh [选项]"
            echo ""
            echo "选项:"
            echo "  -e, --env <env>     环境类型 (dev|prod)，默认 dev"
            echo "  -s, --services      指定服务（逗号分隔）"
            echo "  --infra             仅启动基础设施"
            echo "  --observability     仅启动可观测性服务"
            echo "  --stop              停止服务"
            echo "  --logs              查看日志"
            echo "  -h, --help          显示帮助"
            echo ""
            echo "示例:"
            echo "  ./scripts/deploy.sh                    # 启动所有服务（开发环境）"
            echo "  ./scripts/deploy.sh -e prod            # 启动所有服务（生产环境）"
            echo "  ./scripts/deploy.sh --infra            # 仅启动基础设施"
            echo "  ./scripts/deploy.sh --observability    # 仅启动可观测性服务"
            echo "  ./scripts/deploy.sh --stop             # 停止所有服务"
            echo "  ./scripts/deploy.sh --logs             # 查看日志"
            exit 0
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            exit 1
            ;;
    esac
done

# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_DIR="$PROJECT_ROOT/infrastructure/docker"

# Docker Compose 文件
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.yml"
if [ -f "$COMPOSE_DIR/docker-compose.$ENV.yml" ]; then
    COMPOSE_FILE="-f $COMPOSE_FILE -f $COMPOSE_DIR/docker-compose.$ENV.yml"
fi

# 添加可观测性配置文件
OBSERVABILITY_FILE="$COMPOSE_DIR/docker-compose.observability.yml"
if [ -f "$OBSERVABILITY_FILE" ] && [ "$OBSERVABILITY_ONLY" = true ]; then
    if [ -n "$COMPOSE_FILE" ]; then
        COMPOSE_FILE="$COMPOSE_FILE -f $OBSERVABILITY_FILE"
    else
        COMPOSE_FILE="-f $OBSERVABILITY_FILE"
    fi
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Open-IoT 部署脚本${NC}"
echo -e "${BLUE}  环境: ${ENV}${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

cd "$COMPOSE_DIR"

# 停止服务
if [ "$STOP_MODE" = true ]; then
    echo -e "${YELLOW}停止服务...${NC}"
    docker-compose $COMPOSE_FILE down
    echo -e "${GREEN}✓ 服务已停止${NC}"
    exit 0
fi

# 查看日志
if [ "$SHOW_LOGS" = true ]; then
    echo -e "${YELLOW}查看日志（Ctrl+C 退出）...${NC}"
    docker-compose $COMPOSE_FILE logs -f
    exit 0
fi

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 启动基础设施
echo -e "${YELLOW}启动基础设施...${NC}"

if [ "$OBSERVABILITY_ONLY" = true ]; then
    docker-compose $COMPOSE_FILE up -d prometheus loki tempo grafana alertmanager
    echo -e "${GREEN}✓ 可观测性服务已启动${NC}"
    echo ""
    echo -e "${BLUE}服务地址:${NC}"
    echo -e "  Grafana:     ${GREEN}http://localhost:3000${NC} (admin/admin)"
    echo -e "  Prometheus:  ${GREEN}http://localhost:9090${NC}"
    echo -e "  Loki:        ${GREEN}http://localhost:3100${NC}"
    echo -e "  Tempo:       ${GREEN}http://localhost:3200${NC}"
    echo -e "  Alertmanager:${GREEN}http://localhost:9093${NC}"
    exit 0
fi

if [ "$INFRA_ONLY" = true ]; then
    docker-compose $COMPOSE_FILE up -d nacos postgres redis mongodb kafka emqx
    echo -e "${GREEN}✓ 基础设施已启动${NC}"
    echo ""
    echo -e "${BLUE}服务地址:${NC}"
    echo -e "  Nacos:      ${GREEN}http://localhost:8848/nacos${NC} (nacos/nacos)"
    echo -e "  PostgreSQL: ${GREEN}localhost:5432${NC} (openiot/openiot123)"
    echo -e "  Redis:      ${GREEN}localhost:6379${NC}"
    echo -e "  MongoDB:    ${GREEN}localhost:27017${NC}"
    echo -e "  Kafka:      ${GREEN}localhost:9092${NC}"
    echo -e "  EMQX:       ${GREEN}http://localhost:18083${NC} (admin/public)"
    exit 0
fi

# 启动所有服务
echo -e "${YELLOW}启动所有服务...${NC}"

if [ -n "$SERVICES" ]; then
    # 启动指定服务
    IFS=',' read -ra SERVICE_ARRAY <<< "$SERVICES"
    for service in "${SERVICE_ARRAY[@]}"; do
        docker-compose $COMPOSE_FILE up -d "$service"
    done
else
    # 启动所有服务
    docker-compose $COMPOSE_FILE up -d
fi

# 等待服务就绪
echo -e "${YELLOW}等待服务就绪...${NC}"
sleep 10

# 检查服务状态
echo -e "${YELLOW}服务状态:${NC}"
docker-compose $COMPOSE_FILE ps

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}服务地址:${NC}"
echo -e "  Nacos:      ${GREEN}http://localhost:8848/nacos${NC}"
echo -e "  Gateway:    ${GREEN}http://localhost:8080${NC}"
echo -e "  前端:       ${GREEN}http://localhost:5173${NC}"
echo ""
echo -e "${BLUE}常用命令:${NC}"
echo -e "  查看日志:   ${YELLOW}./scripts/deploy.sh --logs${NC}"
echo -e "  停止服务:   ${YELLOW}./scripts/deploy.sh --stop${NC}"
