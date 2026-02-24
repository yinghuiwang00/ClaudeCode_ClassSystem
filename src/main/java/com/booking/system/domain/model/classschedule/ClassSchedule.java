package com.booking.system.domain.model.classschedule;

import com.booking.system.domain.model.shared.Capacity;
import com.booking.system.domain.model.shared.TimeRange;
import com.booking.system.domain.model.shared.Location;
import com.booking.system.domain.shared.AggregateRoot;
import com.booking.system.domain.model.instructor.Instructor;
import com.booking.system.domain.shared.DomainException;
import com.booking.system.domain.event.ClassBookedEvent;
import com.booking.system.domain.event.ClassCancelledEvent;
import com.booking.system.domain.event.ClassCompletedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ClassSchedule聚合根
 * 封装课程调度相关业务逻辑
 */
@Entity(name = "DomainClassSchedule")
@Table(name = "domain_class_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassSchedule extends AggregateRoot<Long> {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Embedded
    private TimeRange timeRange;

    @Embedded
    private Capacity capacity;

    @Column(name = "current_bookings")
    private Integer currentBookings = 0;

    @Embedded
    private Location location;

    @Column(length = 20)
    private String status = "SCHEDULED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建新课程
     */
    public static ClassSchedule create(String name, String description, Instructor instructor,
                                       TimeRange timeRange, Capacity capacity, Location location) {
        validateCreationParameters(name, description, instructor, timeRange, capacity, location);

        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.name = name;
        classSchedule.description = description;
        classSchedule.instructor = instructor;
        classSchedule.timeRange = timeRange;
        classSchedule.capacity = capacity;
        classSchedule.location = location;
        classSchedule.currentBookings = 0;
        classSchedule.status = "SCHEDULED";
        classSchedule.createdAt = LocalDateTime.now();
        classSchedule.updatedAt = LocalDateTime.now();

        // 可以在这里发布领域事件：ClassCreatedEvent
        // classSchedule.registerEvent(new ClassCreatedEvent(classSchedule));

        return classSchedule;
    }

    /**
     * 更新课程基本信息
     */
    public void updateInfo(String name, String description, Location location) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Class name cannot be empty");
        }
        if (description == null) {
            throw new DomainException("Description cannot be null");
        }

        this.name = name;
        this.description = description;
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新课程时间
     */
    public void updateTime(TimeRange newTimeRange) {
        if (newTimeRange == null) {
            throw new DomainException("Time range cannot be null");
        }
        if (hasStarted()) {
            throw new DomainException("Cannot change time for a class that has already started");
        }

        this.timeRange = newTimeRange;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新课程容量
     */
    public void updateCapacity(Capacity newCapacity) {
        if (newCapacity == null) {
            throw new DomainException("Capacity cannot be null");
        }
        if (newCapacity.getValue() < currentBookings) {
            throw new DomainException("New capacity cannot be less than current bookings");
        }

        this.capacity = newCapacity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 预订课程
     */
    public void book() {
        validateBooking();

        currentBookings++;
        updatedAt = LocalDateTime.now();

        // 发布领域事件：ClassBookedEvent
        registerEvent(new ClassBookedEvent(getId(), name, currentBookings, capacity.getValue()));
    }

    /**
     * 取消预订
     */
    public void cancelBooking() {
        if (currentBookings <= 0) {
            throw new DomainException("No bookings to cancel");
        }
        if (hasEnded()) {
            throw new DomainException("Cannot cancel booking for a class that has ended");
        }

        currentBookings--;
        updatedAt = LocalDateTime.now();

        // 可以在这里发布领域事件：BookingCancelledEvent
        // registerEvent(new BookingCancelledEvent(this));
    }

    /**
     * 取消课程
     */
    public void cancel() {
        if (!"SCHEDULED".equals(status)) {
            throw new DomainException("Only scheduled classes can be cancelled");
        }
        if (hasStarted()) {
            throw new DomainException("Cannot cancel a class that has already started");
        }

        status = "CANCELLED";
        updatedAt = LocalDateTime.now();

        // 发布领域事件：ClassCancelledEvent
        registerEvent(new ClassCancelledEvent(getId(), name, currentBookings));
    }

    /**
     * 标记课程为已完成
     */
    public void complete() {
        if (!"SCHEDULED".equals(status)) {
            throw new DomainException("Only scheduled classes can be completed");
        }
        if (!hasEnded()) {
            throw new DomainException("Class must have ended to be marked as completed");
        }

        status = "COMPLETED";
        updatedAt = LocalDateTime.now();

        // 发布领域事件：ClassCompletedEvent
        registerEvent(new ClassCompletedEvent(getId(), name, currentBookings,
            timeRange.getStartTime(), timeRange.getEndTime()));
    }

    /**
     * 检查课程是否已满
     */
    public boolean isFull() {
        return currentBookings >= capacity.getValue();
    }

    /**
     * 获取剩余名额
     */
    public int getRemainingSeats() {
        return capacity.getRemaining(currentBookings);
    }

    /**
     * 检查课程是否已经开始
     */
    public boolean hasStarted() {
        return timeRange.hasStarted();
    }

    /**
     * 检查课程是否已经结束
     */
    public boolean hasEnded() {
        return timeRange.hasEnded();
    }

    /**
     * 检查课程是否正在进行中
     */
    public boolean isInProgress() {
        return timeRange.isInProgress();
    }

    /**
     * 验证预订
     */
    private void validateBooking() {
        if (!"SCHEDULED".equals(status)) {
            throw new DomainException("Cannot book a class that is not scheduled");
        }
        if (hasStarted()) {
            throw new DomainException("Cannot book a class that has already started");
        }
        if (isFull()) {
            throw new DomainException("Class is full");
        }
    }

    /**
     * 验证创建参数
     */
    private static void validateCreationParameters(String name, String description, Instructor instructor,
                                                   TimeRange timeRange, Capacity capacity, Location location) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Class name cannot be empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (instructor == null) {
            throw new IllegalArgumentException("Instructor cannot be null");
        }
        if (timeRange == null) {
            throw new IllegalArgumentException("Time range cannot be null");
        }
        if (capacity == null) {
            throw new IllegalArgumentException("Capacity cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
    }

    /**
     * 获取课程持续时间（分钟）
     */
    public long getDurationInMinutes() {
        return timeRange.getDurationInMinutes();
    }

    /**
     * 检查是否即将开始
     */
    public boolean isStartingSoon(int minutes) {
        return timeRange.isStartingSoon(minutes);
    }

    // ========== Public setters for infrastructure layer ==========
    // 这些setter仅供基础设施层（如JPA适配器）使用，领域逻辑不应直接调用

    public void setId(Long id) {
        super.setId(id);
    }

    public void setCurrentBookings(Integer currentBookings) {
        this.currentBookings = currentBookings;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setVersion(Long version) {
        super.setVersion(version);
    }

    /**
     * 更新课程所有信息（用于批量更新）
     */
    public void update(String name, String description, TimeRange timeRange,
                       Capacity capacity, Location location) {
        if (hasStarted()) {
            throw new DomainException("Cannot update a class that has already started");
        }

        if (name == null || name.isBlank()) {
            throw new DomainException("Class name cannot be empty");
        }
        if (description == null) {
            throw new DomainException("Description cannot be null");
        }
        if (timeRange == null) {
            throw new DomainException("Time range cannot be null");
        }
        if (capacity == null) {
            throw new DomainException("Capacity cannot be null");
        }
        if (location == null) {
            throw new DomainException("Location cannot be null");
        }

        this.name = name;
        this.description = description;
        this.timeRange = timeRange;
        this.capacity = capacity;
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 工厂方法：从现有数据重建课程（用于仓储层加载）
     */
    public static ClassSchedule fromExisting(Long id, String name, String description,
                                             Instructor instructor, TimeRange timeRange,
                                             Capacity capacity, Integer currentBookings,
                                             Location location, String status,
                                             LocalDateTime createdAt, LocalDateTime updatedAt,
                                             Long version) {
        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.setId(id);
        classSchedule.name = name;
        classSchedule.description = description;
        classSchedule.instructor = instructor;
        classSchedule.timeRange = timeRange;
        classSchedule.capacity = capacity;
        classSchedule.currentBookings = currentBookings;
        classSchedule.location = location;
        classSchedule.status = status;
        classSchedule.createdAt = createdAt;
        classSchedule.updatedAt = updatedAt;
        classSchedule.setVersion(version);
        return classSchedule;
    }

    @Override
    public String toString() {
        return String.format("ClassSchedule{id=%d, name='%s', status='%s', bookings=%d/%d}",
            getId(), name, status, currentBookings, capacity.getValue());
    }
}