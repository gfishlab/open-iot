# Open-IoT 开放物联网平台

[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=http://jenkins.example.com/job/open-iot)](https://jenkins.example.com/job/open-iot)
[![Docker Build](https://img.shields.io/badge/docker-ready-blue)](https://hub.docker.com/r/example/open-iot)
[![License](https://img.shields.io/github/license/gxj1134506645/open-iot)](LICENSE)

**Open-IoT** 是一个学习型开源物联网平台，旨在系统化学习 IoT 后端核心能力。

## 🎯 核心能力

- ✅ **微服务治理**: Nacos 注册中心 + 配置中心 + Spring Cloud Gateway 网关
- ✅ **Kafka 消息驱动**: 实时 + 异步双通道数据流
- ✅ **Netty 高并发接入**: TCP 私有协议 + MQTT (EMQX) + HTTP 多协议支持
- ✅ **多租户数据库设计**: 全链路租户隔离

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                            │
│              设备监控 │ 轨迹展示 │ 租户管理                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                 API Gateway (Spring Cloud Gateway)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
    ▼                      ▼                      ▼
┌──────────┐         ┌──────────┐         ┌──────────┐
│ device-  │         │  data-   │         │ tenant-  │
│ service  │         │ service  │         │ service  │
└──────────┘         └──────────┘         └──────────┘

┌─────────────────────────────────────────────────────────────┐
│                        设备接入层                            │
│   EMQX (MQTT)    │    connect-service (Netty TCP)    │ HTTP  │
└─────────────────────────────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │    Kafka    │
                    └──────┬──────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    ▼                      ▼                      ▼
┌──────────┐         ┌──────────┐         ┌──────────┐
│PostgreSQL│         │  Redis   │         │ MongoDB  │
└──────────┘         └──────────┘         └──────────┘
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| Docker | 24+ |
| Docker Compose | 2.20+ |

### 1. 启动基础设施

```bash
cd infrastructure/docker
docker-compose up -d
```

等待所有服务启动（约 2-3 分钟）。

### 2. 构建并启动后端

```bash
cd backend
mvn clean package -DskipTests

# 启动各服务
java -jar gateway-service/target/gateway-service.jar &
java -jar tenant-service/target/tenant-service.jar &
java -jar device-service/target/device-service.jar &
java -jar connect-service/target/connect-service.jar &
java -jar data-service/target/data-service.jar &
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问

- 前端: http://localhost:5173
- Nacos: http://localhost:8848/nacos (nacos/nacos)
- EMQX Dashboard: http://localhost:18083 (admin/admin123)
- Kafka UI: http://localhost:9000

## 📦 项目结构

```
open-iot/
├── backend/                    # 后端服务
│   ├── common/                 # 公共模块
│   │   ├── common-core/        # 核心工具类
│   │   ├── common-redis/       # Redis 配置
│   │   ├── common-kafka/       # Kafka 配置
│   │   ├── common-mongodb/     # MongoDB 配置
│   │   └── common-security/    # 安全认证
│   ├── gateway-service/        # API 网关
│   ├── tenant-service/         # 租户管理
│   ├── device-service/         # 设备管理
│   ├── connect-service/        # 设备接入 (Netty)
│   └── data-service/           # 数据处理
├── frontend/                   # 前端 (Vue 3 + Vite)
├── infrastructure/             # 基础设施
│   ├── docker/                 # Docker 配置
│   ├── emqx/                   # EMQX 配置
│   ├── kafka/                  # Kafka 配置
│   └── sql/                    # 数据库脚本
├── specs/                      # 功能规格文档
├── Jenkinsfile                 # CI/CD 流水线
└── README.md
```

## 🔧 CI/CD

### Jenkins 流水线

项目使用 Jenkins + Docker 实现 CI/CD：

1. **代码提交** → GitHub Webhook 触发 Jenkins
2. **构建** → Maven 构建后端，npm 构建前端
3. **测试** → 运行单元测试
4. **镜像构建** → 根据 Dockerfile 构建镜像
5. **部署** → 推送镜像，docker-compose 部署

### Jenkins 配置

1. 安装插件: Docker Pipeline, Kubernetes (可选)
2. 添加凭证: `docker-registry` (镜像仓库登录)
3. 创建流水线任务，指向 Jenkinsfile

## 📚 文档

- [功能规格](./specs/001-mvp-core/spec.md)
- [实施计划](./specs/001-mvp-core/plan.md)
- [API 契约](./specs/001-mvp-core/contracts/api.yaml)
- [事件契约](./specs/001-mvp-core/contracts/events.yaml)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[MIT License](LICENSE)

---

**Version**: 1.0.0 | **Author**: gxj1134506645
