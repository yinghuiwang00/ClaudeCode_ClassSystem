package com.booking.system.domain.repository;

import com.booking.system.domain.model.instructor.Instructor;

import java.util.Optional;

/**
 * Instructor仓储接口（领域层定义）
 */
public interface InstructorRepository {

    /**
     * 根据ID查找讲师
     */
    Optional<Instructor> findById(Long id);

    /**
     * 根据用户ID查找讲师
     */
    Optional<Instructor> findByUserId(Long userId);

    /**
     * 保存讲师
     */
    Instructor save(Instructor instructor);

    /**
     * 删除讲师
     */
    void delete(Instructor instructor);

    /**
     * 检查讲师是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查用户是否已经是讲师
     */
    boolean existsByUserId(Long userId);
}