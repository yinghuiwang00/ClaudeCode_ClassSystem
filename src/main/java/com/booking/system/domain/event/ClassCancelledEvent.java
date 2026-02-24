package com.booking.system.domain.event;

import java.time.LocalDateTime;

/**
 * 课程取消事件
 * 当课程被取消时发布
 */
public class ClassCancelledEvent extends DomainEvent {

    private final Long classScheduleId;
    private final String className;
    private final String reason;
    private final int currentBookings;

    public ClassCancelledEvent(Long classScheduleId, String className, int currentBookings) {
        this(classScheduleId, className, currentBookings, null);
    }

    public ClassCancelledEvent(Long classScheduleId, String className, int currentBookings, String reason) {
        super();
        this.classScheduleId = classScheduleId;
        this.className = className;
        this.currentBookings = currentBookings;
        this.reason = reason;
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
     * 获取取消原因（可选）
     */
    public String getReason() {
        return reason;
    }

    /**
     * 获取取消时的当前预订数
     */
    public int getCurrentBookings() {
        return currentBookings;
    }

    @Override
    public String toString() {
        return String.format("ClassCancelledEvent{classScheduleId=%d, className='%s', bookings=%d, reason='%s'}",
            classScheduleId, className, currentBookings, reason != null ? reason : "N/A");
    }
}