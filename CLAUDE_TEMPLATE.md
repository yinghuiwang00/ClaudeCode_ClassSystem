# CLAUDE.md - Spring Boot 项目开发指南

本文档是 Claude Code 协助开发 Java Spring Boot 项目的核心指导文档。

---

## 目录

1. [项目规划与需求分析](#1-项目规划与需求分析)
2. [DDD 架构设计](#2-ddd-架构设计)
3. [TDD 测试驱动开发](#3-tdd-测试驱动开发)
4. [编码风格与规范](#4-编码风格与规范)
5. [Git 工作流](#5-git-工作流)
6. [CI/CD 持续集成](#6-cicd-持续集成)
7. [文档维护指南](#7-文档维护指南)

---

## 1. 项目规划与需求分析

### 1.1 需求分析流程

在开始任何新功能开发前，必须完成以下步骤：

#### Step 1: 需求收集
- **功能需求**: 系统应该做什么
- **非功能需求**: 性能、安全、可扩展性
- **约束条件**: 技术、时间、资源限制

#### Step 2: 需求规格说明
为每个功能创建需求文档，包含：
- 功能描述
- 用户故事（As a... I want... So that...）
- 验收标准
- 依赖关系
- 风险评估

#### Step 3: 领域建模
- 识别核心领域概念
- 定义领域边界和上下文映射
- 确定聚合根和实体关系

### 1.2 项目规划模板

```markdown
## 功能名称

### 背景
描述为什么需要这个功能

### 用户故事
- **用户角色**: 谁会使用这个功能
- **目标**: 用户想要完成什么
- **价值**: 为什么这对用户重要

### 功能需求
1. [REQ-001] 需求描述
2. [REQ-002] 需求描述

### 验收标准
- [ ] 验收标准 1
- [ ] 验收标准 2

### 技术考虑
- 性能要求
- 安全要求
- 兼容性要求

### 依赖项
- 依赖的功能/模块
- 需要的 API
- 第三方服务

### 风险
- 潜在的技术风险
- 业务风险
- 缓解措施
```

### 1.3 需求优先级

使用 MoSCoW 方法分类需求：
- **Must Have**: 必须有（核心功能）
- **Should Have**: 应该有（重要但可延后）
- **Could Have**: 可以有（锦上添花）
- **Won't Have**: 不会有（当前版本不实现）

---

## 2. DDD 架构设计

### 2.1 领域驱动设计核心概念

#### 领域层结构
```
domain/
├── model/              # 领域模型
│   ├── aggregate/     # 聚合根
│   ├── entity/        # 实体
│   ├── valueobject/   # 值对象
│   └── event/         # 领域事件
├── repository/        # 仓储接口
├── service/           # 领域服务
└── specification/     # 规约
```

#### 应用层结构
```
application/
├── service/           # 应用服务
├── dto/              # 数据传输对象
│   ├── request/      # 请求 DTO
│   ├── response/     # 响应 DTO
│   └── command/      # 命令对象
├── assembler/        # DTO 转换器
└── facade/           # 门面接口
```

#### 基础设施层结构
```
infrastructure/
├── persistence/      # 持久化实现
│   ├── repository/   # 仓储实现
│   └── mapper/       # MyBatis Mapper
├── config/          # 配置类
├── cache/           # 缓存实现
└── messaging/       # 消息队列实现
```

### 2.2 聚合设计原则

```java
// 聚合根示例
@Entity
public class Order {
    private OrderId id;
    private List<OrderItem> items;
    private OrderStatus status;

    // 保持聚合一致性
    public void addItem(Product product, int quantity) {
        if (this.status != OrderStatus.NEW) {
            throw new IllegalStateException("Cannot add items to confirmed order");
        }
        items.add(new OrderItem(product, quantity));
    }

    // 领域事件
    public Order confirm() {
        this.status = OrderStatus.CONFIRMED;
        // 发布领域事件
        DomainEventPublisher.publish(new OrderConfirmedEvent(this.id));
        return this;
    }
}
```

### 2.3 值对象设计

```java
// 值对象示例：不可变、无身份标识
@Embeddable
public final class Money implements ValueObject {
    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        validate(amount, currency);
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    // 值对象相等性
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}
```

### 2.4 领域服务设计

```java
@Service
@DomainService
public class OrderDomainService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    // 需要多个聚合协作时使用领域服务
    public void validateOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            if (!inventoryService.isAvailable(product, item.getQuantity())) {
                throw new InsufficientStockException(product.getId());
            }
        }
    }
}
```

### 2.5 仓储接口设计

```java
// 领域层定义接口
public interface OrderRepository extends Repository<Order, OrderId> {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    void delete(OrderId id);

    List<Order> findByCustomer(CustomerId customerId);
    Optional<Order> findByOrderNumber(OrderNumber orderNumber);
}

// 基础设施层实现
@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final SpringDataOrderRepository springRepository;

    @Override
    public Order save(Order order) {
        return springRepository.save(order);
    }
}
```

### 2.6 领域事件设计

```java
// 领域事件基类
public abstract class DomainEvent {
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected DomainEvent() {
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }
}

// 具体领域事件
public class OrderConfirmedEvent extends DomainEvent {
    private final OrderId orderId;
    private final CustomerId customerId;

    // 构造函数和 getter
}

// 领域事件发布器
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }
}

// 事件处理器
@Component
public class OrderEventHandler {
    @EventListener
    public void handle(OrderConfirmedEvent event) {
        // 处理订单确认后的业务逻辑
    }
}
```

---

## 3. TDD 测试驱动开发

### 3.1 TDD 循环

```
RED → GREEN → REFACTOR
```

1. **RED**: 编写一个失败的测试
2. **GREEN**: 编写最少代码让测试通过
3. **REFACTOR**: 重构代码保持可读性

### 3.2 测试层次金字塔

```
        /\
       /E2E\        - 端到端测试 (5%)
      /------\
     /Integration\   - 集成测试 (15%)
    /------------\
   /   Unit       \  - 单元测试 (80%)
  /----------------\
```

### 3.3 单元测试规范

#### 测试命名规范
```java
@Test
void shouldThrowExceptionWhenInvalidInput() {}

@Test
void shouldReturnSuccessWhenValidUser() {}

@Test
void shouldCalculateTaxWithGivenRate() {}
```

#### Given-When-Then 模式
```java
@Test
@DisplayName("Should create order with valid items")
void shouldCreateOrderWithValidItems() {
    // Given
    Customer customer = new Customer("John");
    Product product = new Product("Book", Money.of(100, "USD"));

    // When
    Order order = Order.create(customer, List.of(product));

    // Then
    assertThat(order.getCustomer()).isEqualTo(customer);
    assertThat(order.getTotalAmount()).isEqualTo(Money.of(100, "USD"));
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
}
```

#### Mockito 使用示例
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderWhenValidInput() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(
            customerId,
            List.of(orderItemDto)
        );

        Order mockOrder = Order.create(customer, items);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // When
        OrderResponse response = orderService.createOrder(command);

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(productId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(command))
            .isInstanceOf(ProductNotFoundException.class);
    }
}
```

### 3.4 集成测试规范

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Should create order via REST API")
    void shouldCreateOrderViaRestApi() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(...);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("NEW"));
    }
}
```

### 3.5 测试覆盖率要求

- **单元测试覆盖率**: ≥ 80%
- **集成测试覆盖率**: ≥ 60%
- **核心业务逻辑覆盖率**: 100%

```xml
<!-- pom.xml JaCoCo 配置 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 3.6 测试数据构建

```java
// 使用建造者模式构建测试数据
public class OrderTestDataBuilder {
    private OrderId orderId = OrderId.generate();
    private CustomerId customerId = CustomerId.generate();
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.NEW;

    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }

    public OrderTestDataBuilder withCustomerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }

    public OrderTestDataBuilder withItems(List<OrderItem> items) {
        this.items = items;
        return this;
    }

    public OrderTestDataBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public Order build() {
        return Order.restore(orderId, customerId, items, status);
    }
}

// 使用
Order order = OrderTestDataBuilder.anOrder()
    .withCustomerId(customerId)
    .withItems(items)
    .build();
```

---

## 4. 编码风格与规范

### 4.1 Java 代码规范

#### 命名规范
```java
// 类名：大驼峰
public class OrderService {}

// 接口：动词或名词 + able/ible
public interface OrderRepository {}
public interface Serializable {}

// 方法名：小驼峰，动词开头
public void createOrder() {}
public boolean isValid() {}
public Order getOrder() {}

// 变量名：小驼峰
private String userName;
private final List<OrderItem> items;

// 常量：全大写，下划线分隔
private static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_CURRENCY = "USD";

// 包名：全小写
package com.example.order.service;

// 泛型：单个大写字母
public interface Repository<T, ID> {}
public class Pair<K, V> {}
```

#### 类结构顺序
```java
// 1. 静态常量
private static final String LOGGER_NAME = "OrderService";

// 2. 静态变量
private static int instanceCount;

// 3. 实例变量（按访问权限排序：private -> protected -> public）
private final OrderRepository orderRepository;
private String currentUser;

// 4. 构造函数
public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
}

// 5. 工厂方法
public static OrderService create(OrderRepository repo) {
    return new OrderService(repo);
}

// 6. 公共方法
public Order createOrder(CreateOrderCommand command) {}

// 7. 受保护方法
protected void validateCommand(CreateOrderCommand command) {}

// 8. 私有方法
private void sendNotification(Order order) {}

// 9. getter/setter
public OrderRepository getOrderRepository() {
    return orderRepository;
}

// 10. 内部类
private static class OrderValidator {}
```

#### 方法设计原则
```java
// 单一职责
public void createOrder(CreateOrderCommand command) {} // 好
public void createOrderAndNotifyAndLog(...) {} // 差

// 参数数量 ≤ 4
public void createUser(String name, String email) {} // 好
public void createUser(String name, String email, String phone, String address, String ...) {} // 差
// 使用参数对象替代
public void createUser(CreateUserCommand command) {} // 好

// 返回类型明确
public Order findById(Long id) {} // 好
public Optional<Order> findById(Long id) {} // 更好
public Object findById(Long id) {} // 差
```

#### 异常处理规范
```java
// 定义业务异常
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
        BusinessException ex) {
        ErrorResponse response = ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(
            ErrorResponse.of("INTERNAL_SERVER_ERROR")
        );
    }
}

