package com.booking.system.integration;

import com.booking.system.dto.request.BookingRequest;
import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.dto.response.BookingResponse;
import com.booking.system.dto.response.ClassResponse;
import com.booking.system.repository.BookingRepository;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Booking Integration Tests")
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private String authToken;
    private Long testClassId;
    private Long testUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login a test user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();

        // Save user ID
        testUserId = userRepository.findByEmail("test@example.com").get().getId();

        // Create a test class as admin
        // First, promote user to admin
        var user = userRepository.findById(testUserId).get();
        user.setRole("ROLE_ADMIN");
        userRepository.save(user);

        // Login as admin
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String adminResponse = adminResult.getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(adminResponse).get("token").asText();

        // Create a class
        String createClassJson = String.format(
            "{\"name\":\"Test Class\",\"description\":\"Test Description\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\",\"capacity\":10,\"location\":\"Studio A\"}",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1)
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        String classResponse = classResult.getResponse().getContentAsString();
        testClassId = objectMapper.readTree(classResponse).get("id").asLong();

        // Demote user back to regular user
        user.setRole("ROLE_USER");
        userRepository.save(user);

        // Login again as regular user
        MvcResult userResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String userResponse = userResult.getResponse().getContentAsString();
        authToken = objectMapper.readTree(userResponse).get("token").asText();
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        classScheduleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full booking workflow")
    void shouldCompleteFullBookingWorkflow() throws Exception {
        // Given
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(testClassId);
        bookingRequest.setNotes("Looking forward to this class!");

        // When - Create booking
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + authToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        // Then - Verify booking created
        String response = result.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(response).get("id").asLong();

        assertThat(bookingId).isNotNull();
        assertThat(objectMapper.readTree(response).get("bookingStatus").asText()).isEqualTo("CONFIRMED");

        // Verify class bookings increased
        ClassScheduleResponse classSchedule = objectMapper.readValue(
            mockMvc.perform(get("/api/v1/classes/" + testClassId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ClassScheduleResponse.class
        );
        assertThat(classSchedule.getCurrentBookings()).isEqualTo(1);

        // When - Get user bookings
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(bookingId));

        // When - Cancel booking
        mockMvc.perform(delete("/api/v1/bookings/" + bookingId)
                .header("Authorization", "Bearer " + authToken)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Then - Verify booking cancelled
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));

        // Verify class bookings decreased
        ClassScheduleResponse updatedClass = objectMapper.readValue(
            mockMvc.perform(get("/api/v1/classes/" + testClassId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ClassScheduleResponse.class
        );
        assertThat(updatedClass.getCurrentBookings()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prevent double booking")
    void shouldPreventDoubleBooking() throws Exception {
        // Given
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(testClassId);
        bookingRequest.setNotes("First booking");

        // When - First booking succeeds
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + authToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated());

        // Then - Second booking should fail
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + authToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You have already booked this class"));
    }

    @Test
    @DisplayName("Should require authentication for booking")
    void shouldRequireAuthenticationForBooking() throws Exception {
        // Given
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(testClassId);

        // When & Then - Spring Security returns 403 when no authentication is provided
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get only active bookings when requested")
    void shouldGetOnlyActiveBookingsWhenRequested() throws Exception {
        // Given
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(testClassId);
        bookingRequest.setNotes("Active booking");

        // Create two classes
        String createClassJson2 = String.format(
            "{\"name\":\"Test Class 2\",\"description\":\"Test Description 2\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\",\"capacity\":10,\"location\":\"Studio B\"}",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(1)
        );

        // Promote to admin to create second class
        var user = userRepository.findById(testUserId).get();
        user.setRole("ROLE_ADMIN");
        userRepository.save(user);

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andReturn();

        String adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();

        MvcResult classResult2 = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createClassJson2))
            .andReturn();

        Long classId2 = objectMapper.readTree(classResult2.getResponse().getContentAsString()).get("id").asLong();

        // Book first class
        bookingRequest.setClassScheduleId(testClassId);
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated());

        // Book second class
        bookingRequest.setClassScheduleId(classId2);
        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andReturn();

        Long bookingId2 = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asLong();

        // Cancel second booking
        mockMvc.perform(delete("/api/v1/bookings/" + bookingId2)
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Demote to regular user
        user.setRole("ROLE_USER");
        userRepository.save(user);

        // When & Then - All bookings should return 2
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        // When & Then - Active bookings should return 1
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + adminToken)
                .param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].bookingStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should get booking by ID")
    void shouldGetBookingById() throws Exception {
        // Given
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setClassScheduleId(testClassId);

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + authToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andReturn();

        Long bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookingId))
            .andExpect(jsonPath("$.userId").value(testUserId));
    }

    private static class ClassScheduleResponse {
        private Long id;
        private String name;
        private Integer capacity;
        private Integer currentBookings;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public Integer getCurrentBookings() { return currentBookings; }
        public void setCurrentBookings(Integer currentBookings) { this.currentBookings = currentBookings; }
    }
}
