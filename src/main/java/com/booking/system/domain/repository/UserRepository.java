package com.booking.system.domain.repository;

import com.booking.system.domain.model.user.User;

import java.util.Optional;

/**
 * User仓储接口（领域层定义）
 */
public interface UserRepository {

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 删除用户
     */
    void delete(User user);

    /**
     * 检查邮箱是否已存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);
}