// 使用异常
public Order createOrder(CreateOrderCommand command) {
    if (command.getCustomerId() == null) {
        throw new BusinessException(
            ErrorCode.INVALID_INPUT,
            "Customer ID is required"
        );
    }
    // ...
}
```

### 4.2 JPA/Hibernate 规范

#### Entity 设计
```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
                orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // 使用构造函数保证对象完整性
    protected Order() {} // JPA 要求

    public Order(String orderNumber, OrderStatus status) {
        this.orderNumber = orderNumber;
        this.status = status;
    }

    // 领域方法
    public void addItem(OrderItem item) {
        this.items.add(item);
        this.totalAmount = calculateTotal();
    }

    // equals 和 hashCode 只基于业务标识
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return orderNumber.equals(order.orderNumber);
    }
}
```

#### Repository 命名规范
```java
// JPA Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 查询方法命名规范
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatusAndCreatedDateAfter(
        OrderStatus status,
        LocalDateTime date
    );

    // @Query 自定义查询
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId " +
           "AND o.status = :status")
    List<Order> findOrdersByCustomerAndStatus(
        @Param("customerId") Long customerId,
        @Param("status") OrderStatus status
    );

    // 分页查询
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // 统计查询
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    // 投影查询（性能优化）
    @Query("SELECT NEW com.example.dto.OrderSummary(o.id, o.orderNumber, o.status) " +
           "FROM Order o WHERE o.customer.id = :customerId")
    List<OrderSummary> findSummariesByCustomerId(@Param("customerId") Long customerId);
}
```

### 4.3 MyBatis 规范

#### Mapper 接口
```java
@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO orders (order_number, customer_id, status, total_amount) " +
            "VALUES (#{orderNumber}, #{customerId}, #{status}, #{totalAmount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);

    @Select("SELECT * FROM orders WHERE customer_id = #{customerId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "orderNumber", column = "order_number"),
        @Result(property = "items", column = "id",
                many = @Many(select = "findItemsByOrderId"))
    })
    List<Order> findByCustomerId(Long customerId);

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId}")
    List<OrderItem> findItemsByOrderId(Long orderId);
}
```

### 4.4 代码审查检查清单

#### 功能性
- [ ] 代码实现了需求文档中的所有功能点
- [ ] 所有边界条件都已处理
- [ ] 异常情况有适当的错误处理
- [ ] 日志记录在关键位置

#### 代码质量
- [ ] 方法长度 < 50 行
- [ ] 类长度 < 500 行
- [ ] 圈复杂度 < 10
- [ ] 没有重复代码（DRY 原则）

#### 最佳实践
- [ ] 使用构造函数注入而非字段注入
- [ ] 避免使用 `@Autowired` 在字段上
- [ ] 事务边界清晰
- [ ] 避免在事务中进行外部调用
- [ ] 懒加载关系正确处理 N+1 问题

#### 测试
- [ ] 单元测试覆盖率达标
- [ ] 集成测试覆盖关键流程
- [ ] 测试命名清晰描述测试意图

---

## 5. Git 工作流

### 5.1 分支策略

```
main (生产环境)
  ↑
  release/* (发布分支)
  ↑
develop (开发主分支)
  ↑
feature/* (功能分支)  hotfix/* (修复分支)  bugfix/* (Bug修复)
```

### 5.2 分支命名规范

```
feature/功能描述          # 新功能
feature/user-authentication
feature/order-management

bugfix/问题描述           # Bug 修复
bugfix/login-fails-on-ss

hotfix/紧急修复描述       # 生产环境紧急修复
hotfix/payment-gateway-down

release/版本号            # 发布准备
release/v1.2.0
```

### 5.3 提交信息规范

#### Conventional Commits 格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type 类型
```
feat:     新功能
fix:      Bug 修复
docs:     文档更新
style:    代码格式（不影响功能）
refactor: 重构（不是新功能也不是 Bug 修复）
perf:     性能优化
test:     测试相关
chore:    构建/工具链相关
ci:       CI/CD 相关
```

#### 示例
```bash
# 功能添加
git commit -m "feat(order): add order cancellation feature"

# Bug 修复
git commit -m "fix(auth): resolve login failure on special characters"

# 重构
git commit -m "refactor(payment): extract payment gateway interface"

# 文档更新
git commit -m "docs(readme): update installation instructions"

# 测试
git commit -m "test(order): add unit tests for order calculation"
```

### 5.4 Pull Request 规范

#### PR 标题格式
```
[类型] 简短描述

feat: Add user registration endpoint
fix: Resolve NPE in order processing
```

#### PR 描述模板
```markdown
## 变更类型
- [ ] 新功能
- [ ] Bug 修复
- [ ] 重构
- [ ] 文档更新
- [ ] 性能优化
- [ ] 其他

## 变更描述
简要描述本次变更的内容和目的。

## 相关 Issue
Closes #123
Related to #456

## 变更截图（如适用）
[添加截图或 GIF]

## 测试
- [ ] 单元测试已通过
- [ ] 集成测试已通过
- [ ] 手动测试已完成

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 已添加必要的测试
- [ ] 已更新相关文档
- [ ] 无合并冲突
- [ ] CI 检查通过

## 备注
任何审查者需要了解的额外信息。
```

### 5.5 Git Hook 配置

```bash
# .git/hooks/pre-commit
#!/bin/bash

# 运行单元测试
mvn test -DskipTests=false

# 运行代码格式检查
mvn spotless:check

# 如果检查失败，阻止提交
if [ $? -ne 0 ]; then
    echo "Pre-commit checks failed. Aborting commit."
    exit 1
fi
```

```bash
# .git/hooks/commit-msg
#!/bin/bash

# 检查提交信息格式
commit_regex='^(feat|fix|docs|style|refactor|perf|test|chore|ci)(\(.+\))?: .{1,50}'

if ! grep -qE "$commit_regex" "$1"; then
    echo "Invalid commit message format."
    echo "Expected format: type(scope): subject"
    echo "Example: feat(order): add order cancellation feature"
    exit 1
fi
```

---

## 6. CI/CD 持续集成

### 6.1 GitHub Actions 工作流

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Run tests
        run: mvn clean test

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml

      - name: Run SonarQube analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

      - name: Build application
        run: mvn clean package -DskipTests

      - name: Upload build artifacts
        uses: actions/upload-artifact@v3
        with:
          name: application-jar
          path: target/*.jar

  security:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'target'
          path: '.'
          format: 'HTML'

      - name: Upload vulnerability report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: target/dependency-check-report.html

  docker:
    runs-on: ubuntu-latest
    needs: build

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            username/app:${{ github.sha }}
            username/app:latest
          cache-from: type=registry,ref=username/app:buildcache
          cache-to: type=registry,ref=username/app:buildcache,mode=max
```

### 6.2 代码质量检查

#### Checkstyle 配置
```xml
<!-- checkstyle.xml -->
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>

    <module name="TreeWalker">
        <!-- 命名规范 -->
        <module name="ConstantName"/>
        <module name="LocalVariableName"/>
        <module name="MethodName"/>
        <module name="PackageName"/>
        <module name="TypeName"/>

        <!-- 导入规范 -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>

        <!-- 代码规范 -->
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="NeedBraces"/>
        <module name="OneStatementPerLine"/>
        <module name="UpperEll"/>

        <!-- 长度限制 -->
        <module name="LineLength">
            <property name="max" value="120"/>
            <property name="ignorePattern" value="^package.*|^import.*|a href|href|http://|https://|ftp://"/>
        </module>
        <module name="MethodLength">
            <property name="max" value="50"/>
        </module>
    </module>
</module>
```

#### Spotless 代码格式化
```xml
<!-- pom.xml -->
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.39.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.16.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 6.3 版本发布流程

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build and test
        run: mvn clean package

      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body: |
            ## Changes in this Release
            See CHANGELOG.md for details.
          draft: false
          prerelease: false

      - name: Upload Release Asset
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: target/app.jar
          asset_name: app-${{ github.ref }}.jar
          asset_content_type: application/java-archive
```

---

## 7. 文档维护指南

### 7.1 文档类型与更新时机

#### 架构文档 (ARCHITECTURE.md)
**更新时机**：
- 添加新的模块或组件
- 重构现有架构
- 更新技术栈或依赖
- 修改架构模式

**内容结构**：
```markdown
# 架构文档

## 系统概述

## 架构图

## 模块划分

## 技术栈

## 数据库设计

## API 设计

## 安全设计

## 部署架构

## 性能考虑

## 扩展性设计
```

#### API 文档 (API.md)
**更新时机**：
- 添加新的 API 端点
- 修改现有 API 参数或响应
- 废弃 API 端点

**内容结构**：
```markdown
# API 文档

## 认证方式

## 基础信息

## 接口列表

### 端点名称

#### 请求
```
POST /api/v1/orders
Content-Type: application/json
Authorization: Bearer {token}
```

#### 请求参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|

#### 响应
```
HTTP/1.1 201 Created
{
  "id": "123",
  "status": "CREATED"
}
```

#### 错误码
| 状态码 | 说明 |
|--------|------|
```

#### CHANGELOG.md
**更新时机**：每次发布新版本

**格式**：
```markdown
# 更新日志

## [Unreleased]

### Added
- 新功能列表

### Changed
- 变更列表

### Deprecated
- 废弃功能列表

### Removed
- 移除功能列表

### Fixed
- Bug 修复列表

### Security
- 安全修复列表

## [1.2.0] - 2024-01-15

### Added
- 新功能

### Fixed
- Bug 修复
```

### 7.2 代码注释规范

#### JavaDoc 格式
```java
/**
 * 订单服务类，负责订单的业务逻辑处理。
 *
 * <p>主要功能包括订单创建、确认、取消等核心业务流程。
 * 所有操作都在事务边界内执行，确保数据一致性。
 *
 * @author 开发者姓名
 * @since 1.0.0
 */
