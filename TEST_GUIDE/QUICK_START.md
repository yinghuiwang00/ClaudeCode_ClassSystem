# 快速开始测试 (Quick Start)

最简单的测试执行指南。

## 5 分钟快速开始

### 1. 运行所有测试

```bash
mvn clean test jacoco:report
```

### 2. 查看测试结果

测试完成后，查看控制台输出或打开报告：

```bash
# macOS
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

### 3. 运行特定测试

```bash
# 只运行单元测试（跳过集成测试）
mvn test -DskipITs

# 只运行集成测试
mvn verify

# 运行单个测试类
mvn test -Dtest=AuthServiceTest

# 运行 Cucumber 测试
mvn test -Dtest=CucumberTestRunner
```

## 测试类型说明

| 测试类型 | 命令 | 说明 |
|---------|------|------|
| 单元测试 | `mvn test` | 测试单个类/方法 |
| 集成测试 | `mvn verify` | 测试多个组件协作 |
| Cucumber | `mvn test -Dtest=CucumberTestRunner` | 功能/行为测试 |
| 覆盖率报告 | `mvn jacoco:report` | 代码覆盖率分析 |

## 常用命令

```bash
# 清理并运行所有测试
mvn clean test

# 生成覆盖率报告
mvn jacoco:report

# 跳过测试
mvn clean install -DskipTests

# 运行测试并输出详细信息
mvn test -X

# 运行测试并保存日志
mvn test > test-log.txt 2>&1
```

## 故障排除

### 问题：测试失败

```bash
# 查看详细错误信息
mvn test -X

# 重新运行失败的测试
mvn test -Dsurefire.rerunFailingTestsCount=3
```

### 问题：端口被占用

在 `src/test/resources/application-test.yml` 中设置随机端口：

```yaml
server:
  port: 0
```

### 问题：数据库连接失败

确保 H2 数据库配置正确（默认配置应该可以工作）。

---

需要更多帮助？查看 [完整测试执行指南](./TEST_EXECUTION_GUIDE.md)
