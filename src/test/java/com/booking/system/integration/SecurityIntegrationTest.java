package com.booking.system.integration;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.entity.Instructor;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.InstructorRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Security and Role Permissions Integration Tests")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    private String userToken;
    private String adminToken;
    private String instructorToken;
    private Long userId;
    private Long adminId;
    private Long instructorId;
    private Long classId;
    private Long bookingId;

    @BeforeEach
    void setUp() throws Exception {
        // Create regular user
        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setUsername("regularuser");
        userRequest.setEmail("user@example.com");
        userRequest.setPassword("password123");
        userRequest.setFirstName("Regular");
        userRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
            .andExpect(status().isCreated());

        // Login as regular user
        LoginRequest userLogin = new LoginRequest();
        userLogin.setEmail("user@example.com");
        userLogin.setPassword("password123");

        MvcResult userResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("token").asText();
        userId = userRepository.findByEmail("user@example.com").get().getId();

        // Create admin user
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("adminuser");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password123");
        adminRequest.setFirstName("Admin");
        adminRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
            .andExpect(status().isCreated());

        // Promote to admin
        var adminUser = userRepository.findByEmail("admin@example.com").get();
        adminUser.setRole("ROLE_ADMIN");
        userRepository.save(adminUser);
        adminId = adminUser.getId();

        // Login as admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@example.com");
        adminLogin.setPassword("password123");

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
            .andExpect(status().isOk())
            .andReturn();

        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();

        // Create instructor user
        RegisterRequest instructorRequest = new RegisterRequest();
        instructorRequest.setUsername("instructoruser");
        instructorRequest.setEmail("instructor@example.com");
        instructorRequest.setPassword("password123");
        instructorRequest.setFirstName("Instructor");
        instructorRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instructorRequest)))
            .andExpect(status().isCreated());

        // Promote to instructor and create Instructor entity
        var instructorUser = userRepository.findByEmail("instructor@example.com").get();
        instructorUser.setRole("ROLE_INSTRUCTOR");
        userRepository.save(instructorUser);

        Instructor instructor = new Instructor();
        instructor.setUser(instructorUser);
        instructor.setBio("Test instructor");
        instructor.setSpecialization("Yoga");
        instructorRepository.save(instructor);
        instructorId = instructor.getId();

        // Login as instructor
        LoginRequest instructorLogin = new LoginRequest();
        instructorLogin.setEmail("instructor@example.com");
        instructorLogin.setPassword("password123");

        MvcResult instructorResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instructorLogin)))
            .andExpect(status().isOk())
            .andReturn();

        instructorToken = objectMapper.readTree(instructorResult.getResponse().getContentAsString()).get("token").asText();

        // Create a test class as admin
        String classJson = String.format(
            "{\"name\":\"Test Class\",\"description\":\"Test class for security tests\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":10,\"location\":\"Studio A\"}",
            instructorId,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1)
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(classJson))
            .andExpect(status().isCreated())
            .andReturn();

        classId = objectMapper.readTree(classResult.getResponse().getContentAsString()).get("id").asLong();

        // Create a booking as regular user
        String bookingJson = String.format("{\"classScheduleId\":%d,\"notes\":\"Test booking\"}", classId);

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + userToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
            .andExpect(status().isCreated())
            .andReturn();

        bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        classScheduleRepository.deleteAll();
        instructorRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== User Endpoints ====================

    @Test
    @DisplayName("USER role: Should access own profile but not admin endpoints")
    void userRoleShouldAccessOwnProfileButNotAdminEndpoints() throws Exception {
        // Should succeed: Get own profile
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        // Should fail: Get all users (admin only)
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());

        // Should fail: Get specific user by ID (admin only)
        mockMvc.perform(get("/api/v1/users/" + adminId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("INSTRUCTOR role: Should access own profile but not admin endpoints")
    void instructorRoleShouldAccessOwnProfileButNotAdminEndpoints() throws Exception {
        // Should succeed: Get own profile
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isOk());

        // Should fail: Get all users (admin only)
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isForbidden());

        // Should fail: Get specific user by ID (admin only)
        mockMvc.perform(get("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role: Should access all user endpoints")
    void adminRoleShouldAccessAllUserEndpoints() throws Exception {
        // Should succeed: Get own profile
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // Should succeed: Get all users
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // Should succeed: Get specific user by ID
        mockMvc.perform(get("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    // ==================== Class Endpoints ====================

    @Test
    @DisplayName("All roles: Should access public class endpoints")
    void allRolesShouldAccessPublicClassEndpoints() throws Exception {
        // GET /api/v1/classes - public access
        mockMvc.perform(get("/api/v1/classes"))
            .andExpect(status().isOk());

        // GET /api/v1/classes/{id} - public access
        mockMvc.perform(get("/api/v1/classes/" + classId))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Class creation: ADMIN and INSTRUCTOR allowed, USER denied")
    void classCreationPermissions() throws Exception {
        String newClassJson = String.format(
            "{\"name\":\"New Class\",\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":10,\"location\":\"Studio\"}",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(1)
        );

        // ADMIN should succeed
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isCreated());

        // INSTRUCTOR should succeed
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isCreated());

        // USER should fail
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + userToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Class update: ADMIN and INSTRUCTOR allowed, USER denied")
    void classUpdatePermissions() throws Exception {
        String updateJson = "{\"name\":\"Updated Name\"}";

        // ADMIN should succeed
        mockMvc.perform(put("/api/v1/classes/" + classId)
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk());

        // INSTRUCTOR should succeed (if they created the class or are assigned)
        mockMvc.perform(put("/api/v1/classes/" + classId)
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk());

        // USER should fail
        mockMvc.perform(put("/api/v1/classes/" + classId)
                .header("Authorization", "Bearer " + userToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Class cancellation: ADMIN and INSTRUCTOR allowed, USER denied")
    void classCancellationPermissions() throws Exception {
        // ADMIN should succeed
        mockMvc.perform(delete("/api/v1/classes/" + classId)
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Recreate class for next test
        String classJson = String.format(
            "{\"name\":\"Test Class 2\",\"description\":\"Another test class\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":10,\"location\":\"Studio A\"}",
            instructorId,
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(3).plusHours(1)
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(classJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long newClassId = objectMapper.readTree(classResult.getResponse().getContentAsString()).get("id").asLong();

        // INSTRUCTOR should succeed
        mockMvc.perform(delete("/api/v1/classes/" + newClassId)
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Recreate class again
        classResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(classJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long finalClassId = objectMapper.readTree(classResult.getResponse().getContentAsString()).get("id").asLong();

        // USER should fail
        mockMvc.perform(delete("/api/v1/classes/" + finalClassId)
                .header("Authorization", "Bearer " + userToken)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    // ==================== Booking Endpoints ====================

    @Test
    @DisplayName("Booking creation: All authenticated users allowed")
    void bookingCreationPermissions() throws Exception {
        // Create another class for booking test
        String classJson = String.format(
            "{\"name\":\"Booking Test Class\",\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":5,\"location\":\"Studio B\"}",
            LocalDateTime.now().plusDays(4),
            LocalDateTime.now().plusDays(4).plusHours(1)
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(classJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long testClassId = objectMapper.readTree(classResult.getResponse().getContentAsString()).get("id").asLong();
        String bookingJson = String.format("{\"classScheduleId\":%d}", testClassId);

        // USER should succeed
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + userToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
            .andExpect(status().isCreated());

        // INSTRUCTOR should succeed
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
            .andExpect(status().isCreated());

        // ADMIN should succeed
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("All bookings access: ADMIN only")
    void allBookingsAccessAdminOnly() throws Exception {
        // ADMIN should succeed
        mockMvc.perform(get("/api/v1/bookings")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // USER should fail
        mockMvc.perform(get("/api/v1/bookings")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());

        // INSTRUCTOR should fail
        mockMvc.perform(get("/api/v1/bookings")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Class bookings access: ADMIN and INSTRUCTOR allowed")
    void classBookingsAccessAdminAndInstructorAllowed() throws Exception {
        // ADMIN should succeed
        mockMvc.perform(get("/api/v1/bookings/class/" + classId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // INSTRUCTOR should succeed (if they teach the class)
        mockMvc.perform(get("/api/v1/bookings/class/" + classId)
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isOk());

        // USER should fail
        mockMvc.perform(get("/api/v1/bookings/class/" + classId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User's own bookings: All authenticated users can access")
    void ownBookingsAccessAllAuthenticatedUsers() throws Exception {
        // USER should succeed
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        // INSTRUCTOR should succeed
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isOk());

        // ADMIN should succeed
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }


    // ==================== Authentication Tests ====================

    @Test
    @DisplayName("Unauthenticated access to protected endpoints should fail")
    void unauthenticatedAccessShouldFail() throws Exception {
        // User endpoints
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/" + userId))
            .andExpect(status().isForbidden());

        // Class creation/update endpoints
        String classJson = "{\"name\":\"Test\",\"startTime\":\"2024-01-01T10:00:00\",\"endTime\":\"2024-01-01T11:00:00\",\"capacity\":10}";
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(classJson))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/classes/" + classId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/classes/" + classId)
                .with(csrf()))
            .andExpect(status().isForbidden());

        // Booking endpoints
        mockMvc.perform(post("/api/v1/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":1}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/bookings/my-bookings"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/bookings"))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/bookings/" + bookingId)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid JWT token should be rejected")
    void invalidJwtTokenShouldBeRejected() throws Exception {
        String invalidToken = "invalid.token.here";

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isForbidden()); // Spring Security rejects invalid tokens

        // Empty token
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer "))
            .andExpect(status().isForbidden());

        // Malformed header
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "NotBearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Missing Authorization header should be rejected")
    void missingAuthorizationHeaderShouldBeRejected() throws Exception {
        // Endpoint that requires authentication
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public endpoints should not require authentication")
    void publicEndpointsShouldNotRequireAuthentication() throws Exception {
        // Auth endpoints
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("new@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk());

        // Class listing endpoints
        mockMvc.perform(get("/api/v1/classes"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/classes/" + classId))
            .andExpect(status().isOk());
    }
}