@Service
@Transactional
public class OrderService {

    /**
     * 创建新订单。
     *
     * <p>此方法会执行以下操作：
     * <ul>
     *   <li>验证请求数据完整性</li>
     *   <li>检查库存可用性</li>
     *   <li>创建订单实体</li>
     *   <li>发布订单创建事件</li>
     * </ul>
     *
     * @param command 订单创建命令，包含客户ID和商品列表
     * @return 创建的订单信息，包含订单ID和初始状态
     * @throws ProductNotFoundException 当商品不存在时抛出
     * @throws InsufficientStockException 当库存不足时抛出
     * @throws InvalidOrderException 当订单数据无效时抛出
     * @see CreateOrderCommand
     * @see OrderResponse
     */
    public OrderResponse createOrder(CreateOrderCommand command) {
        // 实现
    }
}
```

#### 行内注释规范
```java
public void processOrder(Order order) {
    // 好的注释：解释"为什么"
    // 使用乐观锁处理并发更新，避免悲观锁的性能开销
    int updated = orderRepository.updateWithVersion(order.getId(), order.getVersion());

    if (updated == 0) {
        // 订单已被其他事务修改，抛出并发异常
        throw new ConcurrentModificationException("Order was modified by another transaction");
    }

    // 不要写这样的注释
    // 调用更新方法 (无用注释)
    orderRepository.update(order);
}
```

### 7.3 知识库文档 (KNOWLEDGE_BASE.md)

当开发过程中积累了特定领域的知识，应更新到知识库：

```markdown
# 知识库

