# 测试执行指南 (Test Execution Guide)

本指南详细说明如何运行 Class Booking System 项目的所有测试、生成测试报告以及理解测试结果。

## 目录

1. [测试概览](#测试概览)
2. [运行测试](#运行测试)
3. [生成测试报告](#生成测试报告)
4. [测试覆盖率](#测试覆盖率)
5. [Cucumber 功能测试](#cucumber-功能测试)
6. [故障排查](#故障排查)

---

## 测试概览

本项目包含以下测试类型：

### 单元测试 (Unit Tests)
- **服务层测试**: `AuthServiceTest`, `UserServiceTest`, `ClassScheduleServiceTest`, `BookingServiceTest`
- **控制器测试**: `AuthControllerTest`, `BookingControllerTest`, `ClassControllerTest`, `UserControllerTest`
- **安全组件测试**: `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`, `UserDetailsServiceImplTest`
- **异常处理测试**: `GlobalExceptionHandlerTest`

### 集成测试 (Integration Tests)
- `AuthenticationIntegrationTest` - 完整的认证流程测试
- `BookingIntegrationTest` - 完整的预订流程测试

### 功能测试 (Functional Tests - Cucumber)
- `authentication.feature` - 用户认证场景
- `booking.feature` - 课程预订场景
- `class-management.feature` - 课程管理场景

---

## 运行测试

### 运行所有测试

```bash
# 运行所有测试（单元测试 + 集成测试）
mvn clean test

# 运行所有测试并生成覆盖率报告
mvn clean test jacoco:report

# 运行所有测试（跳过集成测试）
mvn clean test -DskipITs
```

### 运行特定测试类

```bash
# 运行单个测试类
mvn test -Dtest=AuthServiceTest

# 运行多个测试类
mvn test -Dtest=AuthServiceTest,UserServiceTest

# 运行特定包的所有测试
mvn test -Dtest=com.booking.system.service.*

# 运行特定测试方法
mvn test -Dtest=AuthServiceTest#shouldRegisterNewUserSuccessfully
```

### 运行集成测试

```bash
# 运行所有集成测试
mvn clean verify

# 运行特定集成测试
mvn verify -Dtest=BookingIntegrationTest
```

### 运行 Cucumber 功能测试

```bash
# 运行所有 Cucumber 测试
mvn test -Dtest=CucumberTestRunner

# 运行特定 Feature 文件
mvn test -Dcucumber.options="classpath:features/authentication.feature"

# 运行特定场景标签
mvn test -Dcucumber.options="--tags @smoke"
```

---

## 生成测试报告

### JaCoCo 代码覆盖率报告

```bash
# 生成覆盖率报告
mvn jacoco:report

# 在浏览器中打开报告
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html  # Windows
```

报告位置：`target/site/jacoco/index.html`

#### 覆盖率报告包含：
- **指令覆盖率 (Instruction Coverage)**: 字节码级别的覆盖率
- **行覆盖率 (Line Coverage)**: 源代码行覆盖率
- **分支覆盖率 (Branch Coverage)**: 条件分支覆盖率
- **圈复杂度 (Cyclomatic Complexity)**: 代码复杂度
- **方法覆盖率 (Method Coverage)**: 方法级别的覆盖率
- **类覆盖率 (Class Coverage)**: 类级别的覆盖率

### Cucumber HTML 报告

```bash
# 运行 Cucumber 测试后会自动生成
mvn test -Dtest=CucumberTestRunner

# 打开报告
open target/cucumber-reports/cucumber-pretty.html
```

报告位置：
- `target/cucumber-reports/cucumber-pretty.html` - 美化的 HTML 报告
- `target/cucumber-reports/cucumber.json` - JSON 格式报告
- `target/cucumber-reports/cucumber.xml` - JUnit XML 格式报告

### Surefire 测试报告

```bash
# 生成 Surefire 报告
mvn test

# 打开报告
open target/surefire-reports/index.html
```

报告位置：`target/surefire-reports/`

---

## 测试覆盖率

### 配置说明

项目配置了 80% 的最低代码覆盖率要求（在 `pom.xml` 中配置）：

```xml
<rule>
    <limits>
        <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.80</minimum>
        </limit>
    </limits>
</rule>
```

### 查看覆盖率

#### 命令行查看
```bash
# 运行测试并显示覆盖率
mvn clean test jacoco:report

# 查看覆盖率摘要
grep -A 10 "Total" target/site/jacoco/index.html
```

#### 理解覆盖率指标

| 指标 | 说明 | 目标 |
|------|------|------|
| Line Coverage | 执行的代码行百分比 | ≥ 80% |
| Branch Coverage | 执行的条件分支百分比 | ≥ 70% |
| Method Coverage | 被测试调用的方法百分比 | ≥ 90% |
| Class Coverage | 被测试使用的类百分比 | ≥ 90% |

### 提高覆盖率

如果覆盖率低于 80%，检查以下内容：

1. **遗漏的测试场景**
   - 错误处理路径
   - 边界条件
   - 异常情况

2. **未测试的代码分支**
   - if/else 语句的两个分支
   - switch/case 的所有 case
   - try/catch 的异常处理

3. **私有方法**
   - 通过公共方法间接测试
   - 或使用反射测试

---

## Cucumber 功能测试

### Feature 文件位置

```
src/test/resources/features/
├── authentication.feature
├── booking.feature
└── class-management.feature
```

### 步骤定义位置

```
src/test/java/com/booking/system/cucumber/steps/
├── AuthenticationSteps.java
└── BookingSteps.java
```

### 编写新的 Cucumber 测试

1. **创建 Feature 文件**

```gherkin
Feature: 新功能名称

  Scenario: 场景描述
    Given 前置条件
    When 执行操作
    Then 预期结果
    And 额外验证
```

2. **实现步骤定义**

```java
@Given("前置条件")
public void 前置条件() {
    // 实现步骤
}

@When("执行操作")
public void 执行操作() {
    // 实现步骤
}

@Then("预期结果")
public void 预期结果() {
    // 断言验证
}
```

3. **运行测试**

```bash
mvn test -Dtest=CucumberTestRunner
```

### Cucumber 报告解读

- ✅ **Passed**: 场景成功执行
- ❌ **Failed**: 场景执行失败
- ⏭️ **Skipped**: 场景被跳过
- ⚠️ **Pending**: 步骤未实现

---

## 故障排查

### 常见问题

#### 1. 测试失败：数据库连接问题

**问题**: `Caused by: org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC connection`

**解决方案**:
```bash
# 确保 H2 数据库配置正确
# 检查 src/test/resources/application-test.yml
```

#### 2. 测试失败：端口已被占用

**问题**: `Port 8080 was already in use`

**解决方案**:
```bash
# 使用随机端口
# 在 application-test.yml 中配置:
server:
  port: 0

# 或在测试类上使用:
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

#### 3. 覆盖率报告未生成

**问题**: JaCoCo 报告未生成

**解决方案**:
```bash
# 确保按正确顺序执行
mvn clean test jacoco:report

# 检查 jacoco.exec 文件是否存在
ls -la target/jacoco.exec
```

#### 4. Cucumber 测试找不到 Feature 文件

**问题**: `No features found`

**解决方案**:
```bash
# 确保 Feature 文件在正确位置
ls -la src/test/resources/features/

# 检查 CucumberOptions 配置
# features = "classpath:features"
```

#### 5. 测试超时

**问题**: 测试执行时间过长

**解决方案**:
```bash
# 增加超时时间
mvn test -Dsurefire.timeout=300

# 或跳过慢速测试
mvn test -DskipSlowTests
```

### 调试测试

#### 使用 Maven 调试

```bash
# 调试特定测试
mvn test -Dtest=AuthServiceTest -Dmaven.surefire.debug

# 调试所有测试
mvn test -Dmaven.surefire.debug
```

#### 在 IDE 中调试

1. 在测试类或方法上设置断点
2. 右键点击测试，选择 "Debug"
3. 测试会在断点处暂停

### 查看详细日志

```bash
# 启用详细日志
mvn test -X

# 查看特定包的日志
mvn test -Dlogging.level.com.booking.system=DEBUG

# 保存日志到文件
mvn test -X > test-output.log 2>&1
```

---

## 测试最佳实践

### 编写测试

1. **遵循 AAA 模式**
   - Arrange (准备): 设置测试数据和依赖
   - Act (执行): 调用被测方法
   - Assert (断言): 验证结果

2. **使用描述性测试名称**
   ```java
   @Test
   void shouldThrowExceptionWhenUserNotFound() {
       // 实现
   }
   ```

3. **隔离测试**
   - 每个测试独立运行
   - 使用 `@BeforeEach` 和 `@AfterEach` 清理状态
   - 使用事务回滚（`@Transactional`）

4. **使用 Mock 而不是真实实现**
   ```java
   @Mock
   private UserRepository userRepository;
   ```

5. **测试边界条件**
   - 零值、空值、null
   - 最大值、最小值
   - 异常情况

### 持续集成

在 CI/CD 流水线中运行测试：

```yaml
# GitHub Actions 示例
- name: Run tests
  run: mvn clean test

- name: Generate coverage report
  run: mvn jacoco:report

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ./target/site/jacoco/jacoco.xml
```

---

## 快速参考

### Maven 命令速查

| 命令 | 说明 |
|------|------|
| `mvn clean test` | 清理并运行所有测试 |
| `mvn clean verify` | 运行所有测试（包括集成测试） |
| `mvn jacoco:report` | 生成覆盖率报告 |
| `mvn test -Dtest=TestName` | 运行特定测试 |
| `mvn test -DskipTests` | 跳过测试 |
| `mvn test -DskipITs` | 跳过集成测试 |
| `mvn test -DfailIfNoTests=false` | 不因无测试而失败 |

### 测试目录结构

```
src/test/
├── java/
│   └── com/booking/system/
│       ├── service/           # 服务层单元测试
│       ├── controller/       # 控制器单元测试
│       ├── security/         # 安全组件测试
│       ├── exception/        # 异常处理测试
│       ├── integration/      # 集成测试
│       └── cucumber/         # Cucumber 配置和步骤
└── resources/
    ├── application.yml       # 测试配置
    ├── application-test.yml  # 测试专用配置
    └── features/           # Cucumber Feature 文件
```

---

## 获取帮助

如果遇到问题：

1. 检查 [项目文档](../CLAUDE.md)
2. 查看测试日志输出
3. 运行测试并查看详细错误信息
4. 参考本文档的故障排查部分

---

**最后更新**: 2024-02-22
