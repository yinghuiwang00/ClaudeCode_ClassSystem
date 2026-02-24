package com.booking.system.infrastructure.adapters;

import com.booking.system.domain.model.classschedule.ClassSchedule;
import com.booking.system.domain.model.shared.Capacity;
import com.booking.system.domain.model.shared.TimeRange;
import com.booking.system.domain.model.shared.Location;
import com.booking.system.domain.model.instructor.Instructor;
import org.springframework.stereotype.Component;

/**
 * ClassSchedule防腐层适配器
 * 负责新旧ClassSchedule对象之间的转换
 */
@Component
public class ClassScheduleAdapter {

    private final InstructorAdapter instructorAdapter;

    public ClassScheduleAdapter(InstructorAdapter instructorAdapter) {
        this.instructorAdapter = instructorAdapter;
    }

    /**
     * 旧实体 → 新聚合
     */
    public ClassSchedule toDomain(com.booking.system.entity.ClassSchedule legacyClassSchedule) {
        if (legacyClassSchedule == null) {
            return null;
        }

        // 转换值对象
        TimeRange timeRange = TimeRange.of(legacyClassSchedule.getStartTime(), legacyClassSchedule.getEndTime());
        Capacity capacity = Capacity.of(legacyClassSchedule.getCapacity());
        Location location = Location.of(legacyClassSchedule.getLocation());

        // 转换讲师
        Instructor domainInstructor = instructorAdapter.toDomain(legacyClassSchedule.getInstructor());

        // 使用工厂方法从现有数据重建聚合根
        return ClassSchedule.fromExisting(
            legacyClassSchedule.getId(),
            legacyClassSchedule.getName(),
            legacyClassSchedule.getDescription(),
            domainInstructor,
            timeRange,
            capacity,
            legacyClassSchedule.getCurrentBookings(),
            location,
            legacyClassSchedule.getStatus(),
            legacyClassSchedule.getCreatedAt(),
            legacyClassSchedule.getUpdatedAt(),
            legacyClassSchedule.getVersion()
        );
    }

    /**
     * 新聚合 → 旧实体（用于兼容旧代码）
     */
    public com.booking.system.entity.ClassSchedule toLegacy(ClassSchedule domainClassSchedule) {
        if (domainClassSchedule == null) {
            return null;
        }

        com.booking.system.entity.ClassSchedule legacyClassSchedule = new com.booking.system.entity.ClassSchedule();
        legacyClassSchedule.setId(domainClassSchedule.getId());
        legacyClassSchedule.setName(domainClassSchedule.getName());
        legacyClassSchedule.setDescription(domainClassSchedule.getDescription());
        // 转换讲师
        com.booking.system.entity.Instructor legacyInstructor = instructorAdapter.toLegacy(domainClassSchedule.getInstructor());
        legacyClassSchedule.setInstructor(legacyInstructor);
        // 从TimeRange值对象提取startTime和endTime
        if (domainClassSchedule.getTimeRange() != null) {
            legacyClassSchedule.setStartTime(domainClassSchedule.getTimeRange().getStartTime());
            legacyClassSchedule.setEndTime(domainClassSchedule.getTimeRange().getEndTime());
        }
        // 从Capacity值对象提取capacity值
        if (domainClassSchedule.getCapacity() != null) {
            legacyClassSchedule.setCapacity(domainClassSchedule.getCapacity().getValue());
        }
        legacyClassSchedule.setCurrentBookings(domainClassSchedule.getCurrentBookings());
        // 从Location值对象提取location值
        if (domainClassSchedule.getLocation() != null) {
            legacyClassSchedule.setLocation(domainClassSchedule.getLocation().getValue());
        }
        legacyClassSchedule.setStatus(domainClassSchedule.getStatus());
        legacyClassSchedule.setCreatedAt(domainClassSchedule.getCreatedAt());
        legacyClassSchedule.setUpdatedAt(domainClassSchedule.getUpdatedAt());
        legacyClassSchedule.setVersion(domainClassSchedule.getVersion());

        return legacyClassSchedule;
    }

    /**
     * 更新旧实体（保持ID不变）
     */
    public void updateLegacy(com.booking.system.entity.ClassSchedule legacyClassSchedule, ClassSchedule domainClassSchedule) {
        if (legacyClassSchedule == null || domainClassSchedule == null) {
            return;
        }

        // ID保持不变
        legacyClassSchedule.setName(domainClassSchedule.getName());
        legacyClassSchedule.setDescription(domainClassSchedule.getDescription());
        // 转换讲师
        com.booking.system.entity.Instructor legacyInstructor = instructorAdapter.toLegacy(domainClassSchedule.getInstructor());
        legacyClassSchedule.setInstructor(legacyInstructor);
        if (domainClassSchedule.getTimeRange() != null) {
            legacyClassSchedule.setStartTime(domainClassSchedule.getTimeRange().getStartTime());
            legacyClassSchedule.setEndTime(domainClassSchedule.getTimeRange().getEndTime());
        }
        if (domainClassSchedule.getCapacity() != null) {
            legacyClassSchedule.setCapacity(domainClassSchedule.getCapacity().getValue());
        }
        legacyClassSchedule.setCurrentBookings(domainClassSchedule.getCurrentBookings());
        if (domainClassSchedule.getLocation() != null) {
            legacyClassSchedule.setLocation(domainClassSchedule.getLocation().getValue());
        }
        legacyClassSchedule.setStatus(domainClassSchedule.getStatus());
        legacyClassSchedule.setCreatedAt(domainClassSchedule.getCreatedAt());
        legacyClassSchedule.setUpdatedAt(domainClassSchedule.getUpdatedAt());
        legacyClassSchedule.setVersion(domainClassSchedule.getVersion());
    }
}