## 常见问题

### Q: 如何处理订单并发修改？
A: 使用乐观锁机制，在实体中添加 version 字段...

### Q: 为什么订单项使用嵌入式对象而非独立实体？
A: 订单项不存在独立的生命周期，始终从属于订单...

## 设计决策

### 订单状态机设计
决策原因：使用有限状态机管理订单状态流转
考虑的替代方案：枚举 + 状态检查
最终选择：Spring StateMachine

### 库存管理策略
决策原因：使用预扣库存而非实时扣减
考虑因素：订单取消率、超时处理

## 已知限制

1. 当前不支持跨库事务
2. 消息队列未实现持久化
3. 缓存未实现失效策略

## 未来改进

1. 引入事件溯源
2. 实现读写分离
3. 添加分布式事务支持
```

### 7.4 文档更新检查清单

在代码变更后，检查并更新以下文档：

- [ ] **README.md** - 如果影响了项目概述或快速开始
- [ ] **ARCHITECTURE.md** - 如果影响了架构设计
- [ ] **API.md** - 如果添加或修改了 API
- [ ] **KNOWLEDGE_BASE.md** - 如果学习了新知识或做出设计决策
- [ ] **CHANGELOG.md** - 发布新版本时
- [ ] **JavaDoc** - 如果添加或修改了公共 API
- [ ] **依赖文档** - 如果添加或移除了依赖
- [ ] **部署文档** - 如果影响了部署流程

### 7.5 自动化文档生成

#### Swagger/OpenAPI 配置
```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Management API")
                .version("1.0.0")
                .description("订单管理系统 REST API 文档")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("开发环境"),
                new Server().url("https://api.example.com").description("生产环境")
            ));
    }
}
```

#### 文档生成命令
```bash
# 生成 JavaDoc
mvn javadoc:javadoc

