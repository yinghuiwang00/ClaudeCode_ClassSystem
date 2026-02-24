package com.booking.system.infrastructure.persistence.jpa;

import com.booking.system.domain.model.classschedule.ClassSchedule;
import com.booking.system.domain.repository.ClassScheduleRepository;
import com.booking.system.infrastructure.adapters.ClassScheduleAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ClassSchedule仓储的JPA实现
 * 实现领域层定义的ClassScheduleRepository接口
 */
@Repository("domainClassScheduleRepository")
public class JpaClassScheduleRepository implements ClassScheduleRepository {

    private final SimpleJpaRepository<com.booking.system.entity.ClassSchedule, Long> jpaRepository;
    private final ClassScheduleAdapter classScheduleAdapter;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaClassScheduleRepository(EntityManager entityManager, ClassScheduleAdapter classScheduleAdapter) {
        this.jpaRepository = new SimpleJpaRepository<>(com.booking.system.entity.ClassSchedule.class, entityManager);
        this.classScheduleAdapter = classScheduleAdapter;
    }

    @Override
    public Optional<ClassSchedule> findById(Long id) {
        return jpaRepository.findById(id)
            .map(classScheduleAdapter::toDomain);
    }

    @Override
    public ClassSchedule save(ClassSchedule classSchedule) {
        // 检查是新增还是更新
        if (classSchedule.getId() == null) {
            // 新增
            com.booking.system.entity.ClassSchedule legacyClassSchedule = classScheduleAdapter.toLegacy(classSchedule);
            com.booking.system.entity.ClassSchedule saved = jpaRepository.save(legacyClassSchedule);
            // 设置生成的ID和其他字段
            classSchedule.setId(saved.getId());
            classSchedule.setCreatedAt(saved.getCreatedAt());
            classSchedule.setUpdatedAt(saved.getUpdatedAt());
            classSchedule.setVersion(saved.getVersion());
            return classSchedule;
        } else {
            // 更新：先查找现有实体，然后更新
            com.booking.system.entity.ClassSchedule legacyClassSchedule = jpaRepository.findById(classSchedule.getId())
                .orElseThrow(() -> new RuntimeException("ClassSchedule not found with id: " + classSchedule.getId()));
            classScheduleAdapter.updateLegacy(legacyClassSchedule, classSchedule);
            jpaRepository.save(legacyClassSchedule);
            return classSchedule;
        }
    }

    @Override
    public void delete(ClassSchedule classSchedule) {
        jpaRepository.findById(classSchedule.getId())
            .ifPresent(jpaRepository::delete);
    }

    @Override
    public List<ClassSchedule> findByStatus(String status) {
        return jpaRepository.findAll().stream()
            .filter(classSchedule -> status.equals(classSchedule.getStatus()))
            .map(classScheduleAdapter::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> findByInstructorId(Long instructorId) {
        return jpaRepository.findAll().stream()
            .filter(classSchedule -> {
                com.booking.system.entity.Instructor instructor = classSchedule.getInstructor();
                return instructor != null && instructorId.equals(instructor.getId());
            })
            .map(classScheduleAdapter::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.findAll().stream()
            .filter(classSchedule -> {
                LocalDateTime startTime = classSchedule.getStartTime();
                return startTime != null && !startTime.isBefore(startDate) && !startTime.isAfter(endDate);
            })
            .map(classScheduleAdapter::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> findUpcomingClassesByStatus(String status, LocalDateTime now) {
        return jpaRepository.findAll().stream()
            .filter(classSchedule -> {
                LocalDateTime startTime = classSchedule.getStartTime();
                return status.equals(classSchedule.getStatus()) &&
                       startTime != null && startTime.isAfter(now);
            })
            .sorted((cs1, cs2) -> cs1.getStartTime().compareTo(cs2.getStartTime()))
            .map(classScheduleAdapter::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClassSchedule> findByIdWithLock(Long id) {
        // 使用悲观锁查找
        com.booking.system.entity.ClassSchedule legacyClassSchedule = entityManager.find(
            com.booking.system.entity.ClassSchedule.class,
            id,
            LockModeType.PESSIMISTIC_WRITE
        );
        return Optional.ofNullable(legacyClassSchedule)
            .map(classScheduleAdapter::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}