package com.booking.system.service;

import com.booking.system.dto.request.CreateClassRequest;
import com.booking.system.dto.request.UpdateClassRequest;
import com.booking.system.dto.response.ClassResponse;
import com.booking.system.entity.ClassSchedule;
import com.booking.system.entity.Instructor;
import com.booking.system.entity.User;
import com.booking.system.exception.BookingException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.InstructorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassScheduleService Unit Tests")
class ClassScheduleServiceTest {

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @InjectMocks
    private ClassScheduleService classScheduleService;

    private ClassSchedule testClassSchedule;
    private Instructor testInstructor;
    private User testInstructorUser;
    private CreateClassRequest createClassRequest;
    private UpdateClassRequest updateClassRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        // Setup test instructor user
        testInstructorUser = new User();
        testInstructorUser.setId(1L);
        testInstructorUser.setUsername("instructor");
        testInstructorUser.setEmail("instructor@example.com");
        testInstructorUser.setFirstName("John");
        testInstructorUser.setLastName("Doe");

        // Setup test instructor
        testInstructor = new Instructor();
        testInstructor.setId(1L);
        testInstructor.setUser(testInstructorUser);

        // Setup test class schedule
        testClassSchedule = new ClassSchedule();
        testClassSchedule.setId(1L);
        testClassSchedule.setName("Yoga Class");
        testClassSchedule.setDescription("Relaxing yoga session");
        testClassSchedule.setStartTime(tomorrow);
        testClassSchedule.setEndTime(tomorrow.plusHours(1));
        testClassSchedule.setCapacity(20);
        testClassSchedule.setCurrentBookings(5);
        testClassSchedule.setLocation("Studio A");
        testClassSchedule.setStatus("SCHEDULED");
        testClassSchedule.setInstructor(testInstructor);
        testClassSchedule.setCreatedAt(now);

        // Setup create request
        createClassRequest = new CreateClassRequest();
        createClassRequest.setName("Yoga Class");
        createClassRequest.setDescription("Relaxing yoga session");
        createClassRequest.setStartTime(tomorrow);
        createClassRequest.setEndTime(tomorrow.plusHours(1));
        createClassRequest.setCapacity(20);
        createClassRequest.setLocation("Studio A");
        createClassRequest.setInstructorId(1L);

