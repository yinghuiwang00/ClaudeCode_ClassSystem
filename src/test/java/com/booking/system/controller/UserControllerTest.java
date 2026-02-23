package com.booking.system.controller;

import com.booking.system.dto.response.UserResponse;
import com.booking.system.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserResponse userResponse;
    private UserResponse userResponse2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        userResponse = new UserResponse(
            1L,
            "testuser",
            "test@example.com",
            "Test",
            "User",
            "ROLE_USER",
            true,
            now
        );

        userResponse2 = new UserResponse(
            2L,
            "adminuser",
            "admin@example.com",
            "Admin",
            "User",
            "ROLE_ADMIN",
            true,
            now
        );
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("Should get current user successfully")
    void shouldGetCurrentUserSuccessfully() throws Exception {
        // Given
        when(userService.getCurrentUser("test@example.com")).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.firstName").value("Test"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"))
            .andExpect(jsonPath("$.isActive").value(true));

        verify(userService).getCurrentUser("test@example.com");
    }

    @Test
    @DisplayName("Should return 403 when getting current user without authentication")
    void shouldReturn403WhenGettingCurrentUserWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isForbidden());

        verify(userService, never()).getCurrentUser(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get user by ID as admin")
    void shouldGetUserByIdAsAdmin() throws Exception {
        // Given
        when(userService.getUserById(1L)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Should return 500 when non-admin tries to get user by ID (AccessDeniedException)")
    void shouldReturn500WhenNonAdminTriesToGetUserById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    @DisplayName("Should return 403 when getting user by ID without authentication")
    void shouldReturn403WhenGettingUserByIdWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all users as admin")
    void shouldGetAllUsersAsAdmin() throws Exception {
        // Given
        List<UserResponse> users = Arrays.asList(userResponse, userResponse2);
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[0].username").value("testuser"))
            .andExpect(jsonPath("$[1].username").value("adminuser"));

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Should return 500 when non-admin tries to get all users (AccessDeniedException)")
    void shouldReturn500WhenNonAdminTriesToGetAllUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    @DisplayName("Should return 403 when getting all users without authentication")
    void shouldReturn403WhenGettingAllUsersWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should return 404 when current user not found")
    void shouldReturn404WhenCurrentUserNotFound() throws Exception {
        // Given
        when(userService.getCurrentUser("test@example.com"))
            .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isInternalServerError());

        verify(userService).getCurrentUser("test@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when user not found by ID")
    void shouldReturn404WhenUserNotFoundById() throws Exception {
        // Given
        when(userService.getUserById(999L))
            .thenThrow(new RuntimeException("User not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/999"))
            .andExpect(status().isInternalServerError());

        verify(userService).getUserById(999L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should use authenticated user's email for current user request")
    void shouldUseAuthenticatedUserEmailForCurrentUserRequest() throws Exception {
        // Given
        when(userService.getCurrentUser("test@example.com")).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk());

        verify(userService).getCurrentUser("test@example.com");
    }

    @Test
    @WithMockUser(username = "another@example.com", roles = "ADMIN")
    @DisplayName("Should get different user by ID")
    void shouldGetDifferentUserById() throws Exception {
        // Given
        when(userService.getUserById(1L)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() throws Exception {
        // Given
        when(userService.getAllUsers()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(userService).getAllUsers();
    }
}