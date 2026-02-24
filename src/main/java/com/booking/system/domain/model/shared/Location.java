package com.booking.system.domain.model.shared;

import com.booking.system.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;

/**
 * 位置值对象
 * 表示课程地点，包含地点验证逻辑
 */
@Embeddable
public class Location extends ValueObject {

    @Column(length = 200)
    private final String value;

    // JPA需要的无参构造函数
    protected Location() {
        this.value = null;
    }

    private Location(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Location cannot exceed 200 characters");
        }
        this.value = value.trim();
    }

    /**
     * 工厂方法：创建位置值对象
     */
    public static Location of(String value) {
        return new Location(value);
    }

    /**
     * 获取位置值
     */
    public String getValue() {
        return value;
    }

    /**
     * 检查是否为虚拟位置（在线课程）
     */
    public boolean isVirtual() {
        String lower = value.toLowerCase();
        return lower.contains("online") ||
               lower.contains("virtual") ||
               lower.contains("zoom") ||
               lower.contains("webinar") ||
               lower.contains("meet.google") ||
               lower.contains("teams") ||
               lower.contains("skype");
    }

    /**
     * 检查是否为线下位置
     */
    public boolean isPhysical() {
        return !isVirtual();
    }

    /**
     * 获取位置的简短描述（前50个字符）
     */
    public String getShortDescription() {
        if (value.length() <= 50) {
            return value;
        }
        return value.substring(0, 47) + "...";
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return asList(value);
    }

    @Override
    public String toString() {
        return value;
    }
}