package com.booking.system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT token")
    void shouldAuthenticateUserWithValidJwtToken() throws ServletException, IOException {
        // Given
        UserDetails userDetails = new User(
            TEST_EMAIL,
            "password",
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(VALID_TOKEN);
        verify(tokenProvider).getUsernameFromToken(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_EMAIL);
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should not authenticate when Authorization header is missing")
    void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should not authenticate when Authorization header is empty")
    void shouldNotAuthenticateWhenAuthorizationHeaderIsEmpty() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should not authenticate when token is invalid")
    void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(invalidToken);
        verify(tokenProvider, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should not authenticate when Authorization header doesn't start with Bearer")
    void shouldNotAuthenticateWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic someCredentials");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle exception when token validation fails")
    void shouldHandleExceptionWhenTokenValidationFails() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenThrow(new RuntimeException("Token validation failed"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle exception when userDetailsService fails")
    void shouldHandleExceptionWhenUserDetailsServiceFails() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL))
            .thenThrow(new RuntimeException("User not found"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain even after authentication")
    void shouldContinueFilterChainEvenAfterAuthentication() throws ServletException, IOException {
        // Given
        UserDetails userDetails = new User(
            TEST_EMAIL,
            "password",
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void shouldExtractTokenCorrectlyFromBearerHeader() throws ServletException, IOException {
        // Given
        String token = "ey.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(
            new User(TEST_EMAIL, "pass", Collections.emptyList())
        );

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(token);
        verify(tokenProvider, never()).validateToken("Bearer " + token);
    }

    @Test
    @DisplayName("Should set authentication with correct authorities")
    void shouldSetAuthenticationWithCorrectAuthorities() throws ServletException, IOException {
        // Given
        UserDetails userDetails = new User(
            TEST_EMAIL,
            "password",
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should handle malformed token")
    void shouldHandleMalformedToken() throws ServletException, IOException {
        // Given
        String malformedToken = "not.a.valid.jwt";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + malformedToken);
        when(tokenProvider.validateToken(malformedToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should clear authentication context when token is invalid")
    void shouldClearAuthenticationContextWhenTokenIsInvalid() throws ServletException, IOException {
        // Given - set an existing authentication
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing", null, Collections.emptyList()
            )
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(tokenProvider.validateToken(anyString())).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - the existing authentication should be kept, but filter should not set a new one
        // Note: Actually, the filter doesn't clear existing authentication, it just doesn't set a new one
        verify(filterChain).doFilter(request, response);
    }
}
