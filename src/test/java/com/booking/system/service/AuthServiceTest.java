package com.booking.system.service;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.dto.response.AuthResponse;
import com.booking.system.entity.User;
import com.booking.system.exception.AuthenticationException;
import com.booking.system.repository.UserRepository;
import com.booking.system.security.JwtTokenProvider;
import com.booking.system.domain.model.shared.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private com.booking.system.domain.service.AuthDomainService authDomainService;

    @Mock
    private com.booking.system.infrastructure.adapters.UserAdapter userAdapter;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private com.booking.system.domain.model.user.User testDomainUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole("ROLE_USER");
        testUser.setIsActive(true);

        // 创建领域用户对象
        testDomainUser = com.booking.system.domain.model.user.User.create(
            "testuser",
            Email.of("test@example.com"),
            "Test",
            "User",
            "encodedPassword",
            "ROLE_USER"
        );
        // 设置ID
        testDomainUser.setId(1L);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        when(authDomainService.register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        )).thenReturn(testDomainUser);

        when(userAdapter.toLegacy(testDomainUser)).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");

        verify(authDomainService).register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        );
        verify(userAdapter).toLegacy(testDomainUser);
        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider).generateToken(any(Authentication.class));
        // 注意：现在不再直接调用userRepository.save
    }

    @Test
    @DisplayName("Should throw exception when email already exists during registration")
    void shouldThrowExceptionWhenEmailExistsDuringRegistration() {
        // Given
        when(authDomainService.register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        )).thenThrow(new com.booking.system.domain.shared.DomainException("Email already exists"));

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Email already exists");

        verify(authDomainService).register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists during registration")
    void shouldThrowExceptionWhenUsernameExistsDuringRegistration() {
        // Given
        when(authDomainService.register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        )).thenThrow(new com.booking.system.domain.shared.DomainException("Username already exists"));

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Username already exists");

        verify(authDomainService).register(
            eq("testuser"),
            eq("test@example.com"),
            eq("password123"),
            eq("Test"),
            eq("User")
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should set default role as ROLE_USER for new user")
    void shouldSetDefaultRoleAsRoleUserForNewUser() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getRole()).isEqualTo("ROLE_USER");
            return testUser;
        });
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(userRepository).save(argThat(user -> "ROLE_USER".equals(user.getRole())));
    }

    @Test
    @DisplayName("Should set isActive as true for new user")
    void shouldSetIsActiveAsTrueForNewUser() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getIsActive()).isTrue();
            return testUser;
        });
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(userRepository).save(argThat(user -> user.getIsActive() != null && user.getIsActive()));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(tokenProvider).generateToken(any(Authentication.class));
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid credentials");

        verify(tokenProvider, never()).generateToken(any(Authentication.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found after successful authentication")
    void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should encode password during registration")
    void shouldEncodePasswordDuringRegistration() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should use email for authentication during login")
    void shouldUseEmailForAuthenticationDuringLogin() {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        authService.login(loginRequest);

        // Then
        verify(authenticationManager).authenticate(
            argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getName().equals("test@example.com") &&
                auth.getCredentials().equals("password123"))
        );
    }

    @Test
    @DisplayName("Should use email for authentication during registration")
    void shouldUseEmailForAuthenticationDuringRegistration() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(authenticationManager).authenticate(
            argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getName().equals("test@example.com"))
        );
    }
}
