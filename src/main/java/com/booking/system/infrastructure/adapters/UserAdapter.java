package com.booking.system.infrastructure.adapters;

import com.booking.system.domain.model.user.User;
import com.booking.system.domain.model.shared.Email;

/**
 * User防腐层适配器
 * 负责新旧User对象之间的转换
 */
public class UserAdapter {

    /**
     * 旧实体 → 新聚合
     */
    public User toDomain(com.booking.system.entity.User legacyUser) {
        if (legacyUser == null) {
            return null;
        }

        return User.create(
            legacyUser.getUsername(),
            Email.of(legacyUser.getEmail()),
            legacyUser.getFirstName(),
            legacyUser.getLastName(),
            legacyUser.getPasswordHash(),
            legacyUser.getRole()
        );
    }

    /**
     * 新聚合 → 旧实体（用于兼容旧代码）
     */
    public com.booking.system.entity.User toLegacy(User domainUser) {
        if (domainUser == null) {
            return null;
        }

        com.booking.system.entity.User legacyUser = new com.booking.system.entity.User();
        legacyUser.setId(domainUser.getId());
        legacyUser.setUsername(domainUser.getUsername());
        legacyUser.setEmail(domainUser.getEmail().getValue());
        legacyUser.setFirstName(domainUser.getFirstName());
        legacyUser.setLastName(domainUser.getLastName());
        legacyUser.setPasswordHash(domainUser.getPasswordHash());
        legacyUser.setRole(domainUser.getRole());
        legacyUser.setCreatedAt(domainUser.getCreatedAt());
        legacyUser.setUpdatedAt(domainUser.getUpdatedAt());
        legacyUser.setIsActive(domainUser.isActive());

        return legacyUser;
    }

    /**
     * 更新旧实体（保持ID不变）
     */
    public void updateLegacy(com.booking.system.entity.User legacyUser, User domainUser) {
        if (legacyUser == null || domainUser == null) {
            return;
        }

        // ID保持不变
        legacyUser.setUsername(domainUser.getUsername());
        legacyUser.setEmail(domainUser.getEmail().getValue());
        legacyUser.setFirstName(domainUser.getFirstName());
        legacyUser.setLastName(domainUser.getLastName());
        legacyUser.setPasswordHash(domainUser.getPasswordHash());
        legacyUser.setRole(domainUser.getRole());
        legacyUser.setCreatedAt(domainUser.getCreatedAt());
        legacyUser.setUpdatedAt(domainUser.getUpdatedAt());
        legacyUser.setIsActive(domainUser.isActive());
    }
}