# 生成 OpenAPI 文档
mvn springdoc-openapi:generate

# 生成架构图
mvn plantuml:generate
```

---

## 附录 A: 快速参考

### Maven 常用命令
```bash
# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package

# 跳过测试打包
mvn package -DskipTests

# 安装到本地仓库
mvn install

# 部署到远程仓库
mvn deploy

# 查看依赖树
mvn dependency:tree

# 运行代码检查
mvn checkstyle:check

# 生成覆盖率报告
mvn jacoco:report
```

### Git 常用命令
```bash
# 创建功能分支
git checkout -b feature/new-feature

# 暂存所有更改
git add .

# 提交更改
git commit -m "feat: add new feature"

# 推送到远程
git push origin feature/new-feature

# 拉取最新代码
git pull origin develop

# 合并分支
git merge feature/new-feature

# 解决冲突后继续
git add .
git commit
```

### Spring Boot 常用注解
```java
// 依赖注入
@Autowired        // 字段注入（不推荐）
@Component        // 通用组件
@Service          // 服务层
@Repository       // 数据访问层
@Controller       // 控制器
@RestController   // REST 控制器

// 配置
@Configuration    // 配置类
@Bean             // Bean 定义
@Value            # 属性注入
@Profile          # 环境配置

// Web
@RequestMapping  # 请求映射
@GetMapping       # GET 请求
@PostMapping      # POST 请求
@PutMapping       # PUT 请求
@DeleteMapping    # DELETE 请求
@PathVariable     # 路径参数
@RequestParam     # 请求参数
@RequestBody      # 请求体

