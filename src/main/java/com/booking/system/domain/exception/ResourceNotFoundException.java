package com.booking.system.domain.exception;

import com.booking.system.domain.shared.DomainException;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with id: %s", resourceName, resourceId));
    }
}