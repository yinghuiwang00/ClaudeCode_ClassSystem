# Class Booking System - DDD重构方案

## 文档信息
- **项目名称**: Class Booking System
- **文档类型**: 领域驱动设计(DDD)重构计划
- **版本**: 1.0.0
- **创建日期**: 2026-02-23
- **目标受众**: 架构师、开发团队、技术负责人

---

## 目录
1. [引言与目标](#引言与目标)
2. [当前架构问题分析](#当前架构问题分析)
3. [DDD领域分析](#ddd领域分析)
4. [重构后的包结构](#重构后的包结构)
5. [关键代码重构示例](#关键代码重构示例)
6. [测试重构策略](#测试重构策略)
7. [数据库迁移策略](#数据库迁移策略)
8. [重构实施步骤](#重构实施步骤)
9. [注意事项与挑战](#注意事项与挑战)
10. [收益预期](#收益预期)
11. [工具与资源](#工具与资源)

---

## 引言与目标

### 重构背景
当前系统采用传统的三层架构（Controller-Service-Repository），虽然功能完善但在业务复杂性增长时面临以下挑战：
- 业务逻辑分散在Service层
- 领域模型贫血，缺乏行为
- 聚合边界不清晰
- 代码可维护性随功能增加而下降

### DDD重构目标
1. **领域模型丰富化**: 将业务逻辑内聚到领域模型中
2. **聚合边界清晰化**: 明确聚合根、实体和值对象的职责边界
3. **代码可维护性提升**: 通过限界上下文降低耦合度
4. **团队协作优化**: 统一语言，建立清晰的领域模型
5. **系统可扩展性**: 为未来功能扩展奠定架构基础

### 基本原则
- **渐进式重构**: 保持现有API兼容，逐步迁移
- **测试驱动**: 确保每一步重构都不破坏现有功能
- **领域优先**: 从最重要的业务领域开始重构

---

## 当前架构问题分析

### 1. 贫血模型问题
```java
// 当前：只有数据，没有行为
@Entity
public class ClassSchedule {
    private Integer currentBookings;
    private Integer capacity;
    // 没有业务逻辑方法，所有行为都在Service层
}
```

**问题**: 领域对象仅仅是数据容器，业务逻辑分散在Service层，导致：
- 业务规则难以发现和维护
- 领域知识分散
- 无法保证数据一致性

### 2. 业务逻辑分散
- **预订规则**: 散落在`BookingService.createBooking()`方法中
- **容量管理**: 在Service层手动操作`currentBookings`字段
- **状态验证**: 分散在多个Service方法中
- **并发控制**: 通过Repository层悲观锁实现，但与业务逻辑分离

### 3. 聚合边界不清晰
- `User`、`Instructor`、`ClassSchedule`、`Booking`之间关系复杂
- 没有明确的聚合根定义
- 跨实体修改缺乏事务一致性保证

### 4. 代码组织结构问题
```
当前结构:
src/main/java/com/booking/system/
├── controller/   # 表现层
├── service/      # 业务逻辑层（过度臃肿）
├── repository/   # 数据访问层
├── entity/       # 贫血的数据模型
└── dto/          # 数据传输对象
```

**问题**: 按技术层次划分，而不是按业务领域划分。

---

## DDD领域分析

### 领域划分（限界上下文）

| 限界上下文 | 核心职责 | 关键概念 |
|------------|----------|----------|
| **用户领域** | 用户管理、认证、角色权限 | User、Role、Authentication |
| **课程领域** | 课程创建、调度、容量管理 | ClassSchedule、Capacity、TimeRange |
| **预订领域** | 预订创建、取消、状态管理 | Booking、BookingStatus、Reservation |
| **支付领域**（未来扩展） | 支付处理、退款 | Payment、Invoice、Refund |

### 聚合根识别

1. **User聚合根**
   - 包含用户基本信息、认证凭证
   - 维护用户状态（激活/停用）
   - 管理用户角色权限
   - 边界：不直接包含预订信息，通过ID关联

2. **ClassSchedule聚合根**
   - 包含课程完整信息
   - 管理容量和当前预订数
   - 控制课程状态流转
   - 边界：包含课程相关值对象，不包含用户详情

3. **Booking聚合根**
   - 包含预订详细信息
   - 管理预订状态机
   - 维护与User和ClassSchedule的关联
   - 边界：引用聚合根ID，不包含完整对象

### 值对象设计

1. **`Email`**
   - 邮箱格式验证
   - 大小写规范化处理
   - 不可变性保证

2. **`TimeRange`**
   - 开始时间必须早于结束时间
   - 时长计算和验证
   - 时间冲突检测

3. **`Capacity`**
   - 容量限制逻辑（总容量、当前预订数）
   - 座位可用性检查
   - 预订数增减操作

4. **`BookingStatus`**
   - 状态机实现（CONFIRMED → CANCELLED）
   - 状态流转验证
   - 状态相关业务规则

### 领域服务识别

1. **`BookingDomainService`**
   - 处理跨聚合的预订业务逻辑
   - 协调User、ClassSchedule、Booking聚合
   - 处理重复预订检查等业务规则

2. **`ClassSchedulingService`**
   - 处理课程调度冲突检查
   - 协调多个ClassSchedule的时间安排
   - 处理讲师时间冲突

### 领域事件设计

| 事件名称 | 触发条件 | 包含数据 | 用途 |
|----------|----------|----------|------|
| `ClassBookedEvent` | 课程被成功预订 | classId, userId, bookingId | 发送确认邮件、更新统计 |
| `BookingCancelledEvent` | 预订被取消 | bookingId, classId, userId | 释放资源、通知相关人员 |
| `ClassRescheduledEvent` | 课程时间调整 | classId, oldTime, newTime | 通知已预订用户 |
| `ClassFullEvent` | 课程达到满员 | classId, capacity | 关闭预订、生成候补列表 |

---

## 重构后的包结构

```
src/main/java/com/booking/system/
├── application/                    # 应用层
│   ├── service/                   # 应用服务（用例实现）
│   │   ├── BookingApplicationService.java
│   │   ├── ClassApplicationService.java
│   │   └── UserApplicationService.java
│   └── dto/                       # 应用层DTO（请求/响应）
│       ├── request/
│       └── response/
├── domain/                        # 领域层（核心）
│   ├── model/                     # 领域模型
│   │   ├── user/                  # 用户子域
│   │   │   ├── User.java          # 聚合根
│   │   │   ├── Role.java          # 值对象
│   │   │   └── Email.java         # 值对象
│   │   ├── class/                 # 课程子域
│   │   │   ├── ClassSchedule.java # 聚合根
│   │   │   ├── Capacity.java      # 值对象
│   │   │   ├── TimeRange.java     # 值对象
│   │   │   └── ClassStatus.java   # 枚举
│   │   ├── booking/               # 预订子域
│   │   │   ├── Booking.java       # 聚合根
│   │   │   ├── BookingStatus.java # 值对象/状态机
│   │   │   └── Reservation.java   # 实体（可选）
│   │   └── shared/                # 共享内核
│   │       ├── AggregateRoot.java # 基类
│   │       ├── ValueObject.java   # 基类
│   │       └── DomainException.java
│   ├── service/                   # 领域服务
│   │   ├── BookingDomainService.java
│   │   └── ClassSchedulingService.java
│   ├── repository/                # 仓储接口（领域层定义）
│   │   ├── ClassScheduleRepository.java
│   │   ├── BookingRepository.java
│   │   └── UserRepository.java
│   ├── event/                     # 领域事件
│   │   ├── ClassBookedEvent.java
│   │   ├── BookingCancelledEvent.java
│   │   └── DomainEventPublisher.java
│   └── exception/                 # 领域异常
│       ├── DomainException.java
│       ├── ResourceNotFoundException.java
│       └── ConcurrencyException.java
├── infrastructure/                # 基础设施层
│   ├── persistence/               # 持久化实现
│   │   ├── jpa/                   # JPA实现
│   │   │   ├── JpaClassScheduleRepository.java
│   │   │   ├── JpaBookingRepository.java
│   │   │   └── JpaUserRepository.java
│   │   └── converter/             # JPA类型转换器
│   │       ├── EmailConverter.java
│   │       ├── TimeRangeConverter.java
│   │       └── CapacityConverter.java
│   ├── messaging/                 # 消息机制
│   │   ├── EventPublisherImpl.java
│   │   └── DomainEventStore.java
│   └── config/                    # 基础设施配置
│       ├── JpaConfig.java
│       └── EventConfig.java
└── interfaces/                    # 接口层
    ├── rest/                      # REST控制器
    │   ├── BookingController.java
    │   ├── ClassController.java
    │   ├── UserController.java
    │   └── AuthController.java
    ├── graphql/                   # GraphQL接口（未来）
    └── cli/                       # 命令行接口（未来）
```

### 各层职责说明

1. **领域层(Domain)**
   - 包含核心业务逻辑和规则
   - 完全独立于框架和技术细节
   - 通过接口定义与外部交互

2. **应用层(Application)**
   - 协调领域对象完成用例
   - 处理事务、安全、日志等横切关注点
   - 不包含业务逻辑，只做协调工作

3. **接口层(Interfaces)**
   - 处理外部请求（HTTP、CLI、消息等）
   - 输入验证、DTO转换
   - 返回适当的响应

4. **基础设施层(Infrastructure)**
   - 提供技术实现（数据库、消息队列、缓存等）
   - 实现领域层定义的接口
   - 处理技术细节和框架集成

---

## 关键代码重构示例

### 1. 领域模型重构（聚合根）

```java
// domain/model/class/ClassSchedule.java
package com.booking.system.domain.model.class;

import com.booking.system.domain.model.shared.*;
import com.booking.system.domain.exception.*;
import com.booking.system.domain.event.*;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_schedules")
public class ClassSchedule extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ClassName name;

    @Embedded
    private TimeRange scheduleTime;

    @Embedded
    private Capacity capacity;

    @Embedded
    private Location location;

    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Version
    private Long version;

    // 受保护的构造方法，通过工厂方法创建
    protected ClassSchedule() {}

    // 工厂方法
    public static ClassSchedule create(ClassName name, TimeRange scheduleTime,
                                      Capacity capacity, Location location,
                                      Instructor instructor) {
        validateCreationParameters(name, scheduleTime, capacity, location);

        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.name = name;
        classSchedule.scheduleTime = scheduleTime;
        classSchedule.capacity = capacity;
        classSchedule.location = location;
        classSchedule.instructor = instructor;
        classSchedule.status = ClassStatus.SCHEDULED;

        return classSchedule;
    }

    // 核心领域行为：预订课程
    public Booking book(User user, String notes) throws DomainException {
        validateAvailableForBooking();

        // 使用值对象的业务逻辑
        capacity.incrementBookings();

        // 创建预订
        Booking booking = Booking.create(this, user, notes);

        // 发布领域事件
        registerEvent(new ClassBookedEvent(this.id, user.getId(), booking.getId()));

        return booking;
    }

    // 核心领域行为：取消预订
    public void cancelBooking(Booking booking) {
        if (!booking.belongsToClass(this.id)) {
            throw new DomainException("Booking does not belong to this class");
        }

        capacity.decrementBookings();
        registerEvent(new BookingCancelledEvent(this.id, booking.getId()));
    }

    // 核心领域行为：重新安排时间
    public void reschedule(TimeRange newTimeRange) {
        if (status != ClassStatus.SCHEDULED) {
            throw new DomainException("Only scheduled classes can be rescheduled");
        }

        if (newTimeRange.isBefore(LocalDateTime.now())) {
            throw new DomainException("Cannot reschedule to past time");
        }

        TimeRange oldTimeRange = this.scheduleTime;
        this.scheduleTime = newTimeRange;

        registerEvent(new ClassRescheduledEvent(this.id, oldTimeRange, newTimeRange));
    }

    // 业务规则验证
    private void validateAvailableForBooking() {
        if (status != ClassStatus.SCHEDULED) {
            throw new DomainException("Class is not available for booking");
        }

        if (scheduleTime.isBefore(LocalDateTime.now())) {
            throw new DomainException("Class has already started");
        }

        if (capacity.isFull()) {
            throw new DomainException("Class is full");
        }
    }

    private static void validateCreationParameters(ClassName name, TimeRange scheduleTime,
                                                  Capacity capacity, Location location) {
        if (name == null || scheduleTime == null || capacity == null || location == null) {
            throw new IllegalArgumentException("All creation parameters are required");
        }

        if (scheduleTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create class in the past");
        }
    }

    // 业务语义查询方法
    public boolean isFull() {
        return capacity.isFull();
    }

    public boolean isAvailable() {
        return status == ClassStatus.SCHEDULED &&
               !scheduleTime.isBefore(LocalDateTime.now()) &&
               !capacity.isFull();
    }

    public int availableSeats() {
        return capacity.availableSeats();
    }

    // Getters（只有必要的getter）
    public Long getId() { return id; }
    public ClassName getName() { return name; }
    public TimeRange getScheduleTime() { return scheduleTime; }
    public Capacity getCapacity() { return capacity; }
    public ClassStatus getStatus() { return status; }
    public Instructor getInstructor() { return instructor; }
}
```

### 2. 值对象实现

```java
// domain/model/shared/Capacity.java
package com.booking.system.domain.model.shared;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Capacity {

    private int total;
    private int current;

    // 工厂方法
    public static Capacity of(int total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        return new Capacity(total, 0);
    }

    public static Capacity of(int total, int current) {
        if (total <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (current < 0 || current > total) {
            throw new IllegalArgumentException("Current bookings must be between 0 and total capacity");
        }
        return new Capacity(total, current);
    }

    // 领域行为
    public void incrementBookings() {
        if (isFull()) {
            throw new IllegalStateException("Class is full");
        }
        this.current++;
    }

    public void decrementBookings() {
        if (this.current > 0) {
            this.current--;
        }
    }

    public void incrementBookings(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        if (current + count > total) {
            throw new IllegalStateException("Not enough seats available");
        }
        this.current += count;
    }

    // 业务语义方法
    public boolean isFull() {
        return current >= total;
    }

    public int availableSeats() {
        return total - current;
    }

    public double occupancyRate() {
        return (double) current / total * 100;
    }

    // 值对象相等性比较
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Capacity capacity = (Capacity) o;
        return total == capacity.total && current == capacity.current;
    }

    @Override
    public int hashCode() {
        int result = total;
        result = 31 * result + current;
        return result;
    }

    @Override
    public String toString() {
        return String.format("Capacity{total=%d, current=%d, available=%d}",
                           total, current, availableSeats());
    }
}
```

### 3. 领域服务

```java
// domain/service/BookingDomainService.java
package com.booking.system.domain.service;

import com.booking.system.domain.model.booking.Booking;
import com.booking.system.domain.model.class.ClassSchedule;
import com.booking.system.domain.model.user.User;
import com.booking.system.domain.repository.ClassScheduleRepository;
import com.booking.system.domain.repository.BookingRepository;
import com.booking.system.domain.repository.UserRepository;
import com.booking.system.domain.exception.DomainException;
import com.booking.system.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingDomainService {

    private final ClassScheduleRepository classScheduleRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public BookingDomainService(ClassScheduleRepository classScheduleRepository,
                               BookingRepository bookingRepository,
                               UserRepository userRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Booking createBooking(Long classId, Long userId, String notes) {
        // 1. 加载聚合（使用悲观锁保持并发安全）
        ClassSchedule classSchedule = classScheduleRepository
            .findByIdWithLock(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 2. 执行业务规则检查
        validateBookingEligibility(classSchedule, user);

        // 3. 委托给聚合根执行核心业务逻辑
        Booking booking = classSchedule.book(user, notes);

        // 4. 保存聚合（自动处理领域事件）
        return bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // 验证用户权限
        if (!booking.isOwnedBy(userId)) {
            throw new DomainException("User is not authorized to cancel this booking");
        }

        // 加载课程（使用悲观锁）
        ClassSchedule classSchedule = classScheduleRepository
            .findByIdWithLock(booking.getClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // 委托给聚合根处理取消逻辑
        classSchedule.cancelBooking(booking);

        // 保存更新
        bookingRepository.save(booking);
    }

    private void validateBookingEligibility(ClassSchedule classSchedule, User user) {
        // 检查用户是否已预订该课程
        if (bookingRepository.existsByUserIdAndClassScheduleId(user.getId(), classSchedule.getId())) {
            throw new DomainException("User has already booked this class");
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new DomainException("User account is not active");
        }

        // 课程状态已在聚合根中验证
    }
}
```

### 4. 应用服务

```java
// application/service/BookingApplicationService.java
package com.booking.system.application.service;

import com.booking.system.domain.service.BookingDomainService;
import com.booking.system.application.dto.BookingRequest;
import com.booking.system.application.dto.BookingResponse;
import com.booking.system.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingApplicationService {

    private final BookingDomainService bookingDomainService;
    private final EventPublisher eventPublisher;

    @Transactional
    public BookingResponse bookClass(BookingRequest request) {
        log.info("Processing booking request: {}", request);

        try {
            // 1. 获取当前认证用户
            Long userId = getCurrentUserId();

            // 2. 调用领域服务执行业务逻辑
            var booking = bookingDomainService.createBooking(
                request.getClassId(),
                userId,
                request.getNotes()
            );

            // 3. 发布应用事件（异步处理）
            eventPublisher.publishAsync(new BookingCreatedEvent(
                booking.getId(),
                userId,
                request.getClassId(),
                LocalDateTime.now()
            ));

            log.info("Booking created successfully: {}", booking.getId());

            // 4. 转换为应用层DTO返回
            return BookingResponse.fromDomain(booking);

        } catch (Exception e) {
            log.error("Failed to create booking: {}", e.getMessage(), e);
            throw e; // 让全局异常处理器处理
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        log.info("Cancelling booking: {}", bookingId);

        try {
            Long userId = getCurrentUserId();
            bookingDomainService.cancelBooking(bookingId, userId);

            eventPublisher.publishAsync(new BookingCancelledEvent(
                bookingId,
                userId,
                LocalDateTime.now()
            ));

            log.info("Booking cancelled successfully: {}", bookingId);

        } catch (Exception e) {
            log.error("Failed to cancel booking {}: {}", bookingId, e.getMessage(), e);
            throw e;
        }
    }

    private Long getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        // 实际实现中可能需要从UserDetails获取
        return Long.parseLong(authentication.getName());
    }
}
```

### 5. REST控制器（接口层）

```java
// interfaces/rest/BookingController.java
package com.booking.system.interfaces.rest;

import com.booking.system.application.service.BookingApplicationService;
import com.booking.system.application.dto.BookingRequest;
import com.booking.system.application.dto.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingApplicationService bookingApplicationService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingApplicationService.bookClass(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingApplicationService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Get current user's bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        // 实现查询逻辑
        return ResponseEntity.ok(List.of());
    }
}
```

### 6. JPA仓储实现（基础设施层）

```java
// infrastructure/persistence/jpa/JpaClassScheduleRepository.java
package com.booking.system.infrastructure.persistence.jpa;

import com.booking.system.domain.model.class.ClassSchedule;
import com.booking.system.domain.repository.ClassScheduleRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaClassScheduleRepository extends
    JpaRepository<ClassSchedule, Long>,
    ClassScheduleRepository {

    // 实现领域层定义的接口方法
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.id = :id")
    Optional<ClassSchedule> findByIdWithLock(@Param("id") Long id);

    // Spring Data JPA会自动实现其他方法
}
```

---

## 测试重构策略

### 1. 领域模型单元测试

```java
// test/domain/model/ClassScheduleTest.java
package com.booking.system.domain.model;

import com.booking.system.domain.model.class.*;
import com.booking.system.domain.model.user.*;
import com.booking.system.domain.model.shared.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ClassScheduleTest {

    private ClassSchedule classSchedule;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建值对象
        var className = ClassName.of("Yoga Class");
        var startTime = LocalDateTime.now().plusDays(1);
        var endTime = startTime.plusHours(1);
        var timeRange = TimeRange.of(startTime, endTime);
        var capacity = Capacity.of(20);
        var location = Location.of("Studio A", "123 Main St");

        // 创建聚合根
        classSchedule = ClassSchedule.create(
            className, timeRange, capacity, location, null
        );

        // 创建测试用户
        testUser = User.create(
            "john.doe@example.com",
            "John",
            "Doe",
            "securePassword"
        );
    }

    @Test
    void should_create_class_with_valid_parameters() {
        // Then
        assertThat(classSchedule).isNotNull();
        assertThat(classSchedule.isAvailable()).isTrue();
        assertThat(classSchedule.isFull()).isFalse();
        assertThat(classSchedule.availableSeats()).isEqualTo(20);
    }

    @Test
    void should_throw_when_booking_full_class() {
        // Given
        // 填满课程
        for (int i = 0; i < 20; i++) {
            var tempUser = User.create(
                "user" + i + "@example.com",
                "User" + i,
                "Test",
                "password"
            );
            classSchedule.book(tempUser, "Test booking");
        }

        // When & Then
        assertThatThrownBy(() -> classSchedule.book(testUser, "notes"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class is full");
    }

    @Test
    void should_increment_capacity_when_booking() {
        // When
        var booking = classSchedule.book(testUser, "Looking forward!");

        // Then
        assertThat(classSchedule.availableSeats()).isEqualTo(19);
        assertThat(booking).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getUser()).isEqualTo(testUser);
    }

    @Test
    void should_decrement_capacity_when_cancelling() {
        // Given
        var booking = classSchedule.book(testUser, "notes");
        int initialSeats = classSchedule.availableSeats();

        // When
        classSchedule.cancelBooking(booking);

        // Then
        assertThat(classSchedule.availableSeats()).isEqualTo(initialSeats + 1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void should_not_allow_booking_past_class() {
        // Given
        var pastTime = TimeRange.of(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1).plusHours(1)
        );
        classSchedule.reschedule(pastTime);

        // When & Then
        assertThatThrownBy(() -> classSchedule.book(testUser, "notes"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class has already started");
    }

    @Test
    void should_allow_rescheduling_future_class() {
        // Given
        var newStartTime = LocalDateTime.now().plusDays(2);
        var newEndTime = newStartTime.plusHours(1);
        var newTimeRange = TimeRange.of(newStartTime, newEndTime);

        // When
        classSchedule.reschedule(newTimeRange);

        // Then
        assertThat(classSchedule.getScheduleTime()).isEqualTo(newTimeRange);
    }

    @Test
    void should_not_allow_rescheduling_to_past() {
        // Given
        var pastTime = TimeRange.of(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1).plusHours(1)
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.reschedule(pastTime))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot reschedule to past time");
    }

    @Test
    void should_publish_event_when_booking_created() {
        // Given
        var eventCollector = new TestDomainEventCollector();
        classSchedule.setDomainEventPublisher(eventCollector);

        // When
        var booking = classSchedule.book(testUser, "notes");

        // Then
        assertThat(eventCollector.getEvents()).hasSize(1);
        var event = eventCollector.getEvents().get(0);
        assertThat(event).isInstanceOf(ClassBookedEvent.class);
        assertThat(((ClassBookedEvent) event).getClassId()).isEqualTo(classSchedule.getId());
        assertThat(((ClassBookedEvent) event).getBookingId()).isEqualTo(booking.getId());
    }
}
```

### 2. 领域服务集成测试

```java
// test/domain/service/BookingDomainServiceTest.java
package com.booking.system.domain.service;

import com.booking.system.domain.model.class.*;
import com.booking.system.domain.model.user.*;
import com.booking.system.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingDomainServiceTest {

    @Autowired
    private BookingDomainService bookingDomainService;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void should_create_booking_successfully() {
        // Given
        var classSchedule = createTestClass();
        var user = createTestUser();

        // When
        var booking = bookingDomainService.createBooking(
            classSchedule.getId(),
            user.getId(),
            "Test notes"
        );

        // Then
        assertThat(booking).isNotNull();
        assertThat(booking.getClassId()).isEqualTo(classSchedule.getId());
        assertThat(booking.getUserId()).isEqualTo(user.getId());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void should_handle_concurrent_bookings_with_pessimistic_lock() throws Exception {
        // Given
        var classSchedule = createTestClassWithCapacity(3);
        var userIds = LongStream.range(1, 6)
            .mapToObj(this::createTestUserWithId)
            .map(User::getId)
            .toList();

        // 线程池执行并发请求
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        var results = userIds.stream()
            .map(userId -> executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 等待所有线程就绪
                    return bookingDomainService.createBooking(
                        classSchedule.getId(), userId, "Concurrent booking"
                    );
                } catch (Exception e) {
                    return e;
                }
            }))
            .toList();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: 只有3个成功（容量为3），2个失败
        var successfulBookings = results.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return e.getCause();
                }
            })
            .filter(Booking.class::isInstance)
            .count();

        var failures = results.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return e.getCause();
                }
            })
            .filter(Throwable.class::isInstance)
            .count();

        assertThat(successfulBookings).isEqualTo(3);
        assertThat(failures).isEqualTo(2);
        assertThat(bookingRepository.count()).isEqualTo(3);
    }

    @Test
    void should_not_allow_duplicate_booking() {
        // Given
        var classSchedule = createTestClass();
        var user = createTestUser();

        // 第一次预订成功
        bookingDomainService.createBooking(classSchedule.getId(), user.getId(), "First");

        // When & Then: 第二次预订失败
        assertThatThrownBy(() ->
            bookingDomainService.createBooking(classSchedule.getId(), user.getId(), "Second")
        ).isInstanceOf(DomainException.class)
         .hasMessageContaining("User has already booked this class");
    }

    @Test
    void should_cancel_booking_successfully() {
        // Given
        var classSchedule = createTestClass();
        var user = createTestUser();
        var booking = bookingDomainService.createBooking(
            classSchedule.getId(), user.getId(), "To cancel"
        );

        // When
        bookingDomainService.cancelBooking(booking.getId(), user.getId());

        // Then
        var cancelledBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        var updatedClass = classScheduleRepository.findById(classSchedule.getId()).orElseThrow();
        assertThat(updatedClass.availableSeats()).isEqualTo(updatedClass.getCapacity().getTotal());
    }

    // 辅助方法
    private ClassSchedule createTestClass() {
        var classSchedule = ClassSchedule.create(
            ClassName.of("Test Class"),
            TimeRange.of(LocalDateTime.now().plusDays(1), Duration.ofHours(1)),
            Capacity.of(10),
            Location.of("Test Studio", "Test Address"),
            null
        );
        return classScheduleRepository.save(classSchedule);
    }

    private ClassSchedule createTestClassWithCapacity(int capacity) {
        var classSchedule = ClassSchedule.create(
            ClassName.of("Test Class"),
            TimeRange.of(LocalDateTime.now().plusDays(1), Duration.ofHours(1)),
            Capacity.of(capacity),
            Location.of("Test Studio", "Test Address"),
            null
        );
        return classScheduleRepository.save(classSchedule);
    }

    private User createTestUser() {
        var user = User.create(
            "test" + System.currentTimeMillis() + "@example.com",
            "Test",
            "User",
            "password123"
        );
        return userRepository.save(user);
    }

    private User createTestUserWithId(Long id) {
        var user = User.create(
            "user" + id + "@example.com",
            "User" + id,
            "Test",
            "password123"
        );
        user.setId(id); // 测试环境特殊处理
        return userRepository.save(user);
    }
}
```

### 3. 应用服务测试

```java
// test/application/service/BookingApplicationServiceTest.java
package com.booking.system.application.service;

import com.booking.system.application.dto.BookingRequest;
import com.booking.system.application.dto.BookingResponse;
import com.booking.system.domain.service.BookingDomainService;
import com.booking.system.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingApplicationServiceTest {

    @Mock
    private BookingDomainService bookingDomainService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Captor
    private ArgumentCaptor<BookingCreatedEvent> eventCaptor;

    @InjectMocks
    private BookingApplicationService bookingApplicationService;

    @Test
    void should_process_booking_request_successfully() {
        // Given
        var request = new BookingRequest(1L, "Test notes");
        var domainBooking = mock(com.booking.system.domain.model.booking.Booking.class);

        when(domainBooking.getId()).thenReturn(100L);
        when(domainBooking.getClassId()).thenReturn(1L);
        when(domainBooking.getUserId()).thenReturn(500L);
        when(domainBooking.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("500");
        SecurityContextHolder.setContext(securityContext);

        when(bookingDomainService.createBooking(1L, 500L, "Test notes"))
            .thenReturn(domainBooking);

        // When
        BookingResponse response = bookingApplicationService.bookClass(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookingId()).isEqualTo(100L);
        assertThat(response.getClassId()).isEqualTo(1L);

        verify(bookingDomainService).createBooking(1L, 500L, "Test notes");
        verify(eventPublisher).publishAsync(eventCaptor.capture());

        var capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getBookingId()).isEqualTo(100L);
        assertThat(capturedEvent.getClassId()).isEqualTo(1L);
        assertThat(capturedEvent.getUserId()).isEqualTo(500L);
    }

    @Test
    void should_handle_booking_failure_gracefully() {
        // Given
        var request = new BookingRequest(1L, "Test notes");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("500");
        SecurityContextHolder.setContext(securityContext);

        when(bookingDomainService.createBooking(anyLong(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> bookingApplicationService.bookClass(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database error");

        verify(eventPublisher, never()).publishAsync(any());
    }
}
```

---

## 数据库迁移策略

### 1. 新增DDD相关表结构

```sql
-- V6__add_ddd_enhancements.sql

-- 1. 添加乐观锁版本字段
ALTER TABLE class_schedules
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. 领域事件存储表（可选，用于事件溯源）
CREATE TABLE domain_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSON NOT NULL,
    metadata JSON,
    occurred_at TIMESTAMP(6) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_published (published, occurred_at)
);

-- 3. 值对象支持表（如果需要单独存储）
CREATE TABLE class_capacities (
    class_schedule_id BIGINT PRIMARY KEY,
    total_capacity INT NOT NULL CHECK (total_capacity > 0),
    current_bookings INT NOT NULL DEFAULT 0 CHECK (current_bookings >= 0),
    CHECK (current_bookings <= total_capacity),
    FOREIGN KEY (class_schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE
);

-- 4. 快照表（用于事件溯源，可选）
CREATE TABLE aggregate_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    snapshot_data JSON NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_aggregate_version (aggregate_type, aggregate_id, version),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
);

-- 5. 预订状态枚举支持
ALTER TABLE bookings
    MODIFY COLUMN booking_status VARCHAR(20) NOT NULL
    CHECK (booking_status IN ('CONFIRMED', 'CANCELLED', 'WAITLISTED', 'COMPLETED'));

-- 6. 课程状态枚举支持
ALTER TABLE class_schedules
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
    CHECK (status IN ('DRAFT', 'SCHEDULED', 'CANCELLED', 'COMPLETED', 'FULL'));

-- 7. 添加索引优化查询性能
CREATE INDEX idx_class_schedule_status ON class_schedules(status, start_time);
CREATE INDEX idx_booking_user_class ON bookings(user_id, class_schedule_id, booking_status);
CREATE INDEX idx_booking_status_date ON bookings(booking_status, booking_date);
```

### 2. 数据迁移脚本

```sql
-- V7__migrate_existing_data.sql

-- 1. 初始化版本号
UPDATE class_schedules SET version = 0 WHERE version IS NULL;
UPDATE bookings SET version = 0 WHERE version IS NULL;
UPDATE users SET version = 0 WHERE version IS NULL;

-- 2. 迁移容量数据到新结构（如果需要）
INSERT INTO class_capacities (class_schedule_id, total_capacity, current_bookings)
SELECT id, capacity, current_bookings
FROM class_schedules
ON DUPLICATE KEY UPDATE
    total_capacity = VALUES(total_capacity),
    current_bookings = VALUES(current_bookings);

-- 3. 规范化状态值
UPDATE bookings
SET booking_status = UPPER(booking_status)
WHERE booking_status IS NOT NULL;

UPDATE class_schedules
SET status = UPPER(status)
WHERE status IS NOT NULL;

-- 4. 修复数据一致性
-- 确保current_bookings不超过capacity
UPDATE class_schedules cs
JOIN (
    SELECT class_schedule_id, COUNT(*) as actual_bookings
    FROM bookings
    WHERE booking_status = 'CONFIRMED'
    GROUP BY class_schedule_id
) b ON cs.id = b.class_schedule_id
SET cs.current_bookings = LEAST(cs.capacity, b.actual_bookings)
WHERE cs.current_bookings != b.actual_bookings;

-- 5. 更新状态为FULL的已满课程
UPDATE class_schedules
SET status = 'FULL'
WHERE current_bookings >= capacity
  AND status = 'SCHEDULED'
  AND start_time > NOW();
```

### 3. 回滚策略

```sql
-- V6__rollback_ddd_changes.sql
-- 仅在迁移失败时使用

-- 1. 删除新增表
DROP TABLE IF EXISTS domain_events;
DROP TABLE IF EXISTS class_capacities;
DROP TABLE IF EXISTS aggregate_snapshots;

-- 2. 删除新增列
ALTER TABLE class_schedules
    DROP COLUMN IF EXISTS version;

ALTER TABLE bookings
    DROP COLUMN IF EXISTS version;

ALTER TABLE users
    DROP COLUMN IF EXISTS version;

-- 3. 删除索引
DROP INDEX IF EXISTS idx_class_schedule_status ON class_schedules;
DROP INDEX IF EXISTS idx_booking_user_class ON bookings;
DROP INDEX IF EXISTS idx_booking_status_date ON bookings;

-- 4. 恢复原状态约束（如果需要）
ALTER TABLE bookings
    MODIFY COLUMN booking_status VARCHAR(20);

ALTER TABLE class_schedules
    MODIFY COLUMN status VARCHAR(20) DEFAULT 'SCHEDULED';
```

---

## 重构实施步骤

### 阶段1：基础设施准备（1-2周）

| 任务 | 描述 | 产出 | 负责人 |
|------|------|------|--------|
| 1.1 创建DDD基础组件 | 创建AggregateRoot、ValueObject等基类 | `domain/shared/`包结构 | 架构师 |
| 1.2 配置领域事件框架 | 实现事件发布/订阅机制 | `DomainEventPublisher`接口及实现 | 后端开发 |
| 1.3 建立测试基础设施 | 创建DDD测试基类和工具 | `TestDomainEventCollector`等 | 测试工程师 |
| 1.4 团队DDD培训 | 组织DDD概念和工作坊 | 培训材料、示例代码 | 技术负责人 |

### 阶段2：用户领域重构（1周）

| 任务 | 描述 | 产出 | 风险 |
|------|------|------|------|
| 2.1 重构User聚合根 | 将User实体转换为聚合根 | `domain/model/user/User.java` | 低 |
| 2.2 实现Email值对象 | 创建邮箱验证逻辑 | `domain/model/shared/Email.java` | 低 |
| 2.3 迁移认证逻辑 | 将认证逻辑移到领域层 | `AuthService`重构 | 中 |
| 2.4 更新用户相关测试 | 适配新的领域模型 | 用户领域测试通过 | 中 |

### 阶段3：课程领域重构（2周）

| 任务 | 描述 | 产出 | 风险 |
|------|------|------|------|
| 3.1 重构ClassSchedule聚合根 | 重构课程核心逻辑 | `domain/model/class/ClassSchedule.java` | 高 |
| 3.2 实现值对象 | 创建Capacity、TimeRange等 | 多个值对象实现 | 中 |
| 3.3 重构课程管理服务 | 将业务逻辑移到聚合根 | `ClassScheduleService`重构 | 高 |
| 3.4 并发控制集成 | 保持悲观锁机制 | 集成测试通过 | 高 |
| 3.5 更新课程相关测试 | 全面测试新模型 | 课程领域测试通过 | 中 |

### 阶段4：预订领域重构（2周）

| 任务 | 描述 | 产出 | 风险 |
|------|------|------|------|
| 4.1 重构Booking聚合根 | 实现预订状态机 | `domain/model/booking/Booking.java` | 高 |
| 4.2 实现BookingStatus值对象 | 状态流转逻辑 | `BookingStatus.java` | 中 |
| 4.3 重构预订领域服务 | 协调跨聚合业务 | `BookingDomainService.java` | 高 |
| 4.4 集成领域事件 | 预订相关事件发布 | `ClassBookedEvent`等 | 中 |
| 4.5 更新预订相关测试 | 包含并发测试 | 预订领域测试通过 | 高 |

### 阶段5：集成与测试（2周）

| 任务 | 描述 | 产出 | 验收标准 |
|------|------|------|----------|
| 5.1 更新所有控制器 | 适配新的应用服务 | 所有API端点可用 | API测试100%通过 |
| 5.2 重构测试套件 | 更新集成和单元测试 | 测试覆盖率达到85%+ | CI/CD流水线通过 |
| 5.3 性能测试 | 验证并发性能 | 性能测试报告 | 响应时间在SLA内 |
| 5.4 数据库迁移 | 执行DDD相关迁移 | 迁移脚本验证 | 数据一致性验证 |
| 5.5 文档更新 | 更新架构和API文档 | 完整的文档 | 团队评审通过 |

### 阶段6：生产部署与监控（1周）

| 任务 | 描述 | 产出 | 成功指标 |
|------|------|------|----------|
| 6.1 蓝绿部署 | 分批次部署到生产 | 部署检查清单 | 零停机部署 |
| 6.2 监控配置 | 添加DDD相关监控 | 监控仪表板 | 关键指标可观测 |
| 6.3 回滚计划 | 准备紧急回滚方案 | 回滚检查清单 | 30分钟内可回滚 |
| 6.4 生产验证 | 验证核心业务流程 | 生产验证报告 | 业务功能正常 |

---

## 注意事项与挑战

### 技术挑战

#### 1. JPA与DDD的阻抗不匹配
**问题**: JPA是为数据模型设计的，而DDD强调领域模型。
**解决方案**:
- 使用`@Embedded`注解实现值对象
- 自定义`AttributeConverter`处理复杂值对象
- 使用`@DomainEvents`和`@AfterDomainEventPublication`处理领域事件
- 避免在领域模型中使用JPA注解污染业务逻辑（可使用XML映射）

#### 2. 并发控制保持
**问题**: 需要保持现有的悲观锁机制。
**解决方案**:
- 在Repository接口中定义`findByIdWithLock()`方法
- 在领域服务中使用`@Transactional`和`@Lock`注解
- 聚合根内部使用`@Version`实现乐观锁
- 对于高频冲突场景，考虑使用数据库行锁

#### 3. 事务边界管理
**问题**: DDD推荐每个用例一个事务，但需要协调多个聚合。
**解决方案**:
- 应用服务控制事务边界
- 领域服务负责业务逻辑，不管理事务
- 使用领域事件实现最终一致性
- 对于强一致性要求，使用 Saga 模式

#### 4. 性能考虑
**问题**: 聚合设计可能影响加载性能。
**解决方案**:
- 避免过大聚合，按业务边界划分
- 使用懒加载和DTO投影
- 为频繁访问的聚合添加二级缓存
- 异步处理领域事件

### 团队挑战

#### 1. 知识转移
**挑战**: 团队需要学习DDD概念和实践。
**应对策略**:
- 组织系列培训和工作坊
- 建立代码评审和结对编程机制
- 创建DDD编码规范和实践指南
- 从简单领域开始，逐步复杂化

#### 2. 渐进式迁移
**挑战**: 需要保持系统持续可用。
**应对策略**:
- 使用防腐层隔离新旧代码
- 分阶段迁移，每个阶段都可独立部署
- 保持API向后兼容
- 建立全面的测试覆盖

#### 3. 文化转变
**挑战**: 从技术驱动转向领域驱动。
**应对策略**:
- 建立领域专家与开发团队的协作机制
- 使用统一语言（Ubiquitous Language）
- 定期进行领域建模工作坊
- 奖励领域知识的积累和分享

### 迁移风险与缓解

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 数据丢失或损坏 | 高 | 低 | 完整备份、迁移前验证、回滚计划 |
| 性能下降 | 中 | 中 | 性能基准测试、监控、容量规划 |
| API不兼容 | 高 | 低 | 版本化API、兼容性测试、客户端通知 |
| 团队生产力下降 | 中 | 中 | 充分培训、代码模板、结对编程 |
| 时间表延误 | 中 | 中 | 增量交付、定期检查点、风险缓冲 |

---

## 收益预期

### 短期收益（1-3个月）

#### 1. 代码质量提升
- **可读性**: 业务逻辑集中，代码更易理解（预计提升30%）
- **可维护性**: 清晰的聚合边界降低修改影响（预计提升25%）
- **测试性**: 领域模型易于单元测试（测试编写时间减少20%）

#### 2. 开发效率
- **功能开发**: 新功能开发速度提升15-20%
- **缺陷修复**: 定位和修复缺陷时间减少30%
- **知识传递**: 新成员上手时间缩短40%

#### 3. 业务价值
- **业务一致性**: 领域模型确保业务规则一致性
- **需求沟通**: 统一语言提高沟通效率
- **变更影响**: 更准确评估变更影响范围

### 中期收益（3-6个月）

#### 1. 架构稳定性
- **扩展性**: 易于添加新限界上下文
- **演化能力**: 支持业务模型持续演化
- **技术债务**: 系统性减少技术债务积累

#### 2. 团队能力
- **领域知识**: 团队深度理解业务领域
- **设计能力**: 提升软件设计能力
- **协作效率**: 跨职能协作更加顺畅

#### 3. 业务敏捷性
- **快速验证**: 支持业务假设快速验证
- **创新支持**: 为业务创新提供技术基础
- **市场响应**: 加快对市场变化的响应速度

### 长期收益（6-12个月）

#### 1. 系统健康度
- **缺陷密度**: 降低生产缺陷率30-40%
- **系统可用性**: 提高系统可用性至99.9%+
- **技术寿命**: 延长系统技术寿命2-3年

#### 2. 组织效能
- **团队扩展**: 支持团队规模扩展
- **产品组合**: 支持产品线扩展和组合
- **投资回报**: 显著提高技术投资回报率

#### 3. 战略价值
- **数字资产**: 领域模型成为重要数字资产
- **竞争优势**: 形成技术驱动的竞争优势
- **业务洞察**: 通过领域模型获得深度业务洞察

### 量化指标跟踪

| 指标类别 | 具体指标 | 当前值 | 目标值 | 测量频率 |
|----------|----------|--------|--------|----------|
| 代码质量 | 圈复杂度 | 待测量 | 降低20% | 每周 |
| 代码质量 | 重复代码率 | 待测量 | < 5% | 每周 |
| 开发效率 | 功能交付周期 | 待测量 | 缩短25% | 每月 |
| 开发效率 | 缺陷解决时间 | 待测量 | 缩短30% | 每月 |
| 系统质量 | 生产缺陷数 | 待测量 | 降低40% | 每月 |
| 系统质量 | API响应时间 | 待测量 | P95 < 200ms | 实时 |
| 团队效能 | 代码评审通过率 | 待测量 | > 95% | 每周 |
| 团队效能 | 知识共享评分 | 待测量 | 4.5/5.0 | 每季度 |
| 业务价值 | 功能使用率 | 待测量 | 提升15% | 每月 |
| 业务价值 | 用户满意度 | 待测量 | NPS > 40 | 每季度 |

---

## 工具与资源

### 开发工具

#### 1. 架构验证工具
- **ArchUnit**: 架构约束测试，确保DDD规则遵守
- **jQAssistant**: 代码结构分析和可视化
- **Structure101**: 软件结构分析和治理

#### 2. DDD支持库
- **jMolecules**: DDD注解库（@AggregateRoot, @ValueObject等）
- **Moduliths**: Spring Modulith，支持模块化开发
- **DDD Starter**: 社区驱动的DDD启动模板

#### 3. 测试工具
- **JUnit 5**: 单元测试框架
- **Testcontainers**: 集成测试数据库容器
- **ArchUnit**: 架构测试
- **Cucumber**: BDD测试框架

#### 4. 代码质量
- **SonarQube**: 代码质量监控
- **Checkstyle**: 代码规范检查
- **SpotBugs**: 静态代码分析

### 学习资源

#### 1. 书籍推荐
- 《实现领域驱动设计》- Vaughn Vernon
- 《领域驱动设计精粹》- Vaughn Vernon
- 《领域驱动设计》- Eric Evans（蓝皮书）
- 《整洁架构》- Robert C. Martin

#### 2. 在线资源
- **DDD社区**: dddcommunity.org
- **Martin Fowler的博客**: martinfowler.com
- **InfoQ DDD专栏**: infoq.com/ddd
- **Dev.to DDD标签**: dev.to/t/ddd

#### 3. 培训资源
- **Vaughn Vernon的课程**: vaughnvernon.co
- **领域驱动设计学堂**: learntoddd.com
- **Pluralsight DDD课程**: pluralsight.com

#### 4. 示例项目
- **DDD by Examples**: github.com/ddd-by-examples
- **IDDD Samples**: github.com/VaughnVernon/IDDD_Samples
- **Spring DDD示例**: github.com/spring-projects/spring-modulith

### 团队协作工具

#### 1. 建模工具
- **Miro/FigJam**: 在线协作白板
- **draw.io**: 图表绘制工具
- **PlantUML**: 文本化UML工具

#### 2. 文档工具
- **Confluence**: 团队知识库
- **GitBook**: API文档
- **Swagger/OpenAPI**: API规范

#### 3. 项目管理
- **Jira**: 敏捷项目管理
- **Miro**: 用户故事映射
- **Retro工具**: 回顾会议工具

### 监控与运维

#### 1. 应用性能监控
- **New Relic/DataDog**: APM工具
- **Prometheus+Grafana**: 指标监控
- **ELK Stack**: 日志分析

#### 2. 业务监控
- **自定义指标**: 领域事件监控
- **业务仪表板**: 关键业务指标
- **异常检测**: 业务规则异常监控

---

## 总结

本次DDD重构是一次系统性的架构升级，旨在解决当前系统在业务复杂性增长时面临的可维护性、可扩展性和团队协作问题。通过引入领域驱动设计，我们将：

1. **建立丰富的领域模型**，将业务逻辑内聚到领域对象中
2. **明确聚合边界**，确保数据一致性和事务完整性
3. **引入统一语言**，改善业务与技术的沟通效率
4. **建立清晰的架构分层**，分离关注点，降低耦合度

重构将采用渐进式策略，分阶段实施，每个阶段都确保系统可用性和功能完整性。我们预计在3-6个月内完成核心领域的重构，并在6-12个月内看到显著的长期收益。

成功的关键因素包括：
- **领导支持**: 获得管理层对重构的认可和支持
- **团队参与**: 全团队参与设计和实施过程
- **领域专家协作**: 紧密的业务领域专家合作
- **持续学习**: 建立持续学习和改进的文化
- **度量驱动**: 通过数据驱动决策和优化

通过这次重构，我们不仅将提升系统的技术质量，更将构建一个能够持续支持业务发展和创新的技术基础。

---

*文档版本: 1.0.0*
*最后更新: 2026-02-23*
*状态: 草案*
*下一步: 团队评审和计划细化*