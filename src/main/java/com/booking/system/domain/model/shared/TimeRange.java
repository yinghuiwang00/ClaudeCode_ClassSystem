package com.booking.system.domain.model.shared;

import com.booking.system.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * 时间范围值对象
 * 表示课程的开始和结束时间，包含时间验证逻辑
 */
@Embeddable
public class TimeRange extends ValueObject {

    @Column(name = "start_time", nullable = false)
    private final LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private final LocalDateTime endTime;

    // JPA需要的无参构造函数
    protected TimeRange() {
        this.startTime = null;
        this.endTime = null;
    }

    private TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (Duration.between(startTime, endTime).toMinutes() < 30) {
            throw new IllegalArgumentException("Class duration must be at least 30 minutes");
        }
        if (Duration.between(startTime, endTime).toMinutes() > 8 * 60) {
            throw new IllegalArgumentException("Class duration cannot exceed 8 hours");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 工厂方法：创建时间范围值对象
     */
    public static TimeRange of(LocalDateTime startTime, LocalDateTime endTime) {
        return new TimeRange(startTime, endTime);
    }

    /**
     * 获取开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 获取结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 获取持续时间（分钟）
     */
    public long getDurationInMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 检查时间范围是否重叠
     */
    public boolean overlaps(TimeRange other) {
        return !(endTime.isBefore(other.startTime) || startTime.isAfter(other.endTime));
    }

    /**
     * 检查课程是否已经开始
     */
    public boolean hasStarted() {
        return LocalDateTime.now().isAfter(startTime) || LocalDateTime.now().isEqual(startTime);
    }

    /**
     * 检查课程是否已经结束
     */
    public boolean hasEnded() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * 检查课程是否正在进行中
     */
    public boolean isInProgress() {
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(startTime) || now.isEqual(startTime)) && now.isBefore(endTime);
    }

    /**
     * 检查课程是否即将开始（在指定分钟内）
     */
    public boolean isStartingSoon(int minutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startThreshold = startTime.minusMinutes(minutes);
        return (now.isAfter(startThreshold) || now.isEqual(startThreshold)) && now.isBefore(startTime);
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return asList(startTime, endTime);
    }

    @Override
    public String toString() {
        return startTime + " - " + endTime;
    }
}