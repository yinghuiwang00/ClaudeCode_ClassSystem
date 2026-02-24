package com.booking.system.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 * 所有领域事件应该继承此类
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    /**
     * 获取事件ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 获取事件发生时间
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    /**
     * 获取事件类型
     */
    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', occurredOn=%s}",
            eventType, eventId, occurredOn);
    }
}