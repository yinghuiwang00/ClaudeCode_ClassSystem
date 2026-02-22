package com.booking.system.service;

import com.booking.system.dto.response.UserResponse;
import com.booking.system.entity.User;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole("ROLE_USER");
        testUser.setIsActive(true);
        testUser.setCreatedAt(now);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        testUser2.setRole("ROLE_ADMIN");
        testUser2.setIsActive(true);
        testUser2.setCreatedAt(now);
    }

    @Test
    @DisplayName("Should get current user by email")
    void shouldGetCurrentUserByEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getCurrentUser("test@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(response.getIsActive()).isTrue();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser("nonexistent@example.com"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should get user by ID")
    void shouldGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found with id: 999");

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, testUser2));

        // When
        List<UserResponse> responses = userService.getAllUsers();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        assertThat(responses.get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(responses.get(1).getEmail()).isEqualTo("test2@example.com");

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<UserResponse> responses = userService.getAllUsers();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should correctly map user fields to response")
    void shouldCorrectlyMapUserFieldsToResponse() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getCurrentUser("test@example.com");

        // Then
        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(response.getLastName()).isEqualTo(testUser.getLastName());
        assertThat(response.getRole()).isEqualTo(testUser.getRole());
        assertThat(response.getIsActive()).isEqualTo(testUser.getIsActive());
        assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle user with null values in optional fields")
    void shouldHandleUserWithNullValuesInOptionalFields() {
        // Given
        User userWithNulls = new User();
        userWithNulls.setId(1L);
        userWithNulls.setUsername("testuser");
        userWithNulls.setEmail("test@example.com");
        userWithNulls.setFirstName("Test");
        userWithNulls.setLastName("User");
        userWithNulls.setRole("ROLE_USER");
        // isActive and createdAt might be null

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithNulls));

        // When
        UserResponse response = userService.getCurrentUser("test@example.com");

        // Then
        assertThat(response).isNotNull();
        // isActive has a default value of true in User entity
        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("Should preserve user role in response")
    void shouldPreserveUserRoleInResponse() {
        // Given
        testUser.setRole("ROLE_ADMIN");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getCurrentUser("test@example.com");

        // Then
        assertThat(response.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should preserve user active status in response")
    void shouldPreserveUserActiveStatusInResponse() {
        // Given
        testUser.setIsActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getCurrentUser("test@example.com");

        // Then
        assertThat(response.getIsActive()).isFalse();
    }
}
