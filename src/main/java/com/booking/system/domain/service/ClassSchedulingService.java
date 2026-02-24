package com.booking.system.domain.service;

import com.booking.system.domain.model.classschedule.ClassSchedule;
import com.booking.system.domain.model.instructor.Instructor;
import com.booking.system.domain.model.shared.Capacity;
import com.booking.system.domain.model.shared.TimeRange;
import com.booking.system.domain.model.shared.Location;
import com.booking.system.domain.repository.ClassScheduleRepository;
import com.booking.system.domain.repository.InstructorRepository;
import com.booking.system.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 课程调度领域服务
 * 处理课程创建、更新、取消等核心领域逻辑
 */
@Service
@Transactional
public class ClassSchedulingService {

    private final ClassScheduleRepository classScheduleRepository;
    private final InstructorRepository instructorRepository;

    public ClassSchedulingService(ClassScheduleRepository classScheduleRepository,
                                  InstructorRepository instructorRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.instructorRepository = instructorRepository;
    }

    /**
     * 创建新课程
     */
    public ClassSchedule createClass(String name, String description, Long instructorId,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     Integer capacity, String location) {
        // 验证时间范围
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new DomainException("End time must be after start time");
        }

        // 查找讲师
        Instructor instructor = instructorRepository.findById(instructorId)
            .orElseThrow(() -> new DomainException("Instructor not found"));

        // 创建值对象
        TimeRange timeRange = TimeRange.of(startTime, endTime);
        Capacity capacityObj = Capacity.of(capacity);
        Location locationObj = Location.of(location);

        // 创建课程聚合根
        ClassSchedule classSchedule = ClassSchedule.create(
            name,
            description,
            instructor,
            timeRange,
            capacityObj,
            locationObj
        );

        // 保存课程
        return classScheduleRepository.save(classSchedule);
    }

    /**
     * 更新课程
     */
    public ClassSchedule updateClass(Long classId, String name, String description,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     Integer capacity, String location) {
        // 查找课程
        ClassSchedule classSchedule = classScheduleRepository.findById(classId)
            .orElseThrow(() -> new DomainException("Class not found"));

        // 验证课程是否可以更新（例如：课程未开始）
        if (classSchedule.hasStarted()) {
            throw new DomainException("Cannot update a class that has already started");
        }

        // 更新值对象
        TimeRange timeRange = TimeRange.of(startTime, endTime);
        Capacity capacityObj = Capacity.of(capacity);
        Location locationObj = Location.of(location);

        // 更新课程属性
        classSchedule.update(name, description, timeRange, capacityObj, locationObj);

        // 保存更新
        return classScheduleRepository.save(classSchedule);
    }

    /**
     * 取消课程
     */
    public void cancelClass(Long classId) {
        ClassSchedule classSchedule = classScheduleRepository.findById(classId)
            .orElseThrow(() -> new DomainException("Class not found"));

        classSchedule.cancel();

        classScheduleRepository.save(classSchedule);
    }

    /**
     * 预订课程
     */
    public void bookClass(Long classId) {
        // 使用悲观锁查找课程，防止并发预订
        ClassSchedule classSchedule = classScheduleRepository.findByIdWithLock(classId)
            .orElseThrow(() -> new DomainException("Class not found"));

        classSchedule.book();

        classScheduleRepository.save(classSchedule);
    }

    /**
     * 取消预订
     */
    public void cancelBooking(Long classId) {
        // 使用悲观锁查找课程，防止并发操作
        ClassSchedule classSchedule = classScheduleRepository.findByIdWithLock(classId)
            .orElseThrow(() -> new DomainException("Class not found"));

        classSchedule.cancelBooking();

        classScheduleRepository.save(classSchedule);
    }

    /**
     * 获取课程详情
     */
    public ClassSchedule getClassDetails(Long classId) {
        return classScheduleRepository.findById(classId)
            .orElseThrow(() -> new DomainException("Class not found"));
    }
}