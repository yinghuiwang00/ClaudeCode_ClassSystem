# 架构知识库 - Class Booking System

此文件为Claude Code提供详细的代码库知识，使其无需重新扫描整个代码库即可理解项目架构和实现细节。

## 项目概览

**核心业务**：基于JWT认证的课程预订REST API，支持角色管理（USER, ADMIN, INSTRUCTOR）

**技术栈**：
- Java 17 + Spring Boot 3.2.2
- Spring Security + JWT认证
- Spring Data JPA + H2数据库（内存）
- Flyway数据库迁移
- Lombok + SpringDoc OpenAPI

## 代码库结构

### 1. 主要包结构
```
com.booking.system/
├── config/                    # 配置类
│   ├── SecurityConfig.java    # Spring Security配置（CSRF禁用，JWT过滤，角色权限）
│   └── OpenApiConfig.java     # Swagger UI配置
├── controller/                # REST控制器
│   ├── AuthController.java    # 认证端点（/register, /login）
│   ├── UserController.java    # 用户管理端点（/me, /users）
│   ├── ClassController.java   # 课程管理端点（CRUD操作）
│   └── BookingController.java # 预订管理端点（创建/取消预订）
├── dto/                       # 数据传输对象
│   ├── request/               # 请求DTO（输入验证）
│   │   ├── RegisterRequest.java (@Email, @NotBlank验证)
│   │   ├── LoginRequest.java
│   │   ├── BookingRequest.java
│   │   ├── CreateClassRequest.java
│   │   └── UpdateClassRequest.java
│   └── response/              # 响应DTO（序列化）
│       ├── AuthResponse.java  # 登录响应（包含JWT token）
│       ├── UserResponse.java
│       ├── ClassResponse.java
│       └── BookingResponse.java
├── entity/                    # JPA实体
│   ├── User.java              # 用户实体（角色字段：USER, ADMIN, INSTRUCTOR）
│   ├── Instructor.java        # 教练实体（与User一对一关系）
│   ├── ClassSchedule.java     # 课程实体（容量管理，乐观锁@Version）
│   └── Booking.java           # 预订实体（防止重复预订的UNIQUE约束）
├── repository/                # 数据访问层
│   ├── UserRepository.java
│   ├── InstructorRepository.java
│   ├── ClassScheduleRepository.java # 关键方法：findByIdWithLock（悲观锁）
│   └── BookingRepository.java
├── service/                   # 业务逻辑层
│   ├── AuthService.java       # 认证逻辑（密码加密，JWT生成）
│   ├── UserService.java       # 用户管理逻辑
│   ├── ClassScheduleService.java # 课程业务逻辑
│   └── BookingService.java    # 预订业务逻辑（并发控制核心）
├── security/                  # 安全组件
│   ├── JwtTokenProvider.java     # JWT令牌生成/验证
│   ├── JwtAuthenticationFilter.java # JWT请求过滤器
│   └── UserDetailsServiceImpl.java # Spring Security用户详情服务
└── exception/                 # 异常处理
    ├── GlobalExceptionHandler.java # 全局异常处理器
    ├── ResourceNotFoundException.java
    ├── BookingException.java
    ├── AuthenticationException.java
    └── ErrorResponse.java
```

### 2. 关键文件位置
- **启动类**: `ClassBookingSystemApplication.java`
- **主配置**: `src/main/resources/application.yml`
- **数据库迁移**: `src/main/resources/db/migration/V1__到V5__.sql`

## 核心实现模式

### 1. 并发控制机制
**位置**: `ClassScheduleRepository.java:15-18`
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT cs FROM ClassSchedule cs WHERE cs.id = :id")
Optional<ClassSchedule> findByIdWithLock(@Param("id") Long id);
```

**使用场景**: `BookingService.createBooking()`中预订课程时
- 防止超额预订（容量检查）
- 确保`currentBookings`计数原子更新
- 事务范围：整个`createBooking`方法都有`@Transactional`

### 2. JWT认证流程
1. 用户注册/登录 → `AuthService`验证 → 生成JWT（24小时过期）
2. 后续请求携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter`拦截 → 验证令牌 → 设置`SecurityContext`
4. 控制器通过`@AuthenticationPrincipal UserDetails`获取用户信息

### 3. 安全配置规则
**公开端点**:
- `/api/v1/auth/**` (注册/登录)
- `/h2-console/**` (H2控制台)
- `/swagger-ui/**` (API文档)
- `GET /api/v1/classes/**` (课程查询)

**角色权限**:
- `USER`: 预订课程、查看个人预订
- `INSTRUCTOR`: 管理课程（创建/更新/删除自己的课程）
- `ADMIN`: 所有权限（包括查看所有用户和预订）

### 4. DTO模式
**原则**: 实体永不直接暴露给Controller
- 请求: Request DTO + `@Valid`验证
- 响应: Response DTO + 序列化
- 转换: Service层中的`convertToResponse()`方法

## 数据库架构

### 表关系
1. **users** ←1:1→ **instructors**
2. **instructors** ←1:N→ **class_schedules**
3. **users** ←N:M→ **class_schedules** (通过**bookings**连接表)

### 关键约束
- `bookings`表: `UNIQUE(user_id, class_schedule_id)` 防止重复预订
- `class_schedules`表: `CHECK (current_bookings <= capacity)` 容量验证
- `class_schedules`表: `CHECK (end_time > start_time)` 时间验证

