#!/bin/bash

################################################################################
# Class Booking System - Development Workflow Script
################################################################################
# 这个脚本自动化整个开发流程：
# 1. 本地运行测试
# 2. 提交并推送到 GitHub
# 3. 监控 GitHub Actions Pipeline
# 4. 如果成功，拉取最新镜像并启动 Docker Compose
################################################################################

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# GitHub 仓库信息
GITHUB_REPO="yinghuiwang00/ClaudeCode_ClassSystem"
DOCKER_IMAGE="yinghuiwang00/class-system:latest"

################################################################################
# 打印带颜色的消息
################################################################################
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

################################################################################
# 步骤 1: 检查是否有未提交的更改
################################################################################
check_git_status() {
    print_info "检查 Git 状态..."
    if git diff --quiet && git diff --cached --quiet; then
        print_warning "没有需要提交的更改"
        read -p "是否继续？(y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "退出..."
            exit 0
        fi
    else
        print_info "发现未提交的更改"
        git status --short
    fi
}

################################################################################
# 步骤 2: 运行本地测试
################################################################################
run_local_tests() {
    print_info "========================================"
    print_info "步骤 1: 运行本地测试"
    print_info "========================================"

    print_info "清理并构建项目..."
    mvn clean compile -DskipTests -B

    print_info "运行所有测试..."
    if mvn test -B; then
        print_success "所有测试通过！"
        return 0
    else
        print_error "测试失败！请修复后重试"
        print_info "你可以运行 'mvn test' 查看详细错误"
        exit 1
    fi
}

################################################################################
# 步骤 3: 提交并推送到 GitHub
################################################################################
commit_and_push() {
    print_info "========================================"
    print_info "步骤 2: 提交并推送到 GitHub"
    print_info "========================================"

    # 提示输入提交信息
    print_info "请输入提交信息（留空使用默认）："
    read -p "> " COMMIT_MSG

    if [ -z "$COMMIT_MSG" ]; then
        COMMIT_MSG="feat: update codebase

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
    fi

    # 添加所有更改
    print_info "添加所有更改..."
    git add -A

    # 提交
    print_info "提交更改..."
    git commit -m "$COMMIT_MSG"

    # 推送
    print_info "推送到 GitHub..."
    git push origin $(git branch --show-current)

    print_success "代码已推送到 GitHub"
}

################################################################################
# 步骤 4: 监控 GitHub Actions Pipeline
################################################################################
monitor_pipeline() {
    print_info "========================================"
    print_info "步骤 3: 监控 GitHub Actions Pipeline"
    print_info "========================================"

    print_info "等待 Pipeline 启动..."
    sleep 5

    # 获取最新的 workflow run
    print_info "获取最新的 CI/CD run..."
    RUN_INFO=$(curl -s "https://api.github.com/repos/$GITHUB_REPO/actions/runs?per_page=1" \
        -H "Accept: application/vnd.github.v3+json")

    RUN_ID=$(echo "$RUN_INFO" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('workflow_runs', [{}])[0].get('id', ''))")
    RUN_NUMBER=$(echo "$RUN_INFO" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('workflow_runs', [{}])[0].get('run_number', ''))")
    RUN_STATUS=$(echo "$RUN_INFO" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('workflow_runs', [{}])[0].get('status', ''))")
    RUN_CONCLUSION=$(echo "$RUN_INFO" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('workflow_runs', [{}])[0].get('conclusion', 'N/A'))")

    if [ -z "$RUN_ID" ]; then
        print_error "无法获取 CI/CD run 信息"
        exit 1
    fi

    print_info "Pipeline #$RUN_NUMBER (ID: $RUN_ID)"
    print_info "URL: https://github.com/$GITHUB_REPO/actions/runs/$RUN_ID"

    # 轮询状态
    MAX_ATTEMPTS=60  # 最多等待 10 分钟
    ATTEMPT=0

    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        # 获取当前状态
        RUN_INFO=$(curl -s "https://api.github.com/repos/$GITHUB_REPO/actions/runs/$RUN_ID" \
            -H "Accept: application/vnd.github.v3+json")
        RUN_STATUS=$(echo "$RUN_INFO" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', ''))")
        RUN_CONCLUSION=$(echo "$RUN_INFO" | python3 -c "import sys, json; print(json.load(sys.stdin).get('conclusion', 'N/A'))")

        case $RUN_STATUS in
            "queued")
                print_info "[$(date +%H:%M:%S)] Pipeline 状态: 排队中..."
                ;;
            "in_progress")
                print_info "[$(date +%H:%M:%S)] Pipeline 状态: 运行中..."
                ;;
            "completed")
                if [ "$RUN_CONCLUSION" = "success" ]; then
                    print_success "[$(date +%H:%M:%S)] Pipeline 成功！"
                    return 0
                else
                    print_error "[$(date +%H:%M:%S)] Pipeline 失败 ($RUN_CONCLUSION)"
                    print_info "请查看: https://github.com/$GITHUB_REPO/actions/runs/$RUN_ID"
                    return 1
                fi
                ;;
            *)
                print_error "未知状态: $RUN_STATUS"
                return 1
                ;;
        esac

        ATTEMPT=$((ATTEMPT + 1))
        sleep 10
    done

    print_error "Pipeline 超时"
    return 1
}

################################################################################
# 步骤 5: 拉取最新镜像并启动 Docker Compose
################################################################################
deploy_docker() {
    print_info "========================================"
    print_info "步骤 4: 部署 Docker 服务"
    print_info "========================================"

    # 拉取最新镜像
    print_info "拉取最新镜像: $DOCKER_IMAGE"
    docker pull $DOCKER_IMAGE

    # 停止并删除旧容器
    print_info "停止旧容器..."
    docker-compose down -v 2>/dev/null || true

    # 启动新容器
    print_info "启动新容器..."
    docker-compose up -d

    # 等待服务启动
    print_info "等待服务启动..."
    sleep 15

    # 检查状态
    print_info "检查服务状态..."
    docker-compose ps

    print_success "Docker 服务已启动！"
    print_info "访问应用: http://localhost:8080"
    print_info "Swagger UI: http://localhost:8080/swagger-ui.html"
}

################################################################################
# 主流程
################################################################################
main() {
    print_info "========================================"
    print_info "Class Booking System 开发工作流"
    print_info "========================================"
    echo

    # 步骤 1: 本地测试
    run_local_tests
    echo

    # 步骤 2: 提交并推送
    check_git_status
    commit_and_push
    echo

    # 步骤 3: 监控 Pipeline
    if monitor_pipeline; then
        echo
        # 步骤 4: 部署 Docker
        deploy_docker
        print_success "========================================"
        print_success "整个流程完成！"
        print_success "========================================"
    else
        print_error "========================================"
        print_error "Pipeline 失败，请修复错误后重试"
        print_error "========================================"
        exit 1
    fi
}

# 运行主流程
main "$@"
