package com.booking.system.controller;

import com.booking.system.dto.request.BookingRequest;
import com.booking.system.dto.response.BookingResponse;
import com.booking.system.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking Management", description = "Booking management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    @Operation(summary = "Book a class")
    public ResponseEntity<BookingResponse> createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingRequest request) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.createBooking(userEmail, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<Void> cancelBooking(
            Authentication authentication,
            @PathVariable Long id) {
        String userEmail = authentication.getName();
        bookingService.cancelBooking(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Get current user's bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {

        String userEmail = authentication.getName();
        List<BookingResponse> bookings;

        if (activeOnly) {
            bookings = bookingService.getActiveUserBookings(userEmail);
        } else {
            bookings = bookingService.getUserBookings(userEmail);
        }

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (Admin only)")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/class/{classScheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Get bookings for a specific class (Admin/Instructor only)")
    public ResponseEntity<List<BookingResponse>> getClassBookings(@PathVariable Long classScheduleId) {
        List<BookingResponse> bookings = bookingService.getClassBookings(classScheduleId);
        return ResponseEntity.ok(bookings);
    }
}
