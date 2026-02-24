package com.booking.system.infrastructure.persistence.jpa;

import com.booking.system.domain.model.user.User;
import com.booking.system.domain.repository.UserRepository;
import com.booking.system.infrastructure.adapters.UserAdapter;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * User仓储的JPA实现
 * 实现领域层定义的UserRepository接口
 */
@Repository("domainUserRepository")
public class JpaUserRepository implements UserRepository {

    private final SimpleJpaRepository<com.booking.system.entity.User, Long> jpaRepository;
    private final UserAdapter userAdapter;

    public JpaUserRepository(EntityManager entityManager, UserAdapter userAdapter) {
        this.jpaRepository = new SimpleJpaRepository<>(com.booking.system.entity.User.class, entityManager);
        this.userAdapter = userAdapter;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
            .map(userAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findAll().stream()
            .filter(user -> email.equals(user.getEmail()))
            .findFirst()
            .map(userAdapter::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findAll().stream()
            .filter(user -> username.equals(user.getUsername()))
            .findFirst()
            .map(userAdapter::toDomain);
    }

    @Override
    public User save(User user) {
        // 检查是新增还是更新
        if (user.getId() == null) {
            // 新增
            com.booking.system.entity.User legacyUser = userAdapter.toLegacy(user);
            com.booking.system.entity.User saved = jpaRepository.save(legacyUser);
            // 设置生成的ID
            user.setId(saved.getId());
            return user;
        } else {
            // 更新：先查找现有实体，然后更新
            com.booking.system.entity.User legacyUser = jpaRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + user.getId()));
            userAdapter.updateLegacy(legacyUser, user);
            jpaRepository.save(legacyUser);
            return user;
        }
    }

    @Override
    public void delete(User user) {
        jpaRepository.findById(user.getId())
            .ifPresent(jpaRepository::delete);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.findAll().stream()
            .anyMatch(user -> email.equals(user.getEmail()));
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.findAll().stream()
            .anyMatch(user -> username.equals(user.getUsername()));
    }
}