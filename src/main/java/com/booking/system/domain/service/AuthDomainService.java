package com.booking.system.domain.service;

import com.booking.system.domain.model.user.User;
import com.booking.system.domain.repository.UserRepository;
import com.booking.system.domain.model.shared.Email;
import com.booking.system.domain.shared.DomainException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证领域服务
 * 处理用户注册、登录等核心领域逻辑
 */
@Service
@Transactional
public class AuthDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDomainService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册新用户
     */
    public User register(String username, String email, String password,
                        String firstName, String lastName) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Email already exists");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new DomainException("Username already exists");
        }

        // 创建Email值对象
        Email emailObj = Email.of(email);

        // 加密密码
        String passwordHash = passwordEncoder.encode(password);

        // 创建用户聚合根
        User user = User.create(
            username,
            emailObj,
            firstName,
            lastName,
            passwordHash,
            "ROLE_USER"
        );

        // 保存用户
        return userRepository.save(user);
    }

    /**
     * 验证用户凭据
     */
    public User authenticate(String email, String password) {
        // 查找用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new DomainException("Invalid email or password"));

        // 验证用户是否活跃
        if (!user.isActive()) {
            throw new DomainException("User account is inactive");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException("Invalid email or password");
        }

        return user;
    }

    /**
     * 检查邮箱是否已存在
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 检查用户名是否已存在
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}