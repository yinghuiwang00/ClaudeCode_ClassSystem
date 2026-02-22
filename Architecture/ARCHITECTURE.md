# Class Booking System - 架构文档

## 文档信息
- **项目名称**: Class Booking System
- **版本**: 1.0.0
- **最后更新**: 2026-02-01
- **文档类型**: 系统架构设计文档

---

## 目录
1. [系统概述](#系统概述)
2. [技术栈](#技术栈)
3. [架构设计](#架构设计)
4. [项目结构](#项目结构)
5. [核心组件](#核心组件)
6. [数据模型](#数据模型)
7. [API设计](#api设计)
8. [安全机制](#安全机制)
9. [架构分析](#架构分析)
10. [可扩展性评估](#可扩展性评估)
11. [改进建议](#改进建议)
12. [部署架构](#部署架构)

---

## 系统概述

### 业务背景
Class Booking System 是一个课程预订管理系统，提供完整的用户认证、课程管理和预订功能。系统支持多角色权限控制，可用于健身房、培训机构、教育机构等场景。

### 核心功能
- **用户认证**: 基于JWT的无状态身份验证
- **角色管理**: 支持USER、ADMIN、INSTRUCTOR三种角色
- **课程管理**: 课程创建、更新、查询、取消
- **预订管理**: 课程预订、取消、查看预订记录
- **并发控制**: 防止超额预订和双重预订
- **API文档**: 提供Swagger UI交互式文档

### 系统特性
- RESTful API设计
- 数据库版本管理(Flyway)
- 自动化API文档生成
- 并发安全的预订机制
- 完善的异常处理

---

## 技术栈

### 核心框架
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.2 | 应用框架 |
| Spring Security | 6.2.1 | 安全框架 |
| Spring Data JPA | 3.2.2 | 数据持久化 |

### 数据库
| 技术 | 版本 | 用途 |
|------|------|------|
| H2 Database | 2.2.224 | 内存数据库(开发/演示) |
| PostgreSQL | - | 生产数据库(可选) |
| Flyway | 10.4.1 | 数据库迁移 |

### 安全与认证
| 技术 | 版本 | 用途 |
|------|------|------|
| JJWT | 0.12.3 | JWT令牌处理 |
| BCrypt | - | 密码加密 |

### 开发工具
| 技术 | 版本 | 用途 |
|------|------|------|
| Lombok | 1.18.30 | 代码生成 |
| SpringDoc OpenAPI | 2.3.0 | API文档 |
| Maven | 3.8+ | 构建工具 |

---

## 架构设计

### 整体架构

系统采用经典的**三层架构(Three-Tier Architecture)**:

```
┌─────────────────────────────────────────────────────────┐
│                    Client Layer                          │
│                 (Browser / Mobile App)                   │
└─────────────────────────────────────────────────────────┘
                           │
                           │ HTTPS/JWT
                           ▼
┌─────────────────────────────────────────────────────────┐
│                 Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    Auth      │  │    Class     │  │   Booking    │  │
│  │  Controller  │  │  Controller  │  │  Controller  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │       JWT Authentication Filter                   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  Business Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    Auth      │  │    Class     │  │   Booking    │  │
│  │   Service    │  │   Service    │  │   Service    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Business Logic & Validation               │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                 Persistence Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    User      │  │    Class     │  │   Booking    │  │
│  │  Repository  │  │  Repository  │  │  Repository  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Spring Data JPA                      │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                            │
│                  H2 / PostgreSQL                         │
└─────────────────────────────────────────────────────────┘
```

### 数据流

#### 典型的预订流程

```
1. 用户请求
   POST /api/v1/bookings
   Headers: Authorization: Bearer <JWT>
   Body: { "classScheduleId": 1, "notes": "..." }

2. JWT过滤器验证
   JwtAuthenticationFilter → 验证令牌 → 提取用户信息

3. Controller层
   BookingController.createBooking() → 接收请求 → 验证参数

4. Service层
   BookingService.createBooking()
   ├── 查询用户信息
   ├── 加锁查询课程 (PESSIMISTIC_WRITE)
   ├── 业务验证
   │   ├── 课程状态检查
   │   ├── 时间检查
   │   ├── 容量检查
   │   └── 重复预订检查
   ├── 创建预订记录
   ├── 更新课程容量
   └── 返回响应DTO

5. Repository层
   ├── BookingRepository.save()
   ├── ClassScheduleRepository.findByIdWithLock()
   └── UserRepository.findByEmail()

6. 返回响应
   BookingResponse DTO → JSON → Client
```

---

## 项目结构

### 目录结构

```
class-booking-system/
├── src/
│   ├── main/
│   │   ├── java/com/booking/system/
│   │   │   ├── ClassBookingSystemApplication.java  # 启动类
│   │   │   │
│   │   │   ├── config/                  # 配置类
│   │   │   │   ├── SecurityConfig.java      # Spring Security配置
│   │   │   │   └── OpenApiConfig.java       # Swagger配置
│   │   │   │
│   │   │   ├── controller/              # REST控制器
│   │   │   │   ├── AuthController.java      # 认证端点
│   │   │   │   ├── UserController.java      # 用户管理端点
│   │   │   │   ├── ClassController.java     # 课程管理端点
│   │   │   │   └── BookingController.java   # 预订管理端点
│   │   │   │
│   │   │   ├── dto/                     # 数据传输对象
│   │   │   │   ├── request/             # 请求DTO
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── BookingRequest.java
│   │   │   │   │   ├── CreateClassRequest.java
│   │   │   │   │   └── UpdateClassRequest.java
│   │   │   │   └── response/            # 响应DTO
│   │   │   │       ├── AuthResponse.java
│   │   │   │       ├── UserResponse.java
│   │   │   │       ├── ClassResponse.java
│   │   │   │       └── BookingResponse.java
│   │   │   │
│   │   │   ├── entity/                  # JPA实体
│   │   │   │   ├── User.java                # 用户实体
│   │   │   │   ├── Instructor.java          # 教练实体
│   │   │   │   ├── ClassSchedule.java       # 课程实体
│   │   │   │   └── Booking.java             # 预订实体
│   │   │   │
│   │   │   ├── repository/              # 数据访问层
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── InstructorRepository.java
│   │   │   │   ├── ClassScheduleRepository.java
│   │   │   │   └── BookingRepository.java
│   │   │   │
│   │   │   ├── service/                 # 业务逻辑层
│   │   │   │   ├── AuthService.java         # 认证服务
│   │   │   │   ├── UserService.java         # 用户服务
│   │   │   │   ├── ClassScheduleService.java # 课程服务
│   │   │   │   └── BookingService.java      # 预订服务
│   │   │   │
│   │   │   ├── security/                # 安全组件
│   │   │   │   ├── JwtTokenProvider.java        # JWT生成/验证
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT过滤器
│   │   │   │   └── UserDetailsServiceImpl.java  # 用户详情服务
│   │   │   │
│   │   │   └── exception/               # 异常处理
│   │   │       ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── BookingException.java
│   │   │       ├── AuthenticationException.java
│   │   │       └── ErrorResponse.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml          # 应用配置
│   │       └── db/migration/            # Flyway迁移脚本
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_instructors_table.sql
│   │           ├── V3__create_class_schedules_table.sql
│   │           ├── V4__create_bookings_table.sql
│   │           └── V5__add_version_column.sql
│   │
│   └── test/
│       └── java/com/booking/system/     # 测试代码
│
├── target/                              # Maven构建输出
├── pom.xml                              # Maven配置
├── README.md                            # 项目说明
└── ARCHITECTURE.md                      # 架构文档(本文档)
```

### 分层职责

#### 1. Controller层 (Presentation)
- **职责**: 处理HTTP请求，参数验证，调用Service层
- **特点**:
  - 使用`@RestController`标注
  - 返回DTO对象，不直接返回Entity
  - 处理HTTP状态码和响应格式

#### 2. Service层 (Business Logic)
- **职责**: 业务逻辑处理，事务管理，实体与DTO转换
- **特点**:
  - 使用`@Service`标注
  - 使用`@Transactional`管理事务
  - 调用Repository进行数据操作

#### 3. Repository层 (Data Access)
- **职责**: 数据访问，数据库操作
- **特点**:
  - 继承`JpaRepository`
  - 支持自定义查询方法
  - 使用`@Lock`实现并发控制

#### 4. Entity层 (Domain Model)
- **职责**: 数据模型定义，映射数据库表
- **特点**:
  - 使用JPA注解
  - 包含业务字段和关系映射

---

## 核心组件

### 1. 认证与授权组件

#### JWT Token Provider
**位置**: `security/JwtTokenProvider.java`

**核心功能**:
```java
public class JwtTokenProvider {
    // 生成JWT令牌
    public String generateToken(Authentication authentication);

    // 从令牌中提取用户名
    public String getUsernameFromToken(String token);

    // 验证令牌有效性
    public boolean validateToken(String token);
}
```

**配置参数**:
- 密钥: 256位随机密钥
- 算法: HS256
- 过期时间: 24小时(86400000ms)

#### JWT Authentication Filter
**位置**: `security/JwtAuthenticationFilter.java`

**工作流程**:
```
1. 拦截请求
2. 从Header提取JWT (Authorization: Bearer <token>)
3. 验证令牌有效性
4. 提取用户信息
5. 设置SecurityContext
6. 放行请求
```

#### Security Config
**位置**: `config/SecurityConfig.java`

**关键配置**:
```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/api/v1/classes").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    );
```

### 2. 预订管理组件

#### Booking Service 核心逻辑
**位置**: `service/BookingService.java:33-70`

**并发控制机制**:
```java
@Transactional
public BookingResponse createBooking(String userEmail, BookingRequest request) {
    // 1. 使用悲观写锁获取课程
    ClassSchedule classSchedule = classScheduleRepository
        .findByIdWithLock(request.getClassScheduleId())  // PESSIMISTIC_WRITE
        .orElseThrow(...);

    // 2. 业务验证
    if (classSchedule.getCurrentBookings() >= classSchedule.getCapacity()) {
        throw new BookingException("Class is full");
    }

    // 3. 防止重复预订
    if (bookingRepository.existsByUserIdAndClassScheduleId(...)) {
        throw new BookingException("You have already booked this class");
    }

    // 4. 创建预订并更新容量
    classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
}
```

**关键特性**:
- ✅ 使用数据库悲观锁防止超售
- ✅ 事务保证数据一致性
- ✅ 多重业务验证
- ⚠️ 整个方法在事务中，锁持有时间较长

### 3. 异常处理组件

#### Global Exception Handler
**位置**: `exception/GlobalExceptionHandler.java`

**处理的异常类型**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(...);

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(...);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(...);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(...);
}
```

**统一错误响应格式**:
```json
{
  "timestamp": "2026-02-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Class is full",
  "path": "/api/v1/bookings"
}
```

---

## 数据模型

### ER图

```
┌─────────────────┐         ┌─────────────────────┐
│     Users       │         │    Instructors      │
├─────────────────┤         ├─────────────────────┤
│ id (PK)         │◄────────│ id (PK)             │
│ username        │         │ user_id (FK)        │
│ email           │         │ bio                 │
│ password_hash   │         │ specialization      │
│ first_name      │         │ ...                 │
│ last_name       │         └─────────────────────┘
│ role            │                    │
│ is_active       │                    │
│ created_at      │                    │
│ updated_at      │                    │
└─────────────────┘                    │
         │                             │
         │                             ▼
         │                   ┌─────────────────────┐
         │                   │  Class Schedules    │
         │                   ├─────────────────────┤
         │                   │ id (PK)             │
         │              ┌────│ instructor_id (FK)  │
         │              │    │ name                │
         │              │    │ description         │
         │              │    │ start_time          │
         │              │    │ end_time            │
         │              │    │ capacity            │
         │              │    │ current_bookings    │
         │              │    │ location            │
         │              │    │ status              │
         │              │    │ ...                 │
         │              │    └─────────────────────┘
         │              │              │
         │              │              │
         │              │              ▼
         │              │    ┌─────────────────────┐
         │              │    │     Bookings        │
         │              │    ├─────────────────────┤
         │              │    │ id (PK)             │
         └──────────────┼────│ user_id (FK)        │
                        └────│ class_schedule_id(FK)│
                             │ booking_status      │
                             │ booking_date        │
                             │ cancellation_date   │
                             │ notes               │
                             │ ...                 │
                             └─────────────────────┘
```

### 表结构详情

#### 1. users 表
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**索引**:
- `UNIQUE INDEX` on `username`
- `UNIQUE INDEX` on `email`

#### 2. instructors 表
```sql
CREATE TABLE instructors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    bio TEXT,
    specialization VARCHAR(200),
    years_of_experience INTEGER,
    certifications TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 3. class_schedules 表
```sql
CREATE TABLE class_schedules (
    id BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INTEGER NOT NULL,
    current_bookings INTEGER DEFAULT 0,
    location VARCHAR(200),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,  -- 乐观锁版本号
    FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE SET NULL
);
```

**索引**:
- `INDEX` on `status`
- `INDEX` on `start_time`

#### 4. bookings 表
```sql
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_schedule_id BIGINT NOT NULL,
    booking_status VARCHAR(20) DEFAULT 'CONFIRMED',
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancellation_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (class_schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE,
    UNIQUE(user_id, class_schedule_id)  -- 防止重复预订
);
```

**索引**:
- `UNIQUE INDEX` on `(user_id, class_schedule_id)`
- `INDEX` on `booking_status`

---

## API设计

### API版本控制
- 基础路径: `/api/v1/`
- 版本策略: URI版本控制

### 端点概览

#### 认证端点 (Public)
| Method | Endpoint | 描述 | 请求体 | 响应 |
|--------|----------|------|--------|------|
| POST | `/api/v1/auth/register` | 用户注册 | RegisterRequest | AuthResponse |
| POST | `/api/v1/auth/login` | 用户登录 | LoginRequest | AuthResponse |

#### 用户管理端点
| Method | Endpoint | 描述 | 权限 | 响应 |
|--------|----------|------|------|------|
| GET | `/api/v1/users/me` | 获取当前用户信息 | USER | UserResponse |
| GET | `/api/v1/users/{id}` | 获取指定用户 | ADMIN | UserResponse |
| GET | `/api/v1/users` | 获取所有用户 | ADMIN | List&lt;UserResponse&gt; |

#### 课程管理端点
| Method | Endpoint | 描述 | 权限 | 响应 |
|--------|----------|------|------|------|
| GET | `/api/v1/classes` | 列出所有课程 | Public | List&lt;ClassResponse&gt; |
| GET | `/api/v1/classes?availableOnly=true` | 列出可预订课程 | Public | List&lt;ClassResponse&gt; |
| GET | `/api/v1/classes?status=SCHEDULED` | 按状态过滤 | Public | List&lt;ClassResponse&gt; |
| GET | `/api/v1/classes/{id}` | 获取课程详情 | Public | ClassResponse |
| POST | `/api/v1/classes` | 创建课程 | ADMIN/INSTRUCTOR | ClassResponse |
| PUT | `/api/v1/classes/{id}` | 更新课程 | ADMIN/INSTRUCTOR | ClassResponse |
| DELETE | `/api/v1/classes/{id}` | 取消课程 | ADMIN/INSTRUCTOR | void |

#### 预订管理端点
| Method | Endpoint | 描述 | 权限 | 响应 |
|--------|----------|------|------|------|
| POST | `/api/v1/bookings` | 预订课程 | USER | BookingResponse |
| DELETE | `/api/v1/bookings/{id}` | 取消预订 | USER | void |
| GET | `/api/v1/bookings/my-bookings` | 获取我的预订 | USER | List&lt;BookingResponse&gt; |
| GET | `/api/v1/bookings/{id}` | 获取预订详情 | USER | BookingResponse |
| GET | `/api/v1/bookings` | 获取所有预订 | ADMIN | List&lt;BookingResponse&gt; |

### 请求/响应示例

#### 注册用户
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**响应**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "john@example.com",
  "username": "johndoe",
  "role": "ROLE_USER"
}
```

#### 预订课程
```http
POST /api/v1/bookings
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "classScheduleId": 1,
  "notes": "Looking forward to this class!"
}
```

**响应**:
```json
{
  "id": 123,
  "userId": 1,
  "userEmail": "john@example.com",
  "classScheduleId": 1,
  "className": "Yoga Basics",
  "classStartTime": "2026-03-15T10:00:00",
  "bookingStatus": "CONFIRMED",
  "bookingDate": "2026-02-01T10:30:00",
  "notes": "Looking forward to this class!"
}
```

### 错误响应格式
```json
{
  "timestamp": "2026-02-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Class is full",
  "path": "/api/v1/bookings"
}
```

**常见HTTP状态码**:
- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证
- `403 Forbidden`: 无权限
- `404 Not Found`: 资源不存在
- `409 Conflict`: 业务冲突(如重复预订)
- `500 Internal Server Error`: 服务器错误

---

## 安全机制

### 1. 认证机制

#### JWT认证流程
```
┌──────┐                                    ┌──────────┐
│Client│                                    │  Server  │
└──┬───┘                                    └────┬─────┘
   │                                             │
   │  1. POST /auth/login                        │
   │     { email, password }                     │
   ├────────────────────────────────────────────>│
   │                                             │
   │                                      2. 验证密码
   │                                      3. 生成JWT
   │                                             │
   │  4. { token, username, role }               │
   │<────────────────────────────────────────────┤
   │                                             │
   │  5. GET /bookings/my-bookings               │
   │     Authorization: Bearer <token>           │
   ├────────────────────────────────────────────>│
   │                                             │
   │                                      6. 验证JWT
   │                                      7. 提取用户信息
   │                                      8. 查询数据
   │                                             │
   │  9. [ bookings... ]                         │
   │<────────────────────────────────────────────┤
   │                                             │
```

### 2. 授权机制

#### 角色权限矩阵
| 资源 | USER | INSTRUCTOR | ADMIN |
|------|------|-----------|-------|
| 注册/登录 | ✅ | ✅ | ✅ |
| 查看课程列表 | ✅ | ✅ | ✅ |
| 预订课程 | ✅ | ✅ | ✅ |
| 取消自己的预订 | ✅ | ✅ | ✅ |
| 创建课程 | ❌ | ✅ | ✅ |
| 修改课程 | ❌ | ✅(自己的) | ✅(所有) |
| 删除课程 | ❌ | ✅(自己的) | ✅(所有) |
| 查看所有用户 | ❌ | ❌ | ✅ |
| 查看所有预订 | ❌ | ❌ | ✅ |

#### 权限控制实现
```java
// 方法级权限控制
@PreAuthorize("hasRole('ADMIN')")
public List<UserResponse> getAllUsers() { ... }

// Controller级权限控制
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }
```

### 3. 数据安全

#### 密码加密
- **算法**: BCrypt
- **强度**: 默认10轮加密
- **盐值**: 自动生成随机盐

```java
// 注册时加密
String hashedPassword = passwordEncoder.encode(plainPassword);

// 登录时验证
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
```

#### SQL注入防护
- 使用JPA/Hibernate参数化查询
- 所有用户输入都经过验证

```java
// 安全的查询方式
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
```

#### XSS防护
- 请求参数使用`@Valid`验证
- DTO字段使用Bean Validation约束

```java
public class RegisterRequest {
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

### 4. CORS配置
```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        return source;
    }
}
```

---

## 架构分析

### 优点分析

#### ✅ 1. 分层架构清晰
- **Controller → Service → Repository**三层分离
- 每层职责明确，易于维护
- 符合单一职责原则

```java
// 清晰的层次调用
Controller (HTTP处理)
  → Service (业务逻辑)
    → Repository (数据访问)
      → Database
```

#### ✅ 2. 安全机制完善
- JWT无状态认证，支持分布式部署
- BCrypt密码加密
- 基于角色的访问控制(RBAC)
- Spring Security全面防护

#### ✅ 3. 并发控制严格
```java
// BookingService.java:38-40
@Lock(LockModeType.PESSIMISTIC_WRITE)
ClassSchedule classSchedule = classScheduleRepository
    .findByIdWithLock(request.getClassScheduleId())
```
- 使用悲观锁防止超售
- 事务保证原子性
- 防止双重预订

#### ✅ 4. 数据库管理规范
- Flyway版本控制，可追溯变更
- 支持多环境迁移
- 自动化数据库升级

#### ✅ 5. API文档自动化
- SpringDoc自动生成OpenAPI规范
- Swagger UI交互式测试
- 减少文档维护成本

#### ✅ 6. DTO模式隔离
- Entity与DTO分离
- 避免过度暴露内部结构
- 灵活的数据转换

### 缺点分析

#### ⚠️ 1. 依赖注入方式不当
**问题位置**: `service/BookingService.java:24-31`

```java
// 当前实现 - 字段注入
@Autowired
private BookingRepository bookingRepository;

@Autowired
private ClassScheduleRepository classScheduleRepository;
```

**问题**:
- 字段注入无法使用final修饰符
- 难以进行单元测试(需要反射注入)
- 依赖关系不明确
- 无法保证不可变性

**建议改进**:
```java
// 推荐 - 构造函数注入
private final BookingRepository bookingRepository;
private final ClassScheduleRepository classScheduleRepository;

public BookingService(
    BookingRepository bookingRepository,
    ClassScheduleRepository classScheduleRepository) {
    this.bookingRepository = bookingRepository;
    this.classScheduleRepository = classScheduleRepository;
}
```

#### ⚠️⚠️ 2. 缺少领域模型层(Domain Layer)

**当前架构问题**:
```java
// 业务逻辑散落在Service中
public BookingResponse createBooking(...) {
    // 直接操作Entity
    if (classSchedule.getCurrentBookings() >= classSchedule.getCapacity()) {
        throw new BookingException("Class is full");
    }

    classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
}
```

**问题**:
- Entity只是数据容器，没有业务行为
- 业务规则分散在Service层
- 违反Domain-Driven Design原则
- 难以管理复杂业务逻辑

**建议引入Domain层**:
```java
// 改进 - 领域模型封装业务逻辑
public class ClassSchedule {
    // ... 字段 ...

    public BookingResult bookSeat(User user) {
        if (isFull()) {
            return BookingResult.failure("Class is full");
        }
        if (hasStarted()) {
            return BookingResult.failure("Class has already started");
        }
        this.currentBookings++;
        return BookingResult.success();
    }

    private boolean isFull() {
        return currentBookings >= capacity;
    }
}
```

#### ⚠️ 3. 事务粒度过大

**位置**: `service/BookingService.java:33`

```java
@Transactional  // 整个方法都在事务中
public BookingResponse createBooking(String userEmail, BookingRequest request) {
    User user = userRepository.findByEmail(userEmail)...;
    ClassSchedule classSchedule = classScheduleRepository.findByIdWithLock(...)...;

    // ... 业务验证 ...

    booking = bookingRepository.save(booking);
    return convertToResponse(booking);  // DTO转换也在事务中
}
```

**问题**:
- DTO转换等非数据库操作也在事务中
- 锁持有时间过长
- 影响并发性能

**建议**:
```java
@Transactional
public Booking createBookingEntity(String userEmail, BookingRequest request) {
    // 只在此方法中保持事务
    // ...
    return booking;
}

public BookingResponse createBooking(String userEmail, BookingRequest request) {
    Booking booking = createBookingEntity(userEmail, request);
    return convertToResponse(booking);  // DTO转换在事务外
}
```

#### ⚠️⚠️⚠️ 4. 单体架构的扩展瓶颈

**问题**:
```
当前架构 = 单体应用 + H2内存数据库

限制：
1. 无法水平扩展(多实例共享H2内存?)
2. 无法分模块独立部署
3. 所有流量集中在一个应用
4. 资源竞争(CPU/内存无法独立扩展)
```

**扩展性瓶颈**:
- H2内存数据库无法持久化，重启丢失数据
- 单实例部署，无法支持高可用
- 垂直扩展有限(加CPU/内存有上限)
- 无法针对高负载模块独立扩展

#### ⚠️ 5. 缺少缓存层

**问题表现**:
```java
// 每次请求都查询数据库
public List<ClassResponse> getAvailableClasses() {
    return classScheduleRepository.findAll()...;  // 每次都查库
}
```

**影响**:
- 热点数据(课程列表)重复查询
- 数据库压力大
- 响应时间慢

**建议添加缓存**:
```java
@Cacheable(value = "classes", key = "#availableOnly")
public List<ClassResponse> getClasses(boolean availableOnly) {
    // ...
}

@CacheEvict(value = "classes", allEntries = true)
public ClassResponse updateClass(Long id, UpdateClassRequest request) {
    // 更新时清除缓存
}
```

#### ⚠️ 6. 缺少异步处理能力

**当前问题**:
- 所有操作都是同步的
- 未来功能(邮件通知、短信提醒)会阻塞主流程
- 无法处理高并发突发流量

**示例痛点**:
```java
public BookingResponse createBooking(...) {
    // 创建预订
    Booking booking = bookingRepository.save(booking);

    // 如果要发送邮件，会阻塞响应
    emailService.sendConfirmation(booking);  // 同步发送，响应慢

    return convertToResponse(booking);
}
```

#### ⚠️ 7. 缺少可观测性

**缺失组件**:
- ❌ 无分布式追踪(无法追踪请求链路)
- ❌ 无业务指标监控(预订成功率、响应时间等)
- ❌ 无告警机制
- ❌ 日志聚合不完善

---

## 可扩展性评估

### 扩展场景分析

#### 场景1: 支持100万用户
| 需求 | 当前架构 | 评估 | 必要改造 |
|------|---------|------|---------|
| 用户数据存储 | H2内存数据库 | ❌ 不支持 | 迁移到PostgreSQL/MySQL |
| 并发读写 | 单实例 | ❌ 不支持 | 添加读写分离 + 主从复制 |
| 认证性能 | JWT验证 | ✅ 支持 | 无需改造 |
| 数据持久化 | 内存 | ❌ 不支持 | 使用持久化数据库 |

**结论**: 需要重大改造，至少需要：
- 切换到生产级数据库
- 添加Redis缓存
- 实现数据库主从分离

#### 场景2: 多地域部署
| 需求 | 当前架构 | 评估 | 必要改造 |
|------|---------|------|---------|
| 数据同步 | H2内存 | ❌ 不支持 | 分布式数据库或数据同步方案 |
| 会话共享 | JWT无状态 | ✅ 支持 | 无需改造 |
| 数据一致性 | 单库 | ❌ 不支持 | 分布式事务或最终一致性 |

**结论**: H2内存数据库是主要障碍，必须替换。

#### 场景3: 高并发预订(类似秒杀)
| 需求 | 当前架构 | 评估 | 必要改造 |
|------|---------|------|---------|
| 并发控制 | 悲观锁 | ⚠️ 勉强支持 | 改为Redis分布式锁 + 消息队列削峰 |
| 库存扣减 | 数据库 | ⚠️ 性能瓶颈 | Redis预扣库存 + 异步落库 |
| 流量削峰 | 无 | ❌ 不支持 | 引入消息队列(RabbitMQ/Kafka) |
| 防刷限流 | 无 | ❌ 不支持 | 添加Redis限流器 |

**建议架构**:
```
用户请求 → Nginx限流 → 应用层验证
  → Redis预扣库存 → 发送MQ消息 → 立即返回
  → MQ消费者 → 异步创建订单 → 数据库持久化
```

#### 场景4: 支付集成
| 功能 | 当前架构 | 评估 | 改造复杂度 |
|------|---------|------|-----------|
| 支付接口调用 | 无 | ✅ 可添加 | 低 - 在Service层添加 |
| 支付回调 | 无 | ✅ 可添加 | 低 - 添加Controller端点 |
| 事务一致性 | 本地事务 | ⚠️ 不够 | 中 - 需要分布式事务或补偿机制 |

**结论**: 基本功能容易添加，但需要考虑分布式事务。

#### 场景5: 邮件/短信通知
| 功能 | 当前架构 | 评估 | 改造复杂度 |
|------|---------|------|-----------|
| 发送通知 | 无 | ✅ 可添加 | 低 - 集成第三方SDK |
| 异步发送 | 无 | ⚠️ 需要改造 | 中 - 添加消息队列 |
| 失败重试 | 无 | ❌ 需要改造 | 中 - 需要可靠消息系统 |

**不建议的实现**:
```java
// ❌ 同步发送 - 会阻塞响应
public BookingResponse createBooking(...) {
    Booking booking = save(...);
    emailService.send(...);  // 如果邮件服务慢，用户等待时间长
    return response;
}
```

**推荐实现**:
```java
// ✅ 异步发送
public BookingResponse createBooking(...) {
    Booking booking = save(...);
    messagingTemplate.convertAndSend("booking.created", booking);  // 发消息
    return response;  // 立即返回
}
```

#### 场景6: 微服务拆分
**当前单体架构**:
```
┌─────────────────────────────────┐
│    Class Booking System         │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ User │ │Class │ │Booking│    │
│  └──────┘ └──────┘ └──────┘    │
│           H2 Database            │
└─────────────────────────────────┘
```

**拆分后微服务架构**:
```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │Class Service │  │Booking Service│
│              │  │              │  │              │
│  PostgreSQL  │  │  PostgreSQL  │  │  PostgreSQL  │
└──────────────┘  └──────────────┘  └──────────────┘
       │                  │                  │
       └──────────────────┼──────────────────┘
                          │
                   ┌──────────────┐
                   │  API Gateway │
                   │  (负载均衡)   │
                   └──────────────┘
```

**拆分难度**: ⚠️⚠️ 较高
- 需要重新设计服务边界
- 需要服务间通信机制(REST/gRPC)
- 需要分布式事务解决方案
- 需要服务注册与发现

#### 场景7: 多租户支持(SaaS化)
| 需求 | 当前架构 | 改造难度 |
|------|---------|---------|
| 租户隔离 | 无 | 🔴 高 - 需要重新设计数据模型 |
| 数据隔离 | 单一数据库 | 🔴 高 - 需要分库或分schema |
| 租户配置 | 无 | 🟡 中 - 添加配置表 |
| 计费系统 | 无 | 🟡 中 - 添加新模块 |

### 可扩展性评分总结

| 扩展场景 | 支持程度 | 改造成本 | 时间估算 |
|---------|---------|---------|---------|
| 小规模使用(< 1万用户) | ✅ 完全支持 | 低 | 无需改造 |
| 中等规模(1-10万用户) | ⚠️ 需要改造 | 中 | 1-2周 |
| 大规模(> 10万用户) | ❌ 需要重构 | 高 | 1-2个月 |
| 高并发秒杀 | ❌ 需要重构 | 高 | 2-3周 |
| 多地域部署 | ❌ 需要重构 | 高 | 1个月 |
| 微服务拆分 | ❌ 需要重新设计 | 很高 | 2-3个月 |
| 支付集成 | ✅ 容易添加 | 低 | 3-5天 |
| 通知系统 | ⚠️ 需要异步改造 | 中 | 1周 |

**总体评分: 6/10**
- ✅ 适合原型开发和小型项目
- ⚠️ 中型项目需要数据库和缓存改造
- ❌ 大型/高并发项目需要架构重构

---

## 改进建议

### 短期优化(保持单体架构)

#### 1. 切换到生产级数据库
**优先级**: 🔴 高

**改造步骤**:
```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: booking_user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # 生产环境禁止自动建表
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**为什么PostgreSQL**:
- ✅ 开源免费
- ✅ 支持复杂查询和JSON类型
- ✅ 成熟的扩展性(分区表、复制)
- ✅ 优秀的并发控制(MVCC)

#### 2. 引入Redis缓存
**优先级**: 🟡 中

**添加依赖**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**配置缓存**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**使用缓存**:
```java
@Service
public class ClassScheduleService {

    // 查询时使用缓存
    @Cacheable(value = "classes", key = "#id")
    public ClassResponse getClassById(Long id) { ... }

    // 更新时清除缓存
    @CacheEvict(value = "classes", key = "#id")
    public ClassResponse updateClass(Long id, ...) { ... }

    // 列表查询缓存
    @Cacheable(value = "availableClasses", unless = "#result.isEmpty()")
    public List<ClassResponse> getAvailableClasses() { ... }
}
```

**缓存策略**:
- 课程列表: 缓存10分钟
- 课程详情: 缓存30分钟
- 可用课程: 缓存5分钟
- 用户信息: 缓存1小时

#### 3. 改用构造函数注入
**优先级**: 🟢 低(但推荐)

```java
// 改造前
@Service
public class BookingService {
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ClassScheduleRepository classScheduleRepository;
    @Autowired private UserRepository userRepository;
}

// 改造后
@Service
@RequiredArgsConstructor  // Lombok自动生成构造函数
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final UserRepository userRepository;

    // Lombok会生成:
    // public BookingService(BookingRepository bookingRepository, ...) {
    //     this.bookingRepository = bookingRepository;
    //     ...
    // }
}
```

#### 4. 添加异步通知
**优先级**: 🟡 中

**配置异步**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**异步服务**:
```java
@Service
public class NotificationService {

    @Async
    public void sendBookingConfirmation(Booking booking) {
        // 发送邮件(不阻塞主流程)
        emailService.send(
            booking.getUser().getEmail(),
            "Booking Confirmed",
            generateEmailContent(booking)
        );
    }
}
```

**在业务中使用**:
```java
@Service
public class BookingService {
    @Autowired private NotificationService notificationService;

    @Transactional
    public BookingResponse createBooking(...) {
        Booking booking = bookingRepository.save(booking);

        // 异步发送通知(不等待)
        notificationService.sendBookingConfirmation(booking);

        return convertToResponse(booking);  // 立即返回
    }
}
```

#### 5. 优化事务粒度
**优先级**: 🟡 中

```java
@Service
public class BookingService {

    // 提取事务操作
    @Transactional
    public Booking createBookingTransaction(User user, ClassSchedule classSchedule, String notes) {
        // 只有数据库操作在事务中
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setClassSchedule(classSchedule);
        booking.setNotes(notes);

        classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
        classScheduleRepository.save(classSchedule);

        return bookingRepository.save(booking);
    }

    // 主方法 - 事务外处理
    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(...);

        ClassSchedule classSchedule = classScheduleRepository
            .findByIdWithLock(request.getClassScheduleId())
            .orElseThrow(...);

        // 业务验证(读操作，无需事务)
        validateBooking(classSchedule);

        // 事务操作
        Booking booking = createBookingTransaction(user, classSchedule, request.getNotes());

        // DTO转换(事务外)
        return convertToResponse(booking);
    }
}
```

### 中长期改进(架构升级)

#### 1. 引入领域驱动设计(DDD)
**优先级**: 🟡 中

**当前问题**:
```java
// 贫血模型 - Entity只是数据容器
@Entity
public class ClassSchedule {
    private Integer capacity;
    private Integer currentBookings;
    // ... getters/setters
}

// 业务逻辑在Service中
public class BookingService {
    public void book(...) {
        if (classSchedule.getCurrentBookings() >= classSchedule.getCapacity()) {
            throw new BookingException("Class is full");
        }
        classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
    }
}
```

**改进 - 充血模型**:
```java
// 领域对象封装业务逻辑
public class ClassSchedule {
    private Integer capacity;
    private Integer currentBookings;

    // 业务方法
    public BookingResult tryBook() {
        if (isFull()) {
            return BookingResult.failure("Class is full");
        }
        if (hasStarted()) {
            return BookingResult.failure("Class has started");
        }

        this.currentBookings++;
        // 发布领域事件
        DomainEvents.raise(new SeatBookedEvent(this.id));

        return BookingResult.success();
    }

    // 业务规则
    private boolean isFull() {
        return currentBookings >= capacity;
    }

    private boolean hasStarted() {
        return startTime.isBefore(LocalDateTime.now());
    }
}
```

**新架构层次**:
```
Controller
    ↓
Application Service (编排)
    ↓
Domain Service (领域服务)
    ↓
Domain Model (领域对象)
    ↓
Repository (持久化)
```

#### 2. 引入事件驱动架构
**优先级**: 🟡 中

**添加消息队列**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**发布事件**:
```java
@Service
public class BookingService {
    @Autowired private RabbitTemplate rabbitTemplate;

    @Transactional
    public BookingResponse createBooking(...) {
        Booking booking = bookingRepository.save(booking);

        // 发布事件
        BookingCreatedEvent event = new BookingCreatedEvent(
            booking.getId(),
            booking.getUser().getEmail(),
            booking.getClassSchedule().getName()
        );
        rabbitTemplate.convertAndSend("booking.exchange", "booking.created", event);

        return convertToResponse(booking);
    }
}
```

**消费事件**:
```java
@Component
public class NotificationEventHandler {

    @RabbitListener(queues = "notification.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        // 异步发送邮件
        emailService.sendBookingConfirmation(event);
    }
}

@Component
public class AnalyticsEventHandler {

    @RabbitListener(queues = "analytics.queue")
    public void handleBookingCreated(BookingCreatedEvent event) {
        // 异步更新统计数据
        analyticsService.recordBooking(event);
    }
}
```

**优势**:
- 解耦业务模块
- 支持异步处理
- 容易添加新功能(只需添加新的消费者)

#### 3. CQRS模式(读写分离)
**优先级**: 🟢 低

**问题**:
- 查询操作(GET)和命令操作(POST/PUT/DELETE)混在一起
- 查询需要复杂的JOIN，影响写入性能

**改进方案**:
```
写模型(Command):
  BookingCommandService → PostgreSQL Master → 发布事件

读模型(Query):
  事件消费者 → 更新 Redis/ElasticSearch
  BookingQueryService → Redis/ElasticSearch读取
```

**实现**:
```java
// 命令服务 - 处理写操作
@Service
public class BookingCommandService {
    public void createBooking(...) {
        // 写入主库
        bookingRepository.save(booking);

        // 发布事件
        eventPublisher.publish(new BookingCreatedEvent(...));
    }
}

// 查询服务 - 处理读操作
@Service
public class BookingQueryService {
    @Autowired private RedisTemplate redisTemplate;

    public List<BookingDTO> getMyBookings(Long userId) {
        // 从Redis读取
        return redisTemplate.opsForList()
            .range("user:bookings:" + userId, 0, -1);
    }
}

// 事件处理器 - 同步读模型
@Component
public class BookingReadModelSync {
    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        // 更新Redis
        BookingDTO dto = convertToDTO(event);
        redisTemplate.opsForList()
            .rightPush("user:bookings:" + event.getUserId(), dto);
    }
}
```

#### 4. 微服务拆分
**优先级**: 🔴 高(用于大规模系统)

**拆分方案**:

```
┌─────────────────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)  │
│        - 路由   - 限流   - 认证   - 监控         │
└─────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ User Service │ │Class Service │ │Booking Service│
├──────────────┤ ├──────────────┤ ├──────────────┤
│ - 用户管理   │ │ - 课程管理   │ │ - 预订管理   │
│ - 认证授权   │ │ - 教练管理   │ │ - 取消预订   │
│              │ │              │ │              │
│ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │
│ + Redis      │ │ + Redis      │ │ + Redis      │
└──────────────┘ └──────────────┘ └──────────────┘
        │               │               │
        └───────────────┼───────────────┘
                        ▼
              ┌──────────────────┐
              │  Message Queue   │
              │   (RabbitMQ)     │
              └──────────────────┘
```

**服务职责**:

| 服务 | 职责 | 技术栈 |
|-----|------|--------|
| User Service | 用户注册、登录、JWT签发 | Spring Boot + PostgreSQL + Redis |
| Class Service | 课程CRUD、教练管理 | Spring Boot + PostgreSQL + Redis |
| Booking Service | 预订管理、库存扣减 | Spring Boot + PostgreSQL + Redis |
| Notification Service | 邮件/短信通知 | Spring Boot + RabbitMQ |
| API Gateway | 路由、限流、认证 | Spring Cloud Gateway |

**服务间通信**:
- 同步: REST API / gRPC
- 异步: RabbitMQ消息队列

**分布式事务**:
```java
// 使用Saga模式
@Service
public class BookingOrchestrator {

    public void createBookingWithPayment(BookingRequest request) {
        try {
            // 1. 扣减库存
            classService.reserveSeat(request.getClassId());

            // 2. 创建订单
            Booking booking = bookingService.createBooking(request);

            // 3. 调用支付
            paymentService.charge(booking.getId(), request.getAmount());

        } catch (Exception e) {
            // 补偿操作
            classService.releaseSeat(request.getClassId());
            bookingService.cancelBooking(booking.getId());
        }
    }
}
```

#### 5. 添加可观测性
**优先级**: 🟡 中

**监控组件**:
```
日志: ELK Stack (Elasticsearch + Logstash + Kibana)
指标: Prometheus + Grafana
追踪: Jaeger / Zipkin
```

**Spring Boot Actuator**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**自定义指标**:
```java
@Service
public class BookingService {
    private final Counter bookingCounter;
    private final Timer bookingTimer;

    public BookingService(MeterRegistry registry) {
        this.bookingCounter = Counter.builder("bookings.created")
            .description("Total bookings created")
            .register(registry);

        this.bookingTimer = Timer.builder("bookings.duration")
            .description("Booking creation duration")
            .register(registry);
    }

    public BookingResponse createBooking(...) {
        return bookingTimer.record(() -> {
            BookingResponse response = doCreateBooking(...);
            bookingCounter.increment();
            return response;
        });
    }
}
```

**分布式追踪**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### 改进优先级路线图

#### Phase 1: 基础稳定(1-2周)
1. ✅ 切换到PostgreSQL
2. ✅ 改用构造函数注入
3. ✅ 添加基本监控(Actuator)

#### Phase 2: 性能优化(2-3周)
1. ✅ 引入Redis缓存
2. ✅ 添加异步通知
3. ✅ 优化事务粒度

#### Phase 3: 架构升级(1-2个月)
1. ✅ 引入消息队列
2. ✅ 实现事件驱动
3. ✅ 添加分布式追踪

#### Phase 4: 扩展能力(2-3个月)
1. ✅ CQRS读写分离
2. ✅ 微服务拆分
3. ✅ 完整可观测性

---

## 部署架构

### 当前部署(开发环境)

```
┌────────────────────────────┐
│   Localhost:8080           │
│                            │
│  ┌──────────────────────┐  │
│  │  Spring Boot App     │  │
│  │  (Embedded Tomcat)   │  │
│  └──────────────────────┘  │
│            │               │
│            ▼               │
│  ┌──────────────────────┐  │
│  │   H2 Database        │  │
│  │   (In-Memory)        │  │
│  └──────────────────────┘  │
└────────────────────────────┘
```

### 推荐的生产部署架构

#### 小规模部署(< 10万用户)

```
                 Internet
                    │
                    ▼
          ┌──────────────────┐
          │   Nginx (443)    │
          │  - SSL Termination│
          │  - Static Files   │
          └──────────────────┘
                    │
                    ▼
          ┌──────────────────┐
          │  Spring Boot     │
          │  (8080)          │
          │  + Redis (6379)  │
          └──────────────────┘
                    │
                    ▼
          ┌──────────────────┐
          │  PostgreSQL      │
          │  (5432)          │
          └──────────────────┘
```

**配置**:
- 服务器: 2核4G内存
- 数据库: 2核8G内存
- Redis: 1核2G内存

#### 中等规模部署(10-100万用户)

```
                 Internet
                    │
                    ▼
          ┌──────────────────┐
          │  Load Balancer   │
          │  (ALB / Nginx)   │
          └──────────────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
  ┌─────────┐ ┌─────────┐ ┌─────────┐
  │ App-01  │ │ App-02  │ │ App-03  │
  │ (8080)  │ │ (8080)  │ │ (8080)  │
  └─────────┘ └─────────┘ └─────────┘
        │           │           │
        └───────────┼───────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │ Redis    │  │PostgreSQL│  │RabbitMQ  │
  │ Cluster  │  │ Master   │  │          │
  │          │  │    +     │  │          │
  │          │  │ Replicas │  │          │
  └──────────┘  └──────────┘  └──────────┘
```

**配置**:
- 应用服务器: 3台 4核8G
- PostgreSQL主库: 8核16G
- PostgreSQL从库: 2台 4核8G
- Redis集群: 3节点 2核4G
- RabbitMQ: 2核4G

#### 大规模微服务部署(> 100万用户)

```
                      Internet
                         │
                         ▼
              ┌──────────────────────┐
              │   CDN (Static Files) │
              └──────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   API Gateway        │
              │   (Spring Cloud GW)  │
              └──────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ User Service │ │Class Service │ │Booking Service│
│  (3 replicas)│ │  (2 replicas)│ │  (3 replicas)│
└──────────────┘ └──────────────┘ └──────────────┘
        │                │                │
        ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │
│ + Redis      │ │ + Redis      │ │ + Redis      │
└──────────────┘ └──────────────┘ └──────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   Message Queue      │
              │   (Kafka Cluster)    │
              └──────────────────────┘
```

### Docker部署

**Dockerfile**:
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/class-booking-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**:
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      REDIS_HOST: redis
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: booking_db
      POSTGRES_USER: booking_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### Kubernetes部署

**deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: booking-app
  template:
    metadata:
      labels:
        app: booking-app
    spec:
      containers:
      - name: app
        image: booking-system:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: db.host
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: booking-service
spec:
  selector:
    app: booking-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## 总结

### 架构优势
1. ✅ **分层清晰**: 标准三层架构,易于理解和维护
2. ✅ **安全完善**: JWT + Spring Security提供全面防护
3. ✅ **并发安全**: 悲观锁机制防止超售
4. ✅ **数据管理**: Flyway版本控制,可追溯
5. ✅ **API友好**: RESTful设计 + Swagger文档

### 主要限制
1. ⚠️ **内存数据库**: H2无法用于生产环境
2. ⚠️ **单体架构**: 水平扩展受限
3. ⚠️ **缺少缓存**: 所有请求直接查库
4. ⚠️ **同步处理**: 无法支持高并发场景
5. ⚠️ **可观测性**: 缺少监控和追踪

### 适用场景
- ✅ 原型开发和概念验证
- ✅ 小型项目(< 1万用户)
- ✅ 学习Spring Boot生态
- ⚠️ 中型项目(需改造数据库和缓存)
- ❌ 大型/高并发项目(需架构重构)

### 改进路线
1. **立即**: 切换PostgreSQL + 构造函数注入
2. **短期**: 添加Redis缓存 + 异步通知
3. **中期**: 引入消息队列 + 事件驱动
4. **长期**: 微服务拆分 + CQRS模式

---

## 附录

### 相关文档
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Spring Security文档](https://spring.io/projects/spring-security)
- [PostgreSQL文档](https://www.postgresql.org/docs/)
- [Redis文档](https://redis.io/documentation)

### 版本历史
| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| 1.0.0 | 2026-02-01 | Claude | 初始版本 - 完整架构分析 |

### 联系方式
如有架构相关问题,请参考README.md中的联系方式。
