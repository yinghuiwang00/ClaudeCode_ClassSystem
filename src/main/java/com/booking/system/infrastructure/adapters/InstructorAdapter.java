package com.booking.system.infrastructure.adapters;

import com.booking.system.domain.model.instructor.Instructor;
import com.booking.system.domain.model.user.User;
import org.springframework.stereotype.Component;

/**
 * Instructor防腐层适配器
 * 负责新旧Instructor对象之间的转换
 */
@Component
public class InstructorAdapter {

    private final UserAdapter userAdapter;

    public InstructorAdapter(UserAdapter userAdapter) {
        this.userAdapter = userAdapter;
    }

    /**
     * 旧实体 → 新聚合
     */
    public Instructor toDomain(com.booking.system.entity.Instructor legacyInstructor) {
        if (legacyInstructor == null) {
            return null;
        }

        // 转换User
        User domainUser = userAdapter.toDomain(legacyInstructor.getUser());

        // 使用工厂方法从现有数据重建聚合根
        return Instructor.fromExisting(
            legacyInstructor.getId(),
            domainUser,
            legacyInstructor.getBio(),
            legacyInstructor.getSpecialization(),
            legacyInstructor.getCreatedAt(),
            legacyInstructor.getUpdatedAt(),
            null // 旧实体没有version字段
        );
    }

    /**
     * 新聚合 → 旧实体（用于兼容旧代码）
     */
    public com.booking.system.entity.Instructor toLegacy(Instructor domainInstructor) {
        if (domainInstructor == null) {
            return null;
        }

        com.booking.system.entity.Instructor legacyInstructor = new com.booking.system.entity.Instructor();
        legacyInstructor.setId(domainInstructor.getId());
        // 转换User
        com.booking.system.entity.User legacyUser = userAdapter.toLegacy(domainInstructor.getUser());
        legacyInstructor.setUser(legacyUser);
        legacyInstructor.setBio(domainInstructor.getBio());
        legacyInstructor.setSpecialization(domainInstructor.getSpecialization());
        legacyInstructor.setCreatedAt(domainInstructor.getCreatedAt());
        legacyInstructor.setUpdatedAt(domainInstructor.getUpdatedAt());
        // 注意：旧实体可能没有version字段，忽略

        return legacyInstructor;
    }

    /**
     * 更新旧实体（保持ID不变）
     */
    public void updateLegacy(com.booking.system.entity.Instructor legacyInstructor, Instructor domainInstructor) {
        if (legacyInstructor == null || domainInstructor == null) {
            return;
        }

        // ID保持不变
        // 注意：User关系通常不更新，因为是一对一固定关系
        // 如果需要更新User，这里可以处理，但通常讲师创建后不会更改关联的用户
        legacyInstructor.setBio(domainInstructor.getBio());
        legacyInstructor.setSpecialization(domainInstructor.getSpecialization());
        legacyInstructor.setCreatedAt(domainInstructor.getCreatedAt());
        legacyInstructor.setUpdatedAt(domainInstructor.getUpdatedAt());
        // 注意：version字段在旧实体中可能不存在，忽略
    }
}