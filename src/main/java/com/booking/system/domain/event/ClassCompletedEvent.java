package com.booking.system.domain.event;

import java.time.LocalDateTime;

/**
 * 课程完成事件
 * 当课程标记为完成时发布
 */
public class ClassCompletedEvent extends DomainEvent {

    private final Long classScheduleId;
    private final String className;
    private final int finalBookings;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public ClassCompletedEvent(Long classScheduleId, String className, int finalBookings,
                               LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.classScheduleId = classScheduleId;
        this.className = className;
        this.finalBookings = finalBookings;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 获取课程ID
     */
    public Long getClassScheduleId() {
        return classScheduleId;
    }

    /**
     * 获取课程名称
     */
    public String getClassName() {
        return className;
    }

    /**
     * 获取最终预订数
     */
    public int getFinalBookings() {
        return finalBookings;
    }

    /**
     * 获取课程开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 获取课程结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 获取课程持续时间（分钟）
     */
    public long getDurationInMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    @Override
    public String toString() {
        return String.format("ClassCompletedEvent{classScheduleId=%d, className='%s', bookings=%d, duration=%d min}",
            classScheduleId, className, finalBookings, getDurationInMinutes());
    }
}