// 数据访问
@Entity           # JPA 实体
@Table            # 表映射
@Id               # 主键
@GeneratedValue    # 主键生成策略
@Column           # 列映射
@OneToMany        # 一对多关系
@ManyToOne        # 多对一关系
@JoinColumn       # 连接列

// 事务
@Transactional    # 事务边界
@Transactional(readOnly = true)  # 只读事务

// 验证
@Valid            # 触发验证
@NotNull          # 非空
@NotBlank         # 非空且非空白
@Email            # 邮箱格式
@Size             # 大小限制
@Min / @Max       # 数值范围

// 测试
@SpringBootTest    # 集成测试
@WebMvcTest       # MVC 层测试
@DataJpaTest      # JPA 层测试
@MockBean         # Mock Bean
@Mock             # Mockito Mock
```

---

## 附录 B: 项目模板结构

```
project-root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── config/           # 配置类
│   │   │           ├── controller/       # 控制器
│   │   │           ├── dto/              # DTO
│   │   │           │   ├── request/      # 请求 DTO
│   │   │           │   ├── response/     # 响应 DTO
│   │   │           │   └── command/      # 命令对象
│   │   │           ├── entity/           # JPA 实体
│   │   │           ├── service/          # 服务层
│   │   │           ├── repository/       # 数据访问层
│   │   │           ├── domain/           # 领域模型
│   │   │           ├── exception/        # 异常处理
│   │   │           └── util/             # 工具类
│   │   └── resources/
│   │       ├── application.yml          # 主配置
│   │       ├── application-dev.yml       # 开发环境
│   │       ├── application-prod.yml      # 生产环境
│   │       ├── db/migration/             # Flyway 迁移
│   │       └── static/                   # 静态资源
│   └── test/
│       ├── java/                         # 测试代码
│       └── resources/                    # 测试资源
├── .github/
│   └── workflows/                         # CI/CD 配置
├── docs/                                  # 文档
├── scripts/                               # 脚本
├── .gitignore
├── pom.xml
├── README.md
├── CLAUDE.md                              # 本文档
├── ARCHITECTURE.md                        # 架构文档
├── API.md                                 # API 文档
├── CHANGELOG.md                           # 更新日志
└── KNOWLEDGE_BASE.md                      # 知识库
```

---

**文档版本**: 1.0.0
**最后更新**: 2024-02-22
**维护者**: 开发团队