### Flyway迁移版本
- V1: 创建users表
- V2: 创建instructors表
- V3: 创建class_schedules表（包含容量检查）
- V4: 创建bookings表（包含唯一约束）
- V5: 添加version列（乐观锁）

## API端点摘要

### 认证端点（公开）
- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/login` - 用户登录（返回JWT）

### 用户端点（需认证）
- `GET /api/v1/users/me` - 获取当前用户信息（USER角色）
- `GET /api/v1/users` - 获取所有用户（仅ADMIN）
- `GET /api/v1/users/{id}` - 获取指定用户（仅ADMIN）

### 课程端点
- `GET /api/v1/classes` - 列出所有课程（公开）
- `GET /api/v1/classes?availableOnly=true` - 列出可预订课程（公开）
- `GET /api/v1/classes/{id}` - 获取课程详情（公开）
- `POST /api/v1/classes` - 创建课程（ADMIN/INSTRUCTOR）
- `PUT /api/v1/classes/{id}` - 更新课程（ADMIN/INSTRUCTOR）
- `DELETE /api/v1/classes/{id}` - 取消课程（ADMIN/INSTRUCTOR）

### 预订端点
- `POST /api/v1/bookings` - 预订课程（USER）
- `DELETE /api/v1/bookings/{id}` - 取消预订（USER）
- `GET /api/v1/bookings/my-bookings` - 获取我的预订（USER）
- `GET /api/v1/bookings` - 获取所有预订（仅ADMIN）

## 关键业务逻辑

### 预订流程（`BookingService.createBooking()`）
1. 使用`findByIdWithLock()`获取课程（悲观锁）
2. 验证课程状态是否为"SCHEDULED"
3. 检查课程是否已开始（开始时间>当前时间）
4. 验证容量：`currentBookings < capacity`
5. 检查是否已预订：`bookingRepository.existsByUserIdAndClassScheduleId()`
6. 创建预订记录
7. 原子增加`currentBookings`计数

### 取消预订流程
1. 获取预订记录
2. 验证用户权限（只能取消自己的预订）
3. 使用`findByIdWithLock()`获取课程
4. 原子减少`currentBookings`计数
5. 更新预订状态为"CANCELLED"

## 配置参数

### application.yml关键设置
```yaml
server:
  port: 8080

jwt:
  secret: "your-jwt-secret-key-change-in-production"  # JWT签名密钥
  expiration: 86400000  # 24小时（毫秒）

spring:
  datasource:
    url: jdbc:h2:mem:bookingdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ''

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway管理架构，Hibernate仅验证
    show-sql: true  # 开发环境显示SQL

  h2:
    console:
      enabled: true
      path: /h2-console
```

## 架构限制与改进点

### 当前限制
1. **H2内存数据库**：数据不持久，重启丢失
2. **字段注入**：Service类使用`@Autowired`字段注入（应改为构造函数注入）
3. **事务边界过宽**：`@Transactional`包含DTO转换逻辑
4. **缺少缓存层**：所有查询直接访问数据库
5. **同步处理**：无异步支持（如邮件通知）

### 建议改进
1. **数据库迁移**：H2 → PostgreSQL/MySQL（生产环境）
2. **依赖注入**：字段注入 → 构造函数注入
3. **缓存集成**：添加Redis缓存高频数据
4. **异步处理**：添加`@Async`或消息队列
5. **监控指标**：集成Spring Boot Actuator + Prometheus

## 开发工作流

### 添加新端点步骤
1. 在`dto/request/`和`dto/response/`创建DTO
2. 在对应Service中添加业务逻辑
3. 在Controller中添加端点方法
4. 更新Swagger文档（通过注解自动生成）

### 修改安全规则
编辑`SecurityConfig.java`中的`filterChain()`方法：
- 使用`requestMatchers()`配置路径规则
- 使用`hasRole()`配置角色权限（注意：Spring Security自动添加"ROLE_"前缀）

### 数据库调试
- H2控制台：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:bookingdb`
- 用户: `sa`，密码: 空

## 测试指南

### 角色提升（开发测试）
默认注册用户为`ROLE_USER`，测试ADMIN/INSTRUCTOR功能需：
```sql
-- 在H2控制台中执行
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'user@example.com';
UPDATE users SET role = 'ROLE_INSTRUCTOR' WHERE email = 'user@example.com';
```

### API测试流程
1. 注册用户 → 获取JWT
2. 在Swagger UI中点击"Authorize"按钮
3. 输入: `Bearer <your-token>`
4. 测试受保护端点

## 扩展架构需求

基于ARCHITECTURE.md分析，未来扩展方向：

### 短期（生产就绪）
1. 切换到PostgreSQL/MySQL
2. 添加Redis缓存层
3. 改用构造函数注入
4. 添加基本监控

### 中期（性能优化）
1. 引入消息队列（RabbitMQ/Kafka）
2. 实现事件驱动架构
3. 优化事务粒度
4. 添加异步通知

### 长期（架构升级）
1. 微服务拆分（用户服务、课程服务、预订服务）
2. CQRS读写分离
3. 领域驱动设计（DDD）
4. 完整可观测性（分布式追踪、指标监控）

---

**最后更新**: 2026-02-07
**知识库版本**: 1.0
**覆盖范围**: 完整代码库架构和实现细节

下次Claude Code会话时，请先阅读此文件以了解代码库上下文，无需重新扫描所有源文件。