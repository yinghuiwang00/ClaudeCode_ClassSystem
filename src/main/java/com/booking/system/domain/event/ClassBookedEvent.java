package com.booking.system.domain.event;

import java.time.LocalDateTime;

/**
 * 课程被预订事件
 * 当用户预订课程时发布
 */
public class ClassBookedEvent extends DomainEvent {

    private final Long classScheduleId;
    private final String className;
    private final int currentBookings;
    private final int capacity;

    public ClassBookedEvent(Long classScheduleId, String className, int currentBookings, int capacity) {
        super();
        this.classScheduleId = classScheduleId;
        this.className = className;
        this.currentBookings = currentBookings;
        this.capacity = capacity;
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
     * 获取当前预订数
     */
    public int getCurrentBookings() {
        return currentBookings;
    }

    /**
     * 获取课程容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 获取剩余名额
     */
    public int getRemainingSeats() {
        return capacity - currentBookings;
    }

    /**
     * 检查课程是否已满
     */
    public boolean isFull() {
        return currentBookings >= capacity;
    }

    @Override
    public String toString() {
        return String.format("ClassBookedEvent{classScheduleId=%d, className='%s', bookings=%d/%d, remaining=%d}",
            classScheduleId, className, currentBookings, capacity, getRemainingSeats());
    }
}