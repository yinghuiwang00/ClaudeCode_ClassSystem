package com.booking.system.domain.model.user;

import com.booking.system.domain.model.shared.Email;
import com.booking.system.domain.shared.AggregateRoot;
import com.booking.system.domain.shared.DomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User聚合根
 * 封装用户相关业务逻辑
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AggregateRoot<Long> {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Embedded
    private Email email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 工厂方法：创建新用户
     */
    public static User create(String username, Email email, String firstName,
                             String lastName, String passwordHash, String role) {
        validateCreationParameters(username, email, firstName, lastName, passwordHash, role);

        User user = new User();
        user.username = username;
        user.email = email;
        user.firstName = firstName;
        user.lastName = lastName;
        user.passwordHash = passwordHash;
        user.role = role;
        user.isActive = true;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();

        return user;
    }

    /**
     * 更新用户基本信息
     */
    public void updateProfile(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new DomainException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new DomainException("Last name cannot be empty");
        }

        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新邮箱地址
     */
    public void updateEmail(Email newEmail) {
        if (newEmail == null) {
            throw new DomainException("Email cannot be null");
        }

        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新密码
     */
    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new DomainException("Password hash cannot be empty");
        }

        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 激活用户账户
     */
    public void activate() {
        if (Boolean.TRUE.equals(this.isActive)) {
            throw new DomainException("User is already active");
        }

        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用用户账户
     */
    public void deactivate() {
        if (Boolean.FALSE.equals(this.isActive)) {
            throw new DomainException("User is already inactive");
        }

        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查用户是否活跃
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 检查用户是否有指定角色
     */
    public boolean hasRole(String role) {
        return this.role.equals(role);
    }

    /**
     * 验证密码哈希
     */
    public boolean verifyPassword(String passwordHash) {
        return this.passwordHash.equals(passwordHash);
    }

    /**
     * 获取用户全名
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    private static void validateCreationParameters(String username, Email email,
                                                  String firstName, String lastName,
                                                  String passwordHash, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }

        // 验证角色是有效值
        if (!role.matches("ROLE_(USER|ADMIN|INSTRUCTOR)")) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}