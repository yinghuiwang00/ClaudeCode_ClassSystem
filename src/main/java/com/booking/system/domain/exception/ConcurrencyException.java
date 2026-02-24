package com.booking.system.domain.exception;

import com.booking.system.domain.shared.DomainException;

/**
 * 并发冲突异常
 * 当乐观锁版本冲突时抛出
 */
public class ConcurrencyException extends DomainException {

    public ConcurrencyException(String message) {
        super("CONCURRENCY_CONFLICT", message);
    }

    public ConcurrencyException(String resourceName, Object resourceId, Long expectedVersion, Long actualVersion) {
        super("CONCURRENCY_CONFLICT",
            String.format("Concurrency conflict for %s with id: %s. Expected version: %d, Actual version: %d",
                resourceName, resourceId, expectedVersion, actualVersion));
    }
}