# 开发工作流文档

## 概述

本文档描述了 Class Booking System 项目的标准化开发流程，从代码修改到部署的完整步骤。

## 自动化脚本

项目包含一个自动化脚本 `dev-workflow.sh`，它将整个开发流程自动化。

### 使用方法

```bash
# 运行完整的开发工作流
./dev-workflow.sh
```

## 工作流程

### 1. 本地测试

**目的**: 确保代码修改在本地通过所有测试

**命令**:
```bash
mvn test
```

**说明**:
- 运行所有单元测试和集成测试
- 确保测试覆盖率达到要求（80%）
- 修复所有失败的测试后再继续

### 2. 提交并推送到 GitHub

**目的**: 将修改后的代码提交到版本控制系统

**命令**:
```bash
# 查看更改状态
git status

# 添加所有更改
git add -A

# 提交（写清楚提交信息）
git commit -m "描述你的改动"

# 推送到 GitHub
git push origin main
```

**提交信息格式**:
```
feat: 添加新功能
fix: 修复 bug
refactor: 重构代码
docs: 更新文档

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

### 3. 监控 GitHub Actions Pipeline

**目的**: 确保 CI/CD pipeline 成功构建并推送 Docker 镜像

**监控方式**:

**方式 1: 使用自动化脚本**
```bash
./dev-workflow.sh
# 脚本会自动监控 Pipeline 状态
```

**方式 2: 手动查看**
```bash
# 查看最近的 Pipeline run
curl -s "https://api.github.com/repos/yinghuiwang00/ClaudeCode_ClassSystem/actions/runs?per_page=1" \
  -H "Accept: application/vnd.github.v3+json" | python3 -m json.tool
```

**方式 3: 浏览器访问**
https://github.com/yinghuiwang00/ClaudeCode_ClassSystem/actions

**Pipeline 状态说明**:
- `queued`: 排队等待执行
- `in_progress`: 正在运行
- `completed`: 已完成
  - `success`: 成功
  - `failure`: 失败
  - `cancelled`: 取消

**如果 Pipeline 失败**:
1. 点击失败的 run 查看详细日志
2. 找出失败原因
3. 在本地修复问题
4. 重新运行 `./dev-workflow.sh`

### 4. 拉取最新镜像并启动服务

**目的**: 使用 CI/CD 构建的新镜像启动本地服务

**命令**:
```bash
# 拉取最新镜像
docker pull yinghuiwang00/class-system:latest

# 停止并删除旧容器和数据卷
docker-compose down -v

# 启动新容器
docker-compose up -d

# 查看日志
docker-compose logs -f

# 检查状态
docker-compose ps
```

**或者使用自动化脚本**:
```bash
./dev-workflow.sh
# Pipeline 成功后会自动执行部署
```

## 开发流程图

```
┌─────────────────┐
│  修改代码      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  本地测试      │
│  mvn test      │
└────────┬────────┘
         │
         ▼ 成功
    ┌─────────────┐
    │ 提交代码    │
    │ git commit  │
    │ git push    │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │  CI/CD     │
    │  Pipeline   │
    └──────┬──────┘
           │
      ┌────┴────┐
      │ 成功?   │
      └────┬────┘
           │
      ┌────┴────┐
      │否       │是
      ▼         ▼
┌─────────┐  ┌──────────────┐
│修复代码 │  │拉取 Docker 镜像│
└────┬────┘  └──────┬───────┘
     │              │
     │              ▼
     │        ┌─────────────┐
     │        │启动服务     │
     │        │docker-compose│
     │        └─────────────┘
     │              │
     └──────────────┘
                    │
                    ▼
              ┌─────────────┐
              │  测试验证   │
              └─────────────┘
