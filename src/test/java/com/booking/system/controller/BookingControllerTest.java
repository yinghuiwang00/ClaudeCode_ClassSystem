package com.booking.system.controller;

import com.booking.system.dto.request.BookingRequest;
import com.booking.system.dto.response.BookingResponse;
import com.booking.system.service.BookingService;
import com.booking.system.repository.UserRepository;
import com.booking.system.repository.BookingRepository;
import com.booking.system.repository.ClassScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookingController Unit Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private ClassScheduleRepository classScheduleRepository;

    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(1L);
        bookingRequest.setNotes("Looking forward to it!");

        bookingResponse = new BookingResponse();
        bookingResponse.setId(1L);
        bookingResponse.setUserId(1L);
        bookingResponse.setUserEmail("test@example.com");
        bookingResponse.setClassScheduleId(1L);
        bookingResponse.setClassName("Yoga Class");
        bookingResponse.setClassStartTime(LocalDateTime.now().plusDays(1));
        bookingResponse.setBookingStatus("CONFIRMED");
        bookingResponse.setBookingDate(LocalDateTime.now());
        bookingResponse.setNotes("Looking forward to it!");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should create booking successfully")
    void shouldCreateBookingSuccessfully() throws Exception {
        // Given
        when(bookingService.createBooking(eq("test@example.com"), any(BookingRequest.class)))
            .thenReturn(bookingResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.userEmail").value("test@example.com"))
            .andExpect(jsonPath("$.classScheduleId").value(1))
            .andExpect(jsonPath("$.className").value("Yoga Class"))
            .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));

        verify(bookingService).createBooking(eq("test@example.com"), any(BookingRequest.class));
    }

    @Test
    @DisplayName("Should return 403 when creating booking without authentication")
    void shouldReturn403WhenCreatingBookingWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isForbidden());

        verify(bookingService, never()).createBooking(anyString(), any(BookingRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() throws Exception {
        // Given
        doNothing().when(bookingService).cancelBooking(eq("test@example.com"), eq(1L));

        // When & Then
        mockMvc.perform(delete("/api/v1/bookings/1")
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(bookingService).cancelBooking(eq("test@example.com"), eq(1L));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should get current user's bookings")
    void shouldGetCurrentUserBookings() throws Exception {
        // Given
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getUserBookings("test@example.com")).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my-bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].userEmail").value("test@example.com"));

        verify(bookingService).getUserBookings("test@example.com");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should get active user bookings only")
    void shouldGetActiveUserBookingsOnly() throws Exception {
        // Given
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getActiveUserBookings("test@example.com")).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].bookingStatus").value("CONFIRMED"));

        verify(bookingService).getActiveUserBookings("test@example.com");
        verify(bookingService, never()).getUserBookings(anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should get booking by ID")
    void shouldGetBookingById() throws Exception {
        // Given
        when(bookingService.getBookingById(1L)).thenReturn(bookingResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.className").value("Yoga Class"));

        verify(bookingService).getBookingById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all bookings as admin")
    void shouldGetAllBookingsAsAdmin() throws Exception {
        // Given
        BookingResponse booking2 = new BookingResponse();
        booking2.setId(2L);
        booking2.setUserEmail("user2@example.com");

        List<BookingResponse> bookings = Arrays.asList(bookingResponse, booking2);
        when(bookingService.getAllBookings()).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));

        verify(bookingService).getAllBookings();
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Should return 500 when non-admin tries to get all bookings (AccessDeniedException)")
    void shouldReturn500WhenNonAdminTriesToGetAllBookings() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/bookings"))
            .andExpect(status().isForbidden());

        verify(bookingService, never()).getAllBookings();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Should get class bookings as instructor")
    void shouldGetClassBookingsAsInstructor() throws Exception {
        // Given
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getClassBookings(1L)).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/class/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].classScheduleId").value(1));

        verify(bookingService).getClassBookings(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get class bookings as admin")
    void shouldGetClassBookingsAsAdmin() throws Exception {
        // Given
        List<BookingResponse> bookings = Arrays.asList(bookingResponse);
        when(bookingService.getClassBookings(1L)).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/class/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(bookingService).getClassBookings(1L);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Should return 500 when user tries to get class bookings (AccessDeniedException)")
    void shouldReturn500WhenUserTriesToGetClassBookings() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/bookings/class/1"))
            .andExpect(status().isForbidden());

        verify(bookingService, never()).getClassBookings(anyLong());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should validate booking request")
    void shouldValidateBookingRequest() throws Exception {
        // Given
        BookingRequest invalidRequest = new BookingRequest();
        // Missing classScheduleId

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(anyString(), any(BookingRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should return 404 when booking not found")
    void shouldReturn404WhenBookingNotFound() throws Exception {
        // Given
        when(bookingService.getBookingById(999L))
            .thenThrow(new RuntimeException("Booking not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/999"))
            .andExpect(status().isInternalServerError());
    }
}
