package com.booking.system.domain.model.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TimeRange Value Object Tests")
class TimeRangeTest {

    private LocalDateTime now = LocalDateTime.now();
    private LocalDateTime inOneHour = now.plusHours(1);
    private LocalDateTime inTwoHours = now.plusHours(2);

    @Test
    @DisplayName("Should create valid time range")
    void shouldCreateValidTimeRange() {
        // When
        TimeRange timeRange = TimeRange.of(inOneHour, inTwoHours);

        // Then
        assertThat(timeRange).isNotNull();
        assertThat(timeRange.getStartTime()).isEqualTo(inOneHour);
        assertThat(timeRange.getEndTime()).isEqualTo(inTwoHours);
    }

    @Test
    @DisplayName("Should throw exception for null start time")
    void shouldThrowExceptionForNullStartTime() {
        // When & Then
        assertThatThrownBy(() -> TimeRange.of(null, inTwoHours))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start time cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for null end time")
    void shouldThrowExceptionForNullEndTime() {
        // When & Then
        assertThatThrownBy(() -> TimeRange.of(inOneHour, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End time cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when start time equals end time")
    void shouldThrowExceptionWhenStartTimeEqualsEndTime() {
        // Given
        LocalDateTime sameTime = inOneHour;

        // When & Then
        assertThatThrownBy(() -> TimeRange.of(sameTime, sameTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start time must be before end time");
    }

    @Test
    @DisplayName("Should throw exception when start time is after end time")
    void shouldThrowExceptionWhenStartTimeIsAfterEndTime() {
        // When & Then
        assertThatThrownBy(() -> TimeRange.of(inTwoHours, inOneHour))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start time must be before end time");
    }

    @Test
    @DisplayName("Should throw exception for duration less than 30 minutes")
    void shouldThrowExceptionForDurationLessThan30Minutes() {
        // Given
        LocalDateTime start = now.plusMinutes(10);
        LocalDateTime end = start.plusMinutes(29);

        // When & Then
        assertThatThrownBy(() -> TimeRange.of(start, end))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class duration must be at least 30 minutes");
    }

    @Test
    @DisplayName("Should throw exception for duration exceeding 8 hours")
    void shouldThrowExceptionForDurationExceeding8Hours() {
        // Given
        LocalDateTime start = now.plusMinutes(10);
        LocalDateTime end = start.plusHours(8).plusMinutes(1);

        // When & Then
        assertThatThrownBy(() -> TimeRange.of(start, end))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class duration cannot exceed 8 hours");
    }


    @Test
    @DisplayName("Should accept duration exactly 30 minutes")
    void shouldAcceptDurationExactly30Minutes() {
        // Given
        LocalDateTime start = now.plusMinutes(10);
        LocalDateTime end = start.plusMinutes(30);

        // When
        TimeRange timeRange = TimeRange.of(start, end);

        // Then
        assertThat(timeRange).isNotNull();
        assertThat(timeRange.getDurationInMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should accept duration exactly 8 hours")
    void shouldAcceptDurationExactly8Hours() {
        // Given
        LocalDateTime start = now.plusMinutes(10);
        LocalDateTime end = start.plusHours(8);

        // When
        TimeRange timeRange = TimeRange.of(start, end);

        // Then
        assertThat(timeRange).isNotNull();
        assertThat(timeRange.getDurationInMinutes()).isEqualTo(8 * 60);
    }

    @Test
    @DisplayName("Should calculate duration correctly")
    void shouldCalculateDurationCorrectly() {
        // Given
        LocalDateTime start = now.plusHours(1);
        LocalDateTime end = start.plusHours(2).plusMinutes(30); // 2.5 hours = 150 minutes

        // When
        TimeRange timeRange = TimeRange.of(start, end);

        // Then
        assertThat(timeRange.getDurationInMinutes()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should check if time ranges overlap")
    void shouldCheckIfTimeRangesOverlap() {
        // Given
        TimeRange range1 = TimeRange.of(now.plusHours(1), now.plusHours(3));
        TimeRange range2 = TimeRange.of(now.plusHours(2), now.plusHours(4)); // Overlaps 2-3
        TimeRange range3 = TimeRange.of(now.plusHours(4), now.plusHours(6)); // No overlap
        TimeRange range4 = TimeRange.of(now.plusHours(0), now.plusHours(2)); // Overlaps 1-2

        // When & Then
        assertThat(range1.overlaps(range2)).isTrue();
        assertThat(range2.overlaps(range1)).isTrue();
        assertThat(range1.overlaps(range3)).isFalse();
        assertThat(range1.overlaps(range4)).isTrue();
    }

    @Test
    @DisplayName("Should check if has started")
    void shouldCheckIfHasStarted() {
        // Given - start time in the past, end time in future
        TimeRange pastStartRange = TimeRange.of(now.minusHours(1), now.plusHours(1));

        // When & Then
        assertThat(pastStartRange.hasStarted()).isTrue();
    }

    @Test
    @DisplayName("Should check if has not started")
    void shouldCheckIfHasNotStarted() {
        // Given - both start and end in future
        TimeRange futureRange = TimeRange.of(now.plusHours(1), now.plusHours(2));

        // When & Then
        assertThat(futureRange.hasStarted()).isFalse();
    }

    @Test
    @DisplayName("Should check if has ended")
    void shouldCheckIfHasEnded() {
        // Given - both start and end in the past
        TimeRange pastRange = TimeRange.of(now.minusHours(2), now.minusHours(1));

        // When & Then
        assertThat(pastRange.hasEnded()).isTrue();
    }

    @Test
    @DisplayName("Should check if has not ended")
    void shouldCheckIfHasNotEnded() {
        // Given - end time in future
        TimeRange ongoingRange = TimeRange.of(now.minusHours(1), now.plusHours(1));

        // When & Then
        assertThat(ongoingRange.hasEnded()).isFalse();
    }

    @Test
    @DisplayName("Should check if is in progress")
    void shouldCheckIfIsInProgress() {
        // Given - start in past, end in future
        TimeRange ongoingRange = TimeRange.of(now.minusMinutes(30), now.plusMinutes(30));

        // When & Then
        assertThat(ongoingRange.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("Should check if is not in progress (not started)")
    void shouldCheckIfIsNotInProgressNotStarted() {
        // Given - both in future
        TimeRange futureRange = TimeRange.of(now.plusHours(1), now.plusHours(2));

        // When & Then
        assertThat(futureRange.isInProgress()).isFalse();
    }

    @Test
    @DisplayName("Should check if is not in progress (ended)")
    void shouldCheckIfIsNotInProgressEnded() {
        // Given - both in past
        TimeRange pastRange = TimeRange.of(now.minusHours(2), now.minusHours(1));

        // When & Then
        assertThat(pastRange.isInProgress()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "30, true",   // 30 minutes before start (threshold exactly now)
        "15, false",  // 15 minutes before start (threshold in future)
        "0, false",   // exactly at start time (threshold at start time)
        "5, false"    // 5 minutes before start (threshold in future)
    })
    @DisplayName("Should check if starting soon")
    void shouldCheckIfStartingSoon(int minutesBefore, boolean expected) {
        // Given
        LocalDateTime start = now.plusMinutes(30);
        LocalDateTime end = start.plusHours(1);
        TimeRange timeRange = TimeRange.of(start, end);

        // When
        boolean result = timeRange.isStartingSoon(minutesBefore);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueBasedEquality() {
        // Given
        TimeRange range1 = TimeRange.of(inOneHour, inTwoHours);
        TimeRange range2 = TimeRange.of(inOneHour, inTwoHours);
        TimeRange range3 = TimeRange.of(inOneHour.plusHours(1), inTwoHours.plusHours(1));

        // When & Then
        assertThat(range1).isEqualTo(range2);
        assertThat(range1).isNotEqualTo(range3);
        assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
    }

    @Test
    @DisplayName("Should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 2, 24, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 24, 12, 0);
        TimeRange timeRange = TimeRange.of(start, end);

        // When & Then
        assertThat(timeRange.toString()).contains("10:00").contains("12:00");
    }
}