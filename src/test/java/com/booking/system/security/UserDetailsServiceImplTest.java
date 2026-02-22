package com.booking.system.security;

import com.booking.system.entity.User;
import com.booking.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserDetailsServiceImpl Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPasswordHash("encodedPassword123");
        testUser.setRole("ROLE_USER");
        testUser.setIsActive(true);
        testUser.setCreatedAt(now);
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void shouldLoadUserByEmailSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: nonexistent@example.com");

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should load admin user with correct authorities")
    void shouldLoadAdminUserWithCorrectAuthorities() {
        // Given
        testUser.setRole("ROLE_ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

        // Then
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should load instructor user with correct authorities")
    void shouldLoadInstructorUserWithCorrectAuthorities() {
        // Given
        testUser.setRole("ROLE_INSTRUCTOR");
        when(userRepository.findByEmail("instructor@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("instructor@example.com");

        // Then
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_INSTRUCTOR");
    }

    @Test
    @DisplayName("Should use email as username in UserDetails")
    void shouldUseEmailAsUsernameInUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        // Note: The original username is "testuser", but we use email as the username in Spring Security
    }

    @Test
    @DisplayName("Should use password hash as password in UserDetails")
    void shouldUsePasswordHashAsPasswordInUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
    }

    @Test
    @DisplayName("Should load inactive user successfully")
    void shouldLoadInactiveUserSuccessfully() {
        // Given
        testUser.setIsActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        // Note: Spring Security's User implementation doesn't check isActive by default
        // The account status checks are all true unless explicitly set
        assertThat(userDetails.isEnabled()).isTrue();
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should handle user with null values in optional fields")
    void shouldHandleUserWithNullValuesInOptionalFields() {
        // Given
        User userWithNulls = new User();
        userWithNulls.setId(1L);
        userWithNulls.setEmail("test@example.com");
        userWithNulls.setPasswordHash("encodedPassword");
        userWithNulls.setRole("ROLE_USER");
        // Other fields are null

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithNulls));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should call repository with correct email")
    void shouldCallRepositoryWithCorrectEmail() {
        // Given
        when(userRepository.findByEmail("specific.email@test.com")).thenReturn(Optional.of(testUser));

        // When
        userDetailsService.loadUserByUsername("specific.email@test.com");

        // Then
        verify(userRepository, times(1)).findByEmail("specific.email@test.com");
    }

    @Test
    @DisplayName("Should throw exception when user role is null")
    void shouldThrowExceptionWhenUserRoleIsNull() {
        // Given
        testUser.setRole(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        // The SimpleGrantedAuthority constructor requires non-null text
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("test@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A granted authority textual representation is required");
    }

    @Test
    @DisplayName("Should return single authority for user")
    void shouldReturnSingleAuthorityForUser() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }
}
