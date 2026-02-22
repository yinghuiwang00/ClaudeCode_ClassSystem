package com.booking.system.controller;

import com.booking.system.dto.request.CreateClassRequest;
import com.booking.system.dto.request.UpdateClassRequest;
import com.booking.system.dto.response.ClassResponse;
import com.booking.system.service.ClassScheduleService;
import com.booking.system.repository.UserRepository;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.InstructorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassController Unit Tests")
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClassScheduleService classScheduleService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ClassScheduleRepository classScheduleRepository;

    @MockBean
    private InstructorRepository instructorRepository;


    private CreateClassRequest createClassRequest;
    private UpdateClassRequest updateClassRequest;
    private ClassResponse classResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

        createClassRequest = new CreateClassRequest();
        createClassRequest.setName("Yoga Class");
        createClassRequest.setDescription("Relaxing yoga session");
        createClassRequest.setStartTime(tomorrow);
        createClassRequest.setEndTime(tomorrow.plusHours(1));
        createClassRequest.setCapacity(20);
        createClassRequest.setLocation("Studio A");
        createClassRequest.setInstructorId(1L);

        updateClassRequest = new UpdateClassRequest();
        updateClassRequest.setName("Updated Yoga Class");
        updateClassRequest.setCapacity(25);

        classResponse = new ClassResponse();
        classResponse.setId(1L);
        classResponse.setName("Yoga Class");
        classResponse.setDescription("Relaxing yoga session");
        classResponse.setStartTime(tomorrow);
        classResponse.setEndTime(tomorrow.plusHours(1));
        classResponse.setCapacity(20);
        classResponse.setCurrentBookings(5);
        classResponse.setAvailableSpots(15);
        classResponse.setLocation("Studio A");
        classResponse.setStatus("SCHEDULED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create class successfully as admin")
    void shouldCreateClassSuccessfullyAsAdmin() throws Exception {
        // Given
        when(classScheduleService.createClass(any(CreateClassRequest.class))).thenReturn(classResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createClassRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Yoga Class"))
            .andExpect(jsonPath("$.capacity").value(20));

        verify(classScheduleService).createClass(any(CreateClassRequest.class));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Should create class successfully as instructor")
    void shouldCreateClassSuccessfullyAsInstructor() throws Exception {
        // Given
        when(classScheduleService.createClass(any(CreateClassRequest.class))).thenReturn(classResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createClassRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        verify(classScheduleService).createClass(any(CreateClassRequest.class));
    }

    @Test
    @DisplayName("Should return 403 when creating class without authentication")
    void shouldReturn403WhenCreatingClassWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createClassRequest)))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).createClass(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user tries to create class")
    void shouldReturn403WhenUserTriesToCreateClass() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createClassRequest)))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).createClass(any());
    }

    @Test
    @DisplayName("Should get class by ID without authentication")
    void shouldGetClassByIdWithoutAuthentication() throws Exception {
        // Given
        when(classScheduleService.getClassById(1L)).thenReturn(classResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/classes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Yoga Class"));

        verify(classScheduleService).getClassById(1L);
    }

    @Test
    @DisplayName("Should get all classes without authentication")
    void shouldGetAllClassesWithoutAuthentication() throws Exception {
        // Given
        ClassResponse class2 = new ClassResponse();
        class2.setId(2L);
        class2.setName("Pilates Class");

        List<ClassResponse> classes = Arrays.asList(classResponse, class2);
        when(classScheduleService.getAllClasses()).thenReturn(classes);

        // When & Then
        mockMvc.perform(get("/api/v1/classes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("Yoga Class"))
            .andExpect(jsonPath("$[1].name").value("Pilates Class"));

        verify(classScheduleService).getAllClasses();
    }

    @Test
    @DisplayName("Should get available classes only")
    void shouldGetAvailableClassesOnly() throws Exception {
        // Given
        when(classScheduleService.getAvailableClasses()).thenReturn(Arrays.asList(classResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/classes")
                .param("availableOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(classScheduleService).getAvailableClasses();
        verify(classScheduleService, never()).getAllClasses();
    }

    @Test
    @DisplayName("Should get classes by status")
    void shouldGetClassesByStatus() throws Exception {
        // Given
        when(classScheduleService.getClassesByStatus("SCHEDULED")).thenReturn(Arrays.asList(classResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/classes")
                .param("status", "SCHEDULED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(classScheduleService).getClassesByStatus("SCHEDULED");
    }

    @Test
    @DisplayName("Should get classes by instructor")
    void shouldGetClassesByInstructor() throws Exception {
        // Given
        when(classScheduleService.getClassesByInstructor(1L)).thenReturn(Arrays.asList(classResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/classes")
                .param("instructorId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(classScheduleService).getClassesByInstructor(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update class successfully")
    void shouldUpdateClassSuccessfully() throws Exception {
        // Given
        classResponse.setName("Updated Yoga Class");
        when(classScheduleService.updateClass(eq(1L), any(UpdateClassRequest.class))).thenReturn(classResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/classes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateClassRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(classScheduleService).updateClass(eq(1L), any(UpdateClassRequest.class));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Should update class as instructor")
    void shouldUpdateClassAsInstructor() throws Exception {
        // Given
        when(classScheduleService.updateClass(eq(1L), any(UpdateClassRequest.class))).thenReturn(classResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/classes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateClassRequest)))
            .andExpect(status().isOk());

        verify(classScheduleService).updateClass(eq(1L), any(UpdateClassRequest.class));
    }

    @Test
    @DisplayName("Should return 403 when updating class without authentication")
    void shouldReturn403WhenUpdatingClassWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/classes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateClassRequest)))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).updateClass(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user tries to update class")
    void shouldReturn403WhenUserTriesToUpdateClass() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/classes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateClassRequest)))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).updateClass(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete class successfully")
    void shouldDeleteClassSuccessfully() throws Exception {
        // Given
        doNothing().when(classScheduleService).deleteClass(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/classes/1")
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(classScheduleService).deleteClass(1L);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Should delete class as instructor")
    void shouldDeleteClassAsInstructor() throws Exception {
        // Given
        doNothing().when(classScheduleService).deleteClass(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/classes/1")
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(classScheduleService).deleteClass(1L);
    }

    @Test
    @DisplayName("Should return 403 when deleting class without authentication")
    void shouldReturn403WhenDeletingClassWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/classes/1")
                .with(csrf()))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).deleteClass(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user tries to delete class")
    void shouldReturn403WhenUserTriesToDeleteClass() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/classes/1")
                .with(csrf()))
            .andExpect(status().isForbidden());

        verify(classScheduleService, never()).deleteClass(anyLong());
    }

    @Test
    @DisplayName("Should return 404 when class not found")
    void shouldReturn404WhenClassNotFound() throws Exception {
        // Given
        when(classScheduleService.getClassById(999L))
            .thenThrow(new RuntimeException("Class not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/classes/999"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate create class request")
    void shouldValidateCreateClassRequest() throws Exception {
        // Given
        CreateClassRequest invalidRequest = new CreateClassRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(classScheduleService, never()).createClass(any());
    }
}
