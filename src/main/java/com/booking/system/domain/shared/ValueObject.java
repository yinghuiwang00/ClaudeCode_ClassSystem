package com.booking.system.domain.shared;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 值对象基类
 * 所有值对象应该继承此类
 * 值对象是不可变的，基于其所有属性实现相等性
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ValueObject {

    /**
     * 获取用于相等性比较的属性值
     * 子类应该返回所有用于equals和hashCode的属性值
     */
    protected abstract List<Object> getEqualityComponents();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueObject that = (ValueObject) o;
        return getEqualityComponents().equals(that.getEqualityComponents());
    }

    @Override
    public int hashCode() {
        return getEqualityComponents().hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + getEqualityComponents();
    }

    /**
     * 辅助方法：将多个值转换为列表
     */
    protected static List<Object> asList(Object... values) {
        return Arrays.asList(values);
    }
}