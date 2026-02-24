package com.booking.system.domain.model.instructor;

import com.booking.system.domain.shared.AggregateRoot;
import com.booking.system.domain.model.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Instructor聚合根
 * 封装讲师相关业务逻辑
 */
@Entity(name = "DomainInstructor")
@Table(name = "domain_instructors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instructor extends AggregateRoot<Long> {

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 100)
    private String specialization;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建讲师
     */
    public static Instructor create(User user, String bio, String specialization) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (bio == null) {
            throw new IllegalArgumentException("Bio cannot be null");
        }

        Instructor instructor = new Instructor();
        instructor.user = user;
        instructor.bio = bio;
        instructor.specialization = specialization != null ? specialization : "";
        instructor.createdAt = LocalDateTime.now();
        instructor.updatedAt = LocalDateTime.now();

        return instructor;
    }

    /**
     * 更新讲师信息
     */
    public void updateInfo(String bio, String specialization) {
        if (bio == null) {
            throw new IllegalArgumentException("Bio cannot be null");
        }

        this.bio = bio;
        this.specialization = specialization != null ? specialization : "";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取讲师全名
     */
    public String getFullName() {
        return user.getFullName();
    }

    /**
     * 获取讲师邮箱
     */
    public String getEmail() {
        return user.getEmail().getValue();
    }

    /**
     * 检查讲师是否活跃
     */
    public boolean isActive() {
        return user.isActive();
    }

    // ========== Public setters for infrastructure layer ==========
    // 这些setter仅供基础设施层（如JPA适配器）使用，领域逻辑不应直接调用

    public void setId(Long id) {
        super.setId(id);
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setVersion(Long version) {
        super.setVersion(version);
    }

    /**
     * 工厂方法：从现有数据重建讲师（用于仓储层加载）
     */
    public static Instructor fromExisting(Long id, User user, String bio, String specialization,
                                          LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        Instructor instructor = new Instructor();
        instructor.setId(id);
        instructor.user = user;
        instructor.bio = bio;
        instructor.specialization = specialization != null ? specialization : "";
        instructor.createdAt = createdAt;
        instructor.updatedAt = updatedAt;
        if (version != null) {
            instructor.setVersion(version);
        }
        return instructor;
    }

    @Override
    public String toString() {
        return String.format("Instructor{id=%d, user=%s, specialization='%s'}",
            getId(), user.getUsername(), specialization);
    }
}