```

## 快捷命令

### 单独执行各个步骤

**1. 仅运行测试**:
```bash
mvn test
```

**2. 仅提交并推送**:
```bash
git add -A
git commit -m "描述改动"
git push origin main
```

**3. 仅监控 Pipeline**:
```bash
# 查看最新 run
curl -s "https://api.github.com/repos/yinghuiwang00/ClaudeCode_ClassSystem/actions/runs?per_page=1" \
  -H "Accept: application/vnd.github.v3+json" | python3 -c "import sys, json; d=json.load(sys.stdin); r=d.get('workflow_runs', [{}])[0]; print(f\"{r.get('run_number')}: {r.get('status')} - {r.get('conclusion', 'running')}\")"
```

**4. 仅部署 Docker**:
```bash
docker-compose pull
docker-compose down -v
docker-compose up -d
docker-compose logs -f
```

### 实用命令

**查看应用日志**:
```bash
docker-compose logs -f class-booking-system
```

**查看所有服务状态**:
```bash
docker-compose ps
```

**重启服务**:
```bash
docker-compose restart
```

**完全清理（删除所有容器、网络、卷）**:
```bash
docker-compose down -v
```

**进入容器**:
```bash
docker exec -it class-booking-system sh
```

**连接到 PostgreSQL**:
```bash
docker exec -it class-postgres psql -U postgres -d bookingdb
```

## 访问地址

运行 `docker-compose up -d` 后：

| 服务 | 地址 | 说明 |
|------|------|------|
| 应用 | http://localhost:8080 | REST API |
| Swagger UI | http://localhost:8080/swagger-ui.html | API 文档 |
| H2 Console | http://localhost:8080/h2-console | H2 数据库（H2 模式） |
| Actuator Health | http://localhost:8080/actuator/health | 健康检查 |
| PostgreSQL | localhost:5432 | PostgreSQL 数据库 |

## 常见问题

### Q: Pipeline 失败怎么办？
A:
1. 查看失败的日志
2. 本地运行 `mvn test` 确认测试通过
3. 修复问题后重新运行 `./dev-workflow.sh`

### Q: Docker 服务启动失败？
A:
1. 查看日志：`docker-compose logs class-booking-system`
2. 确认 PostgreSQL 已启动：`docker-compose ps`
3. 删除数据卷重试：`docker-compose down -v && docker-compose up -d`

### Q: 如何回滚到上一个版本？
A:
```bash
# 回滚代码
git reset --hard HEAD~1
git push -f origin main

# 重新部署
docker-compose pull  # 等待新的 Pipeline 完成
docker-compose down -v
docker-compose up -d
```

### Q: 如何切换数据库模式？
A:

**H2 模式（本地开发）**:
```bash
# 修改 docker-compose.yml
# 取消注释 H2 配置，注释 PostgreSQL 配置
# 移除 depends_on 和 postgres 服务
docker-compose up -d
```

**PostgreSQL 模式（生产）**:
```bash
# 使用当前配置即可
docker-compose up -d
```

## 配置文件说明

| 文件 | 用途 |
|------|------|
| `dev-workflow.sh` | 自动化开发流程脚本 |
| `application.yml` | 默认配置 |
| `application-h2.yml` | H2 数据库配置 |
| `application-prod.yml` | PostgreSQL 生产配置 |
| `application-test.yml` | 测试环境配置 |
| `docker-compose.yml` | Docker Compose 配置 |

## 最佳实践

1. **提交前**: 始终运行 `mvn test` 确保测试通过
2. **提交信息**: 使用清晰、描述性的提交信息
3. **Pipeline 监控**: 推送后及时监控 Pipeline 状态
4. **部署前**: 确认 Pipeline 成功后再拉取镜像
5. **数据清理**: 重大 schema 变更时使用 `docker-compose down -v` 清理旧数据
6. **日志检查**: 部署后查看日志确认服务正常启动

## 相关文档

- [CLAUDE.md](./CLAUDE.md) - 项目架构和实现细节
- [ARCHITECTURE_KNOWLEDGE.md](./Architecture/ARCHITECTURE_KNOWLEDGE.md) - 详细架构知识库
- [ARCHITECTURE.md](./Architecture/ARCHITECTURE.md) - 架构分析和改进建议
