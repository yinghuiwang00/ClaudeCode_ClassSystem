package com.booking.system.integration;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.dto.response.UserResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("User Management Integration Tests")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String userToken;
    private String adminToken;
    private String instructorToken;
    private Long userId;
    private Long adminId;
    private Long instructorId;
    private Long anotherUserId;

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

        // Promote to instructor
        var instructorUser = userRepository.findByEmail("instructor@example.com").get();
        instructorUser.setRole("ROLE_INSTRUCTOR");
        userRepository.save(instructorUser);
        instructorId = instructorUser.getId();

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

        // Create another regular user for testing
        RegisterRequest anotherUserRequest = new RegisterRequest();
        anotherUserRequest.setUsername("anotheruser");
        anotherUserRequest.setEmail("another@example.com");
        anotherUserRequest.setPassword("password123");
        anotherUserRequest.setFirstName("Another");
        anotherUserRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherUserRequest)))
            .andExpect(status().isCreated());

        anotherUserId = userRepository.findByEmail("another@example.com").get().getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("User should get own profile")
    void userShouldGetOwnProfile() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value("regularuser"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin should get all users")
    void adminShouldGetAllUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4)) // user, admin, instructor, another user
            .andExpect(jsonPath("$[?(@.username == 'regularuser')]").exists())
            .andExpect(jsonPath("$[?(@.username == 'adminuser')]").exists())
            .andExpect(jsonPath("$[?(@.username == 'instructoruser')]").exists())
            .andExpect(jsonPath("$[?(@.username == 'anotheruser')]").exists());
    }

    @Test
    @DisplayName("Admin should get specific user by ID")
    void adminShouldGetSpecificUserById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value("regularuser"))
            .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @DisplayName("Regular user should not get all users")
    void regularUserShouldNotGetAllUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden()); // Access denied for non-admin
    }

    @Test
    @DisplayName("Regular user should not get specific user by ID")
    void regularUserShouldNotGetSpecificUserById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/" + anotherUserId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden()); // Access denied for non-admin
    }

    @Test
    @DisplayName("Instructor should not get all users")
    void instructorShouldNotGetAllUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isForbidden()); // Access denied for non-admin
    }

    @Test
    @DisplayName("Admin should get 404 for non-existent user")
    void adminShouldGet404ForNonExistentUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated user should not access user endpoints")
    void unauthenticatedUserShouldNotAccessUserEndpoints() throws Exception {
        // Test /api/v1/users/me
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isForbidden());

        // Test /api/v1/users
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());

        // Test /api/v1/users/{id}
        mockMvc.perform(get("/api/v1/users/" + userId))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User should get correct role in profile")
    void userShouldGetCorrectRoleInProfile() throws Exception {
        // Verify regular user role
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ROLE_USER"));

        // Verify admin role
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        // Verify instructor role
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + instructorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ROLE_INSTRUCTOR"));
    }
}