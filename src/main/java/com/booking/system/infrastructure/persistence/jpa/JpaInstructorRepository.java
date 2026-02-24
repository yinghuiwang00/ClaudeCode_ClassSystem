package com.booking.system.infrastructure.persistence.jpa;

import com.booking.system.domain.model.instructor.Instructor;
import com.booking.system.domain.repository.InstructorRepository;
import com.booking.system.infrastructure.adapters.InstructorAdapter;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Instructor仓储的JPA实现
 * 实现领域层定义的InstructorRepository接口
 */
@Repository("domainInstructorRepository")
public class JpaInstructorRepository implements InstructorRepository {

    private final SimpleJpaRepository<com.booking.system.entity.Instructor, Long> jpaRepository;
    private final InstructorAdapter instructorAdapter;

    public JpaInstructorRepository(EntityManager entityManager, InstructorAdapter instructorAdapter) {
        this.jpaRepository = new SimpleJpaRepository<>(com.booking.system.entity.Instructor.class, entityManager);
        this.instructorAdapter = instructorAdapter;
    }

    @Override
    public Optional<Instructor> findById(Long id) {
        return jpaRepository.findById(id)
            .map(instructorAdapter::toDomain);
    }

    @Override
    public Optional<Instructor> findByUserId(Long userId) {
        return jpaRepository.findAll().stream()
            .filter(instructor -> {
                com.booking.system.entity.User user = instructor.getUser();
                return user != null && userId.equals(user.getId());
            })
            .findFirst()
            .map(instructorAdapter::toDomain);
    }

    @Override
    public Instructor save(Instructor instructor) {
        // 检查是新增还是更新
        if (instructor.getId() == null) {
            // 新增
            com.booking.system.entity.Instructor legacyInstructor = instructorAdapter.toLegacy(instructor);
            com.booking.system.entity.Instructor saved = jpaRepository.save(legacyInstructor);
            // 设置生成的ID和其他字段
            instructor.setId(saved.getId());
            instructor.setCreatedAt(saved.getCreatedAt());
            instructor.setUpdatedAt(saved.getUpdatedAt());
            return instructor;
        } else {
            // 更新：先查找现有实体，然后更新
            com.booking.system.entity.Instructor legacyInstructor = jpaRepository.findById(instructor.getId())
                .orElseThrow(() -> new RuntimeException("Instructor not found with id: " + instructor.getId()));
            instructorAdapter.updateLegacy(legacyInstructor, instructor);
            jpaRepository.save(legacyInstructor);
            return instructor;
        }
    }

    @Override
    public void delete(Instructor instructor) {
        jpaRepository.findById(instructor.getId())
            .ifPresent(jpaRepository::delete);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.findAll().stream()
            .anyMatch(instructor -> {
                com.booking.system.entity.User user = instructor.getUser();
                return user != null && userId.equals(user.getId());
            });
    }
}