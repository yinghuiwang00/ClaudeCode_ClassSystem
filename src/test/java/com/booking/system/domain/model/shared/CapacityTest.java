package com.booking.system.domain.model.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Capacity Value Object Tests")
class CapacityTest {

    @Test
    @DisplayName("Should create valid capacity")
    void shouldCreateValidCapacity() {
        // When
        Capacity capacity = Capacity.of(50);

        // Then
        assertThat(capacity).isNotNull();
        assertThat(capacity.getValue()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should throw exception for null capacity")
    void shouldThrowExceptionForNullCapacity() {
        // When & Then
        assertThatThrownBy(() -> Capacity.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Capacity cannot be null");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should throw exception for non-positive capacity")
    void shouldThrowExceptionForNonPositiveCapacity(int invalidCapacity) {
        // When & Then
        assertThatThrownBy(() -> Capacity.of(invalidCapacity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Capacity must be greater than 0");
    }

    @Test
    @DisplayName("Should throw exception for excessive capacity")
    void shouldThrowExceptionForExcessiveCapacity() {
        // When & Then
        assertThatThrownBy(() -> Capacity.of(1001))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Capacity cannot exceed 1000");
    }

    @Test
    @DisplayName("Should check availability correctly")
    void shouldCheckAvailabilityCorrectly() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThat(capacity.hasAvailability(0)).isTrue();
        assertThat(capacity.hasAvailability(25)).isTrue();
        assertThat(capacity.hasAvailability(49)).isTrue();
        assertThat(capacity.hasAvailability(50)).isFalse();
        assertThat(capacity.hasAvailability(51)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for negative current bookings when checking availability")
    void shouldThrowExceptionForNegativeCurrentBookingsWhenCheckingAvailability() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThatThrownBy(() -> capacity.hasAvailability(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current bookings cannot be negative");
    }

    @Test
    @DisplayName("Should calculate remaining seats correctly")
    void shouldCalculateRemainingSeatsCorrectly() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThat(capacity.getRemaining(0)).isEqualTo(50);
        assertThat(capacity.getRemaining(25)).isEqualTo(25);
        assertThat(capacity.getRemaining(50)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw exception when current bookings exceed capacity")
    void shouldThrowExceptionWhenCurrentBookingsExceedCapacity() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThatThrownBy(() -> capacity.getRemaining(51))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Current bookings exceed capacity");
    }

    @Test
    @DisplayName("Should check if booking is possible")
    void shouldCheckIfBookingIsPossible() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThat(capacity.canBook(0)).isTrue();
        assertThat(capacity.canBook(49)).isTrue();
        assertThat(capacity.canBook(50)).isFalse();
    }

    @Test
    @DisplayName("Should check if cancellation is possible")
    void shouldCheckIfCancellationIsPossible() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThat(capacity.canCancel(0)).isFalse();
        assertThat(capacity.canCancel(1)).isTrue();
        assertThat(capacity.canCancel(50)).isTrue();
    }

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueBasedEquality() {
        // Given
        Capacity capacity1 = Capacity.of(50);
        Capacity capacity2 = Capacity.of(50);
        Capacity capacity3 = Capacity.of(100);

        // When & Then
        assertThat(capacity1).isEqualTo(capacity2);
        assertThat(capacity1).isNotEqualTo(capacity3);
        assertThat(capacity1.hashCode()).isEqualTo(capacity2.hashCode());
    }

    @Test
    @DisplayName("Should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        // Given
        Capacity capacity = Capacity.of(50);

        // When & Then
        assertThat(capacity.toString()).isEqualTo("50");
    }
}