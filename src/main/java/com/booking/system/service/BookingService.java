package com.booking.system.service;

import com.booking.system.dto.request.BookingRequest;
import com.booking.system.dto.response.BookingResponse;
import com.booking.system.entity.Booking;
import com.booking.system.entity.ClassSchedule;
import com.booking.system.entity.User;
import com.booking.system.exception.BookingException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.BookingRepository;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ClassSchedule classSchedule = classScheduleRepository
                .findByIdWithLock(request.getClassScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!"SCHEDULED".equals(classSchedule.getStatus())) {
            throw new BookingException("Class is not available for booking");
        }

        if (classSchedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingException("Cannot book a class that has already started or passed");
        }

        if (classSchedule.getCurrentBookings() >= classSchedule.getCapacity()) {
            throw new BookingException("Class is full");
        }

        if (bookingRepository.existsByUserIdAndClassScheduleId(user.getId(), classSchedule.getId())) {
            throw new BookingException("You have already booked this class");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setClassSchedule(classSchedule);
        booking.setBookingStatus("CONFIRMED");
        booking.setNotes(request.getNotes());

        classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
        classScheduleRepository.save(classSchedule);

        booking = bookingRepository.save(booking);

        return convertToResponse(booking);
    }

    @Transactional
    public void cancelBooking(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BookingException("You can only cancel your own bookings");
        }

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            throw new BookingException("Booking is already cancelled");
        }

        ClassSchedule classSchedule = classScheduleRepository.findByIdWithLock(booking.getClassSchedule().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        booking.setBookingStatus("CANCELLED");
        booking.setCancellationDate(LocalDateTime.now());
        bookingRepository.save(booking);

        classSchedule.setCurrentBookings(Math.max(0, classSchedule.getCurrentBookings() - 1));
        classScheduleRepository.save(classSchedule);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return convertToResponse(booking);
    }

    public List<BookingResponse> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getActiveUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepository.findByUserIdAndBookingStatus(user.getId(), "CONFIRMED").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getClassBookings(Long classScheduleId) {
        return bookingRepository.findByClassScheduleId(classScheduleId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse convertToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setUserEmail(booking.getUser().getEmail());
        response.setClassScheduleId(booking.getClassSchedule().getId());
        response.setClassName(booking.getClassSchedule().getName());
        response.setClassStartTime(booking.getClassSchedule().getStartTime());
        response.setBookingStatus(booking.getBookingStatus());
        response.setBookingDate(booking.getBookingDate());
        response.setCancellationDate(booking.getCancellationDate());
        response.setNotes(booking.getNotes());
        return response;
    }
}
