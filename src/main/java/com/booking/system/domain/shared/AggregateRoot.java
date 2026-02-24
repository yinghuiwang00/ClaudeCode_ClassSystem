package com.booking.system.domain.shared;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根基类
 * 所有聚合根应该继承此类
 *
 * @param <ID> 聚合根ID类型
 */
@MappedSuperclass
@Getter
public abstract class AggregateRoot<ID> extends AbstractAggregateRoot<AggregateRoot<ID>> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private ID id;

    @Version
    private Long version;

    /**
     * 获取聚合根ID
     */
    public ID getId() {
        return id;
    }

    /**
     * 设置聚合根ID（仅限测试和特殊场景使用）
     */
    protected void setId(ID id) {
        this.id = id;
    }

    /**
     * 获取版本号（乐观锁）
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 注册领域事件
     * @param event 领域事件
     * @param <T> 事件类型
     * @return 注册的事件
     */
    protected <T> T registerEvent(T event) {
        return super.registerEvent(event);
    }

    /**
     * 清除所有领域事件
     */
    protected void clearEvents() {
        super.clearDomainEvents();
    }

    /**
     * 获取所有领域事件
     */
    public List<Object> getEvents() {
        return new ArrayList<>(super.domainEvents());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}