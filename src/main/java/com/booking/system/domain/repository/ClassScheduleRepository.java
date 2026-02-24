package com.booking.system.domain.repository;

import com.booking.system.domain.model.classschedule.ClassSchedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ClassSchedule仓储接口（领域层定义）
 */
public interface ClassScheduleRepository {

    /**
     * 根据ID查找课程
     */
    Optional<ClassSchedule> findById(Long id);

    /**
     * 保存课程
     */
    ClassSchedule save(ClassSchedule classSchedule);

    /**
     * 删除课程
     */
    void delete(ClassSchedule classSchedule);

    /**
     * 根据状态查找课程
     */
    List<ClassSchedule> findByStatus(String status);

    /**
     * 根据讲师ID查找课程
     */
    List<ClassSchedule> findByInstructorId(Long instructorId);

    /**
     * 根据日期范围查找课程
     */
    List<ClassSchedule> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 根据状态查找即将开始的课程
     */
    List<ClassSchedule> findUpcomingClassesByStatus(String status, LocalDateTime now);

    /**
     * 使用悲观锁根据ID查找课程（用于并发控制）
     */
    Optional<ClassSchedule> findByIdWithLock(Long id);

    /**
     * 检查课程是否存在
     */
    boolean existsById(Long id);
}