package com.booking.system.integration;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.dto.response.AuthResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private AuthResponse authResponse;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full authentication workflow")
    void shouldCompleteFullAuthenticationWorkflow() throws Exception {
        // When - Register new user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        authResponse = objectMapper.readValue(registerResponse, AuthResponse.class);

        // Then - Verify registration response
        assertThat(authResponse.getToken()).isNotNull();
        assertThat(authResponse.getEmail()).isEqualTo("test@example.com");
        assertThat(authResponse.getUsername()).isEqualTo("testuser");
        assertThat(authResponse.getRole()).isEqualTo("ROLE_USER");

        // When - Login with registered credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        AuthResponse loginAuthResponse = objectMapper.readValue(loginResponse, AuthResponse.class);

        // Then - Verify login response
        assertThat(loginAuthResponse.getToken()).isNotNull();
        assertThat(loginAuthResponse.getEmail()).isEqualTo("test@example.com");
        assertThat(loginAuthResponse.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should prevent duplicate email registration")
    void shouldPreventDuplicateEmailRegistration() throws Exception {
        // Given - Register first user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("user1");
        registerRequest.setEmail("duplicate@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("First");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // When - Try to register with same email
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername("user2");
        duplicateRequest.setEmail("duplicate@example.com");
        duplicateRequest.setPassword("password456");
        duplicateRequest.setFirstName("Second");
        duplicateRequest.setLastName("User");

        // Then - Should fail
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("Should prevent duplicate username registration")
    void shouldPreventDuplicateUsernameRegistration() throws Exception {
        // Given - Register first user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("duplicateuser");
        registerRequest.setEmail("user1@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("First");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // When - Try to register with same username
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername("duplicateuser");
        duplicateRequest.setEmail("user2@example.com");
        duplicateRequest.setPassword("password456");
        duplicateRequest.setFirstName("Second");
        duplicateRequest.setLastName("User");

        // Then - Should fail
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void shouldFailLoginWithIncorrectPassword() throws Exception {
        // Given - Register a user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("correctpassword");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // When - Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpassword");

        // Then - Should fail
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate required fields during registration")
    void shouldValidateRequiredFieldsDuringRegistration() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate email format during registration")
    void shouldValidateEmailFormatDuringRegistration() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email-format");
        invalidRequest.setPassword("password123");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should store user with encrypted password")
    void shouldStoreUserWithEncryptedPassword() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("plaintextpassword");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // Then
        var user = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("plaintextpassword");
        assertThat(user.getPasswordHash()).startsWith("$2a$"); // BCrypt hash format
    }
}
