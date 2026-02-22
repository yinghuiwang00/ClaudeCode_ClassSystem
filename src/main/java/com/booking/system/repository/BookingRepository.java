package com.booking.system.repository;

import com.booking.system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByClassScheduleId(Long classScheduleId);

    boolean existsByUserIdAndClassScheduleId(Long userId, Long classScheduleId);

    List<Booking> findByUserIdAndBookingStatus(Long userId, String bookingStatus);

    List<Booking> findByClassScheduleIdAndBookingStatus(Long classScheduleId, String bookingStatus);
}
