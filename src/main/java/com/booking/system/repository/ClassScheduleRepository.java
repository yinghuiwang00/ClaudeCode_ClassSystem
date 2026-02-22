package com.booking.system.repository;

import com.booking.system.entity.ClassSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByStatus(String status);

    List<ClassSchedule> findByInstructorId(Long instructorId);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.startTime >= :startDate AND cs.startTime <= :endDate")
    List<ClassSchedule> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.status = :status AND cs.startTime >= :now ORDER BY cs.startTime")
    List<ClassSchedule> findUpcomingClassesByStatus(@Param("status") String status,
                                                     @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ClassSchedule cs WHERE cs.id = :id")
    Optional<ClassSchedule> findByIdWithLock(@Param("id") Long id);
}
