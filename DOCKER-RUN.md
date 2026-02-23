# Docker 运行指南

本文档介绍如何在本地使用 Docker 运行 Class Booking System 应用程序。

## 前提条件

1. 安装 [Docker](https://docs.docker.com/get-docker/)
2. 安装 [Docker Compose](https://docs.docker.com/compose/install/)
3. 确保 Docker 服务正在运行

## 快速开始

### 1. 配置环境变量

复制环境变量模板文件：

```bash
cp .env.example .env
```

编辑 `.env` 文件，根据需要修改配置，**特别是 JWT_SECRET**。

### 2. 运行应用程序（使用 H2 文件数据库）

H2 文件数据库模式适合本地开发和测试：

```bash
# 创建数据目录
mkdir -p data

# 启动应用
docker-compose up -d

# 查看日志
docker-compose logs -f class-booking-system
```

### 3. 验证运行状态

```bash
# 检查容器状态
docker-compose ps

# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 查看应用日志
docker-compose logs class-booking-system
```

### 4. 访问应用程序

- **API 文档 (Swagger UI)**: http://localhost:8080/swagger-ui.html
- **H2 数据库控制台**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:/app/data/bookingdb`
  - Username: `sa`
  - Password: (留空)

### 5. 停止应用程序

```bash
# 停止并删除容器
docker-compose down

# 停止容器但保留数据卷
docker-compose stop

# 停止并删除容器、网络、数据卷
docker-compose down -v
```

## 高级配置

### 使用 PostgreSQL 数据库

1. 修改 `docker-compose.yml`，取消注释 PostgreSQL 服务部分
2. 修改 `docker-compose.yml` 中的环境变量配置，使用 PostgreSQL 连接
3. 或者通过 `.env` 文件覆盖数据库配置

```bash
# 启动所有服务（应用 + PostgreSQL）
docker-compose up -d

# 查看所有服务状态
docker-compose ps
```

### 数据持久化

- **H2 模式**: 数据存储在 `./data` 目录中
- **PostgreSQL 模式**: 数据存储在 `./postgres-data` 目录中

确保这些目录有适当的写入权限。

### 自定义 JVM 参数

在 `.env` 文件中修改 `JAVA_OPTS`：

```bash
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx512m -Xms256m
```

### 生产环境配置建议

1. **修改 JWT 密钥**: 使用强密码生成器生成新的 JWT_SECRET
2. **使用 PostgreSQL**: 更适合生产环境
3. **禁用 H2 控制台**: 设置 `SPRING_H2_CONSOLE_ENABLED=false`
4. **调整日志级别**: 设置为 `INFO` 或 `WARN`
5. **配置外部数据库**: 使用云数据库服务

## 常用命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 查看日志
docker-compose logs -f [service-name]

# 查看服务状态
docker-compose ps

# 重启服务
docker-compose restart

# 进入容器
docker-compose exec class-booking-system sh

# 构建并启动（如果修改了 Dockerfile）
docker-compose up -d --build

# 查看资源使用情况
docker-compose stats
```

## 故障排除

### 1. 端口冲突

如果 8080 端口已被占用，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "8081:8080"  # 主机端口:容器端口
```

### 2. 权限问题

如果遇到数据目录权限问题：

```bash
# 确保当前用户有读写权限
chmod 755 data
```

### 3. 容器启动失败

查看详细日志：

```bash
docker-compose logs --tail=100 class-booking-system
```

### 4. 健康检查失败

应用可能需要一些时间启动，等待 1-2 分钟后重试。

### 5. 镜像拉取失败

确保 Docker Hub 可以访问，或使用本地构建的镜像：

```bash
# 从 CI/CD 工作流中获取正确的镜像标签
docker pull yinghuiwang00/class-system:latest
```

## 从源码构建并运行

如果你想从源码构建 Docker 镜像而不是使用 CI/CD 构建的镜像：

```bash
# 构建镜像
docker build -t class-booking-system:local .

# 修改 docker-compose.yml 使用本地镜像
# 将 image: yinghuiwang00/class-system:latest
# 改为 image: class-booking-system:local

# 启动服务
docker-compose up -d
```

## 联系与支持

如果在运行过程中遇到问题，请检查：
1. Docker 和 Docker Compose 版本
2. 系统资源（内存、磁盘空间）
3. 防火墙和网络设置

参考项目文档了解更多信息。