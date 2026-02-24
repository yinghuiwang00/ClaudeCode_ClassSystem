package com.booking.system.domain.model.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object Tests")
class EmailTest {

    @Test
    @DisplayName("Should create email with valid format")
    void shouldCreateEmailWithValidFormat() {
        // When
        Email email = Email.of("test@example.com");

        // Then
        assertThat(email).isNotNull();
        assertThat(email.getValue()).isEqualTo("test@example.com");
        assertThat(email.getLocalPart()).isEqualTo("test");
        assertThat(email.getDomain()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // When
        Email email = Email.of("TEST@EXAMPLE.COM");

        // Then
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespaceFromEmail() {
        // When
        Email email = Email.of("  test@example.com  ");

        // Then
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception for null email")
    void shouldThrowExceptionForNullEmail() {
        // When & Then
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception for empty email")
    void shouldThrowExceptionForEmptyEmail() {
        // When & Then
        assertThatThrownBy(() -> Email.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception for blank email")
    void shouldThrowExceptionForBlankEmail() {
        // When & Then
        assertThatThrownBy(() -> Email.of("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",
        "test@",
        "@example.com",
        "test@example",
        "test@example.",
        "test@.com",
        "test@example..com"
    })
    @DisplayName("Should throw exception for invalid email formats")
    void shouldThrowExceptionForInvalidEmailFormats(String invalidEmail) {
        // When & Then
        assertThatThrownBy(() -> Email.of(invalidEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("Should consider emails with same value as equal")
    void shouldConsiderEmailsWithSameValueAsEqual() {
        // Given
        Email email1 = Email.of("test@example.com");
        Email email2 = Email.of("TEST@EXAMPLE.COM"); // Will be normalized

        // Then
        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    @DisplayName("Should consider emails with different values as not equal")
    void shouldConsiderEmailsWithDifferentValuesAsNotEqual() {
        // Given
        Email email1 = Email.of("test1@example.com");
        Email email2 = Email.of("test2@example.com");

        // Then
        assertThat(email1).isNotEqualTo(email2);
    }

    @Test
    @DisplayName("Should return string representation")
    void shouldReturnStringRepresentation() {
        // Given
        Email email = Email.of("test@example.com");

        // When
        String stringValue = email.toString();

        // Then
        assertThat(stringValue).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Static isValid method should validate email format")
    void staticIsValidMethodShouldValidateEmailFormat() {
        // Then
        assertThat(Email.isValid("test@example.com")).isTrue();
        assertThat(Email.isValid("invalid-email")).isFalse();
        assertThat(Email.isValid(null)).isFalse();
        assertThat(Email.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Static normalize method should normalize email")
    void staticNormalizeMethodShouldNormalizeEmail() {
        // Then
        assertThat(Email.normalize("TEST@EXAMPLE.COM")).isEqualTo("test@example.com");
        assertThat(Email.normalize("  test@example.com  ")).isEqualTo("test@example.com");
        assertThat(Email.normalize(null)).isNull();
    }
}