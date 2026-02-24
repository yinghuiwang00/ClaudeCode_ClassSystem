package com.booking.system.domain.model.shared;

import com.booking.system.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;

/**
 * 容量值对象
 * 表示课程的可容纳人数，包含业务验证逻辑
 */
@Embeddable
public class Capacity extends ValueObject {

    @Column(nullable = false)
    private final Integer value;

    // JPA需要的无参构造函数
    protected Capacity() {
        this.value = null;
    }

    private Capacity(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Capacity cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (value > 1000) {
            throw new IllegalArgumentException("Capacity cannot exceed 1000");
        }
        this.value = value;
    }

    /**
     * 工厂方法：创建容量值对象
     */
    public static Capacity of(Integer value) {
        return new Capacity(value);
    }

    /**
     * 获取容量值
     */
    public Integer getValue() {
        return value;
    }

    /**
     * 检查是否有可用名额
     */
    public boolean hasAvailability(int currentBookings) {
        if (currentBookings < 0) {
            throw new IllegalArgumentException("Current bookings cannot be negative");
        }
        return currentBookings < value;
    }

    /**
     * 获取剩余名额
     */
    public int getRemaining(int currentBookings) {
        if (currentBookings < 0) {
            throw new IllegalArgumentException("Current bookings cannot be negative");
        }
        if (currentBookings > value) {
            throw new IllegalStateException("Current bookings exceed capacity");
        }
        return value - currentBookings;
    }

    /**
     * 检查是否可以增加预订
     */
    public boolean canBook(int currentBookings) {
        return hasAvailability(currentBookings);
    }

    /**
     * 检查是否可以取消预订
     */
    public boolean canCancel(int currentBookings) {
        return currentBookings > 0;
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return asList(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}