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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private ClassSchedule testClassSchedule;
    private Booking testBooking;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole("ROLE_USER");
        testUser.setIsActive(true);

        // Setup test class schedule
        testClassSchedule = new ClassSchedule();
        testClassSchedule.setId(1L);
        testClassSchedule.setName("Yoga Class");
        testClassSchedule.setDescription("Relaxing yoga session");
        testClassSchedule.setStartTime(tomorrow);
        testClassSchedule.setEndTime(tomorrow.plusHours(1));
        testClassSchedule.setCapacity(20);
        testClassSchedule.setCurrentBookings(5);
        testClassSchedule.setLocation("Studio A");
        testClassSchedule.setStatus("SCHEDULED");
        testClassSchedule.setCreatedAt(now);

        // Setup test booking
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUser(testUser);
        testBooking.setClassSchedule(testClassSchedule);
        testBooking.setBookingStatus("CONFIRMED");
        testBooking.setBookingDate(now);
        testBooking.setNotes("Looking forward to it!");

        // Setup booking request
        bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(1L);
        bookingRequest.setNotes("Looking forward to it!");
    }

    @Test
    @DisplayName("Should create booking successfully")
    void shouldCreateBookingSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.existsByUserIdAndClassScheduleId(1L, 1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        BookingResponse response = bookingService.createBooking("test@example.com", bookingRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUserEmail()).isEqualTo("test@example.com");
        assertThat(response.getClassScheduleId()).isEqualTo(1L);
        assertThat(response.getClassName()).isEqualTo("Yoga Class");
        assertThat(response.getBookingStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getNotes()).isEqualTo("Looking forward to it!");

        verify(userRepository).findByEmail("test@example.com");
        verify(classScheduleRepository).findByIdWithLock(1L);
        verify(bookingRepository).existsByUserIdAndClassScheduleId(1L, 1L);
        verify(bookingRepository).save(any(Booking.class));
        verify(classScheduleRepository).save(argThat(cs -> cs.getCurrentBookings() == 6));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("nonexistent@example.com", bookingRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(classScheduleRepository, never()).findByIdWithLock(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when class not found")
    void shouldThrowExceptionWhenClassNotFound() {
        // Given
        bookingRequest.setClassScheduleId(999L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("test@example.com", bookingRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Class not found");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when class is not scheduled")
    void shouldThrowExceptionWhenClassIsNotScheduled() {
        // Given
        testClassSchedule.setStatus("CANCELLED");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("test@example.com", bookingRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("Class is not available for booking");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when class has already started")
    void shouldThrowExceptionWhenClassHasAlreadyStarted() {
        // Given
        testClassSchedule.setStartTime(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("test@example.com", bookingRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("Cannot book a class that has already started");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when class is full")
    void shouldThrowExceptionWhenClassIsFull() {
        // Given
        testClassSchedule.setCurrentBookings(20); // Full
        testClassSchedule.setCapacity(20);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("test@example.com", bookingRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("Class is full");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already booked the class")
    void shouldThrowExceptionWhenUserAlreadyBookedTheClass() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.existsByUserIdAndClassScheduleId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking("test@example.com", bookingRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("You have already booked this class");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        bookingService.cancelBooking("test@example.com", 1L);

        // Then
        verify(bookingRepository).save(argThat(b ->
            "CANCELLED".equals(b.getBookingStatus()) && b.getCancellationDate() != null
        ));
        verify(classScheduleRepository).save(argThat(cs -> cs.getCurrentBookings() == 4));
    }

    @Test
    @DisplayName("Should throw exception when canceling non-existent booking")
    void shouldThrowExceptionWhenCancelingNonExistentBooking() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking("test@example.com", 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Booking not found");
    }

    @Test
    @DisplayName("Should throw exception when canceling booking owned by another user")
    void shouldThrowExceptionWhenCancelingBookingOwnedByAnotherUser() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        testBooking.setUser(otherUser);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking("test@example.com", 1L))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("You can only cancel your own bookings");

        verify(bookingRepository, never()).save(any());
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when canceling already cancelled booking")
    void shouldThrowExceptionWhenCancelingAlreadyCancelledBooking() {
        // Given
        testBooking.setBookingStatus("CANCELLED");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking("test@example.com", 1L))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("Booking is already cancelled");

        verify(bookingRepository, never()).save(any());
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get booking by ID")
    void shouldGetBookingById() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When
        BookingResponse response = bookingService.getBookingById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getClassScheduleId()).isEqualTo(1L);

        verify(bookingRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when booking not found by ID")
    void shouldThrowExceptionWhenBookingNotFoundById() {
        // Given
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.getBookingById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Booking not found with id: 999");
    }

    @Test
    @DisplayName("Should get user bookings")
    void shouldGetUserBookings() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUser(testUser);
        booking2.setClassSchedule(testClassSchedule);
        booking2.setBookingStatus("CANCELLED");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByUserId(1L)).thenReturn(Arrays.asList(testBooking, booking2));

        // When
        List<BookingResponse> responses = bookingService.getUserBookings("test@example.com");

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getBookingStatus()).isEqualTo("CONFIRMED");
        assertThat(responses.get(1).getBookingStatus()).isEqualTo("CANCELLED");

        verify(userRepository).findByEmail("test@example.com");
        verify(bookingRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should get active user bookings only")
    void shouldGetActiveUserBookingsOnly() {
        // Given
        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(2L);
        cancelledBooking.setUser(testUser);
        cancelledBooking.setClassSchedule(testClassSchedule);
        cancelledBooking.setBookingStatus("CANCELLED");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByUserIdAndBookingStatus(1L, "CONFIRMED"))
            .thenReturn(Arrays.asList(testBooking));

        // When
        List<BookingResponse> responses = bookingService.getActiveUserBookings("test@example.com");

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBookingStatus()).isEqualTo("CONFIRMED");

        verify(bookingRepository).findByUserIdAndBookingStatus(1L, "CONFIRMED");
    }

    @Test
    @DisplayName("Should get all bookings")
    void shouldGetAllBookings() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUser(user2);
        booking2.setClassSchedule(testClassSchedule);
        booking2.setBookingStatus("CONFIRMED");

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(testBooking, booking2));

        // When
        List<BookingResponse> responses = bookingService.getAllBookings();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);

        verify(bookingRepository).findAll();
    }

    @Test
    @DisplayName("Should get class bookings")
    void shouldGetClassBookings() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUser(testUser);
        booking2.setClassSchedule(testClassSchedule);
        booking2.setBookingStatus("CONFIRMED");

        when(bookingRepository.findByClassScheduleId(1L))
            .thenReturn(Arrays.asList(testBooking, booking2));

        // When
        List<BookingResponse> responses = bookingService.getClassBookings(1L);

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getClassScheduleId()).isEqualTo(1L);
        assertThat(responses.get(1).getClassScheduleId()).isEqualTo(1L);

        verify(bookingRepository).findByClassScheduleId(1L);
    }

    @Test
    @DisplayName("Should not decrease bookings below zero on cancellation")
    void shouldNotDecreaseBookingsBelowZeroOnCancellation() {
        // Given
        testClassSchedule.setCurrentBookings(0);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        bookingService.cancelBooking("test@example.com", 1L);

        // Then
        verify(classScheduleRepository).save(argThat(cs -> cs.getCurrentBookings() == 0));
    }

    @Test
    @DisplayName("Should use pessimistic lock when creating booking")
    void shouldUsePessimisticLockWhenCreatingBooking() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.existsByUserIdAndClassScheduleId(1L, 1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        bookingService.createBooking("test@example.com", bookingRequest);

        // Then
        verify(classScheduleRepository).findByIdWithLock(1L);
    }

    @Test
    @DisplayName("Should use pessimistic lock when canceling booking")
    void shouldUsePessimisticLockWhenCancelingBooking() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(classScheduleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClassSchedule));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        bookingService.cancelBooking("test@example.com", 1L);

        // Then
        verify(classScheduleRepository).findByIdWithLock(1L);
    }

    @Test
    @DisplayName("Should include class name in booking response")
    void shouldIncludeClassNameInBookingResponse() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When
        BookingResponse response = bookingService.getBookingById(1L);

        // Then
        assertThat(response.getClassName()).isEqualTo("Yoga Class");
    }

    @Test
    @DisplayName("Should include class start time in booking response")
    void shouldIncludeClassStartTimeInBookingResponse() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When
        BookingResponse response = bookingService.getBookingById(1L);

        // Then
        assertThat(response.getClassStartTime()).isNotNull();
        assertThat(response.getClassStartTime()).isAfter(LocalDateTime.now());
    }
}