        // Setup update request
        updateClassRequest = new UpdateClassRequest();
        updateClassRequest.setName("Updated Yoga Class");
        updateClassRequest.setCapacity(25);
        updateClassRequest.setStatus("CANCELLED");
    }

    @Test
    @DisplayName("Should create class successfully")
    void shouldCreateClassSuccessfully() {
        // Given
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(testInstructor));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        ClassResponse response = classScheduleService.createClass(createClassRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Yoga Class");
        assertThat(response.getDescription()).isEqualTo("Relaxing yoga session");
        assertThat(response.getCapacity()).isEqualTo(20);
        assertThat(response.getCurrentBookings()).isEqualTo(5);
        assertThat(response.getAvailableSpots()).isEqualTo(15);
        assertThat(response.getLocation()).isEqualTo("Studio A");
        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
        assertThat(response.getInstructorId()).isEqualTo(1L);

        verify(instructorRepository).findById(1L);
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Should create class without instructor")
    void shouldCreateClassWithoutInstructor() {
        // Given
        createClassRequest.setInstructorId(null);
        testClassSchedule.setInstructor(null);

        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        ClassResponse response = classScheduleService.createClass(createClassRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getInstructorId()).isNull();
        assertThat(response.getInstructorName()).isNull();

        verify(instructorRepository, never()).findById(any());
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Should throw exception when end time is before start time")
    void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createClassRequest.setStartTime(now);
        createClassRequest.setEndTime(now.minusHours(1));

        // When & Then
        assertThatThrownBy(() -> classScheduleService.createClass(createClassRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("End time must be after start time");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when end time equals start time")
    void shouldThrowExceptionWhenEndTimeEqualsStartTime() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        createClassRequest.setStartTime(now);
        createClassRequest.setEndTime(now);

        // When & Then
        assertThatThrownBy(() -> classScheduleService.createClass(createClassRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("End time must be after start time");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when instructor not found")
    void shouldThrowExceptionWhenInstructorNotFound() {
        // Given
        createClassRequest.setInstructorId(999L);
        when(instructorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> classScheduleService.createClass(createClassRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Instructor not found");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set initial values for new class")
    void shouldSetInitialValuesForNewClass() {
        // Given
        createClassRequest.setInstructorId(null);

        when(classScheduleRepository.save(argThat(cs ->
            "SCHEDULED".equals(cs.getStatus()) && cs.getCurrentBookings() == 0
        ))).thenReturn(testClassSchedule);

        // When
        classScheduleService.createClass(createClassRequest);

        // Then
        verify(classScheduleRepository).save(argThat(cs ->
            "SCHEDULED".equals(cs.getStatus()) && cs.getCurrentBookings() == 0
        ));
    }

    @Test
    @DisplayName("Should get class by ID")
    void shouldGetClassById() {
        // Given
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When
        ClassResponse response = classScheduleService.getClassById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Yoga Class");

        verify(classScheduleRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when class not found by ID")
    void shouldThrowExceptionWhenClassNotFoundById() {
        // Given
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> classScheduleService.getClassById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Class not found with id: 999");

        verify(classScheduleRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all classes")
    void shouldGetAllClasses() {
        // Given
        ClassSchedule class2 = new ClassSchedule();
        class2.setId(2L);
        class2.setName("Pilates Class");
        class2.setStartTime(LocalDateTime.now().plusDays(2));
        class2.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        class2.setCapacity(15);
        class2.setCurrentBookings(3);
        class2.setStatus("SCHEDULED");

        when(classScheduleRepository.findAll()).thenReturn(Arrays.asList(testClassSchedule, class2));

        // When
        List<ClassResponse> responses = classScheduleService.getAllClasses();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Yoga Class");
        assertThat(responses.get(1).getName()).isEqualTo("Pilates Class");

        verify(classScheduleRepository).findAll();
    }

    @Test
    @DisplayName("Should get available classes")
    void shouldGetAvailableClasses() {
        // Given
        ClassSchedule fullClass = new ClassSchedule();
        fullClass.setId(2L);
        fullClass.setName("Full Class");
        fullClass.setStartTime(LocalDateTime.now().plusDays(1));
        fullClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        fullClass.setCapacity(10);
        fullClass.setCurrentBookings(10);
        fullClass.setStatus("SCHEDULED");

        when(classScheduleRepository.findUpcomingClassesByStatus(eq("SCHEDULED"), any()))
            .thenReturn(Arrays.asList(testClassSchedule, fullClass));

        // When
        List<ClassResponse> responses = classScheduleService.getAvailableClasses();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Yoga Class");

        verify(classScheduleRepository).findUpcomingClassesByStatus(eq("SCHEDULED"), any());
    }

    @Test
    @DisplayName("Should get classes by status")
    void shouldGetClassesByStatus() {
        // Given
        ClassSchedule cancelledClass = new ClassSchedule();
        cancelledClass.setId(2L);
        cancelledClass.setName("Cancelled Class");
        cancelledClass.setStatus("CANCELLED");

        when(classScheduleRepository.findByStatus("SCHEDULED")).thenReturn(Arrays.asList(testClassSchedule));

        // When
        List<ClassResponse> responses = classScheduleService.getClassesByStatus("SCHEDULED");

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("SCHEDULED");

        verify(classScheduleRepository).findByStatus("SCHEDULED");
    }

    @Test
    @DisplayName("Should get classes by instructor")
    void shouldGetClassesByInstructor() {
        // Given
        when(classScheduleRepository.findByInstructorId(1L)).thenReturn(Arrays.asList(testClassSchedule));

        // When
        List<ClassResponse> responses = classScheduleService.getClassesByInstructor(1L);

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getInstructorId()).isEqualTo(1L);

        verify(classScheduleRepository).findByInstructorId(1L);
    }

    @Test
    @DisplayName("Should update class successfully")
    void shouldUpdateClassSuccessfully() {
        // Given
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        ClassResponse response = classScheduleService.updateClass(1L, updateClassRequest);

        // Then
        assertThat(response).isNotNull();
        verify(classScheduleRepository).findById(1L);
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Should throw exception when updating capacity below current bookings")
    void shouldThrowExceptionWhenUpdatingCapacityBelowCurrentBookings() {
        // Given
        updateClassRequest.setCapacity(3); // Less than currentBookings (5)
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When & Then
        assertThatThrownBy(() -> classScheduleService.updateClass(1L, updateClassRequest))
            .isInstanceOf(BookingException.class)
            .hasMessageContaining("Cannot reduce capacity below current bookings");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow updating capacity equal to or above current bookings")
    void shouldAllowUpdatingCapacityEqualToOrAboveCurrentBookings() {
        // Given
        updateClassRequest.setCapacity(5); // Equal to currentBookings
        updateClassRequest.setName("Updated Name");

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        classScheduleService.updateClass(1L, updateClassRequest);

        // Then
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Should update class instructor")
    void shouldUpdateClassInstructor() {
        // Given
        updateClassRequest.setInstructorId(2L);

        Instructor newInstructor = new Instructor();
        newInstructor.setId(2L);

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));
        when(instructorRepository.findById(2L)).thenReturn(Optional.of(newInstructor));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        classScheduleService.updateClass(1L, updateClassRequest);

        // Then
        verify(instructorRepository).findById(2L);
        verify(classScheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent class")
    void shouldThrowExceptionWhenUpdatingNonExistentClass() {
        // Given
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> classScheduleService.updateClass(999L, updateClassRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Class not found with id: 999");

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete class without bookings")
    void shouldDeleteClassWithoutBookings() {
        // Given
        testClassSchedule.setCurrentBookings(0);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When
        classScheduleService.deleteClass(1L);

        // Then
        verify(classScheduleRepository).delete(testClassSchedule);
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel class with bookings instead of deleting")
    void shouldCancelClassWithBookingsInsteadOfDeleting() {
        // Given
        testClassSchedule.setCurrentBookings(5);
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenReturn(testClassSchedule);

        // When
        classScheduleService.deleteClass(1L);

        // Then
        verify(classScheduleRepository, never()).delete(any());
        verify(classScheduleRepository).save(argThat(cs -> "CANCELLED".equals(cs.getStatus())));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent class")
    void shouldThrowExceptionWhenDeletingNonExistentClass() {
        // Given
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> classScheduleService.deleteClass(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Class not found with id: 999");

        verify(classScheduleRepository, never()).delete(any());
        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate available spots correctly")
    void shouldCalculateAvailableSpotsCorrectly() {
        // Given
        testClassSchedule.setCapacity(20);
        testClassSchedule.setCurrentBookings(5);

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When
        ClassResponse response = classScheduleService.getClassById(1L);

        // Then
        assertThat(response.getAvailableSpots()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should include instructor name in response")
    void shouldIncludeInstructorNameInResponse() {
        // Given
        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When
        ClassResponse response = classScheduleService.getClassById(1L);

        // Then
        assertThat(response.getInstructorName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should not include instructor name when instructor has no user")
    void shouldNotIncludeInstructorNameWhenInstructorHasNoUser() {
        // Given
        Instructor instructorWithoutUser = new Instructor();
        instructorWithoutUser.setId(1L);
        testClassSchedule.setInstructor(instructorWithoutUser);

        when(classScheduleRepository.findById(1L)).thenReturn(Optional.of(testClassSchedule));

        // When
        ClassResponse response = classScheduleService.getClassById(1L);

        // Then
        assertThat(response.getInstructorName()).isNull();
    }
}
