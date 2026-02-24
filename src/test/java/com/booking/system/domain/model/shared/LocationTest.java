package com.booking.system.domain.model.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Location Value Object Tests")
class LocationTest {

    @Test
    @DisplayName("Should create valid location")
    void shouldCreateValidLocation() {
        // When
        Location location = Location.of("Room 101, Building A");

        // Then
        assertThat(location).isNotNull();
        assertThat(location.getValue()).isEqualTo("Room 101, Building A");
    }

    @Test
    @DisplayName("Should throw exception for null location")
    void shouldThrowExceptionForNullLocation() {
        // When & Then
        assertThatThrownBy(() -> Location.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Location cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for empty location")
    void shouldThrowExceptionForEmptyLocation() {
        // When & Then
        assertThatThrownBy(() -> Location.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Location cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for blank location")
    void shouldThrowExceptionForBlankLocation() {
        // When & Then
        assertThatThrownBy(() -> Location.of("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Location cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for location exceeding 200 characters")
    void shouldThrowExceptionForLocationExceeding200Characters() {
        // Given
        String longLocation = "A".repeat(201);

        // When & Then
        assertThatThrownBy(() -> Location.of(longLocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Location cannot exceed 200 characters");
    }

    @Test
    @DisplayName("Should accept location exactly 200 characters")
    void shouldAcceptLocationExactly200Characters() {
        // Given
        String exactLocation = "A".repeat(200);

        // When
        Location location = Location.of(exactLocation);

        // Then
        assertThat(location).isNotNull();
        assertThat(location.getValue()).isEqualTo(exactLocation);
    }

    @Test
    @DisplayName("Should trim whitespace from location")
    void shouldTrimWhitespaceFromLocation() {
        // When
        Location location = Location.of("  Room 101  ");

        // Then
        assertThat(location.getValue()).isEqualTo("Room 101");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Online Class",
        "Virtual Classroom",
        "Zoom Meeting Room",
        "Webinar Series",
        "meet.google.com/abc-def-ghi",
        "Microsoft Teams Meeting",
        "Skype Conference"
    })
    @DisplayName("Should identify virtual locations")
    void shouldIdentifyVirtualLocations(String virtualLocation) {
        // When
        Location location = Location.of(virtualLocation);

        // Then
        assertThat(location.isVirtual()).isTrue();
        assertThat(location.isPhysical()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Room 101, Building A",
        "Conference Center Main Hall",
        "Sports Complex Gymnasium",
        "Library Study Room 3",
        "Cafeteria Meeting Area"
    })
    @DisplayName("Should identify physical locations")
    void shouldIdentifyPhysicalLocations(String physicalLocation) {
        // When
        Location location = Location.of(physicalLocation);

        // Then
        assertThat(location.isVirtual()).isFalse();
        assertThat(location.isPhysical()).isTrue();
    }

    @Test
    @DisplayName("Should get short description for short location")
    void shouldGetShortDescriptionForShortLocation() {
        // Given
        String shortLocation = "Room 101";
        Location location = Location.of(shortLocation);

        // When
        String shortDescription = location.getShortDescription();

        // Then
        assertThat(shortDescription).isEqualTo(shortLocation);
    }

    @Test
    @DisplayName("Should truncate long location in short description")
    void shouldTruncateLongLocationInShortDescription() {
        // Given
        String longLocation = "Conference Center Main Hall - East Wing Building, Floor 3, Room 301A with Projector and Whiteboard Facilities";
        Location location = Location.of(longLocation);

        // When
        String shortDescription = location.getShortDescription();

        // Then
        assertThat(shortDescription).hasSize(50);
        assertThat(shortDescription).endsWith("...");
        assertThat(shortDescription).startsWith("Conference Center Main Hall - East Wing Build");
    }

    @Test
    @DisplayName("Should get correct short description for 50 character location")
    void shouldGetCorrectShortDescriptionFor50CharacterLocation() {
        // Given
        String exact50Chars = "A".repeat(50);
        Location location = Location.of(exact50Chars);

        // When
        String shortDescription = location.getShortDescription();

        // Then
        assertThat(shortDescription).hasSize(50);
        assertThat(shortDescription).doesNotEndWith("...");
        assertThat(shortDescription).isEqualTo(exact50Chars);
    }

    @Test
    @DisplayName("Should get correct short description for 51 character location")
    void shouldGetCorrectShortDescriptionFor51CharacterLocation() {
        // Given
        String exact51Chars = "A".repeat(51);
        Location location = Location.of(exact51Chars);

        // When
        String shortDescription = location.getShortDescription();

        // Then
        assertThat(shortDescription).hasSize(50);
        assertThat(shortDescription).endsWith("...");
        assertThat(shortDescription).startsWith("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }


    @ParameterizedTest
    @ValueSource(strings = {
        "Room 101-202",           // hyphen is allowed
        "Building A, Floor 3",    // comma is allowed
        "Conference (Main Hall)", // parentheses are allowed
        "Room 101 & 102",         // ampersand is allowed
        "Cafeteria @ Main Bldg",  // @ is allowed
        "Study Room #3",          // # is allowed
        "Price: $100"             // $ is allowed
    })
    @DisplayName("Should accept location with valid punctuation")
    void shouldAcceptLocationWithValidPunctuation(String validLocation) {
        // When
        Location location = Location.of(validLocation);

        // Then
        assertThat(location).isNotNull();
        assertThat(location.getValue()).isEqualTo(validLocation);
    }

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueBasedEquality() {
        // Given
        Location location1 = Location.of("Room 101");
        Location location2 = Location.of("Room 101");
        Location location3 = Location.of("Room 102");

        // When & Then
        assertThat(location1).isEqualTo(location2);
        assertThat(location1).isNotEqualTo(location3);
        assertThat(location1.hashCode()).isEqualTo(location2.hashCode());
    }

    @Test
    @DisplayName("Should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        // Given
        Location location = Location.of("Room 101, Building A");

        // When & Then
        assertThat(location.toString()).isEqualTo("Room 101, Building A");
    }
}