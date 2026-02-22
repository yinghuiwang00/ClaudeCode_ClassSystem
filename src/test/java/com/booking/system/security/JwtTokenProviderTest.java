package com.booking.system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "testSecretKeyForJWTTestingPurposeWithMinimum256BitsForHS256Algorithm";
    private static final long TEST_EXPIRATION = 86400000; // 24 hours
    private static final String TEST_USERNAME = "test@example.com";

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", TEST_EXPIRATION);

        authentication = new UsernamePasswordAuthenticationToken(
            TEST_USERNAME,
            null,
            java.util.Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Should generate valid token")
    void shouldGenerateValidToken() {
        // When
        String token = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should validate correct token")
    void shouldValidateCorrectToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should not validate null token")
    void shouldNotValidateNullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate empty token")
    void shouldNotValidateEmptyToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate invalid token")
    void shouldNotValidateInvalidToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.here");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate malformed token")
    void shouldNotValidateMalformedToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("not.a.jwt.token");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
            "another@example.com",
            null,
            java.util.Collections.emptyList()
        );

        // When
        String token1 = jwtTokenProvider.generateToken(authentication);
        String token2 = jwtTokenProvider.generateToken(auth2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should handle tokens with special characters in username")
    void shouldHandleTokensWithSpecialCharactersInUsername() {
        // Given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user+tag@domain.com",
            null,
            java.util.Collections.emptyList()
        );

        // When
        String token = jwtTokenProvider.generateToken(auth);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("user+tag@domain.com");
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void shouldThrowExceptionForExpiredToken() {
        // Given
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -1); // Expired
        String expiredToken = jwtTokenProvider.generateToken(authentication);

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate token with correct structure")
    void shouldGenerateTokenWithCorrectStructure() {
        // When
        String token = jwtTokenProvider.generateToken(authentication);

        // Then
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Decode header (first part) - should contain alg
        String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
        assertThat(header).contains("HS512");
        assertThat(header).contains("alg");

        // Decode payload (second part) - should contain sub (subject/username)
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload).contains(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should not validate token with wrong signature")
    void shouldNotValidateTokenWithWrongSignature() {
        // Given
        String validToken = jwtTokenProvider.generateToken(authentication);
        String[] parts = validToken.split("\\.");
        // Modify the signature (third part)
        String tamperedToken = parts[0] + "." + parts[1] + ".tamperedSignature";

        // When
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle long usernames")
    void shouldHandleLongUsernames() {
        // Given
        String longUsername = "very.long.username.with.many.characters." +
            "that.should.work.fine.with.jwt@subdomain.example.co.uk";
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            longUsername,
            null,
            java.util.Collections.emptyList()
        );

        // When
        String token = jwtTokenProvider.generateToken(auth);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(longUsername);
    }
}
