package com.booking.system.integration;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.entity.Instructor;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.InstructorRepository;
import com.booking.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Class Management Integration Tests")
class ClassIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    private String userToken;
    private String adminToken;
    private String instructorToken;
    private Long instructorId1;
    private Long instructorId2;
    private Long scheduledClassId;
    private Long cancelledClassId;
    private Long pastClassId;
    private Long fullClassId;
    private Long instructorSpecificClassId;

    @BeforeEach
    void setUp() throws Exception {
        // Create regular user
        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setUsername("regularuser");
        userRequest.setEmail("user@example.com");
        userRequest.setPassword("password123");
        userRequest.setFirstName("Regular");
        userRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
            .andExpect(status().isCreated());

        // Login as regular user
        LoginRequest userLogin = new LoginRequest();
        userLogin.setEmail("user@example.com");
        userLogin.setPassword("password123");

        MvcResult userResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("token").asText();

        // Create admin user
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("adminuser");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password123");
        adminRequest.setFirstName("Admin");
        adminRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
            .andExpect(status().isCreated());

        // Promote to admin
        var adminUser = userRepository.findByEmail("admin@example.com").get();
        adminUser.setRole("ROLE_ADMIN");
        userRepository.save(adminUser);

        // Login as admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@example.com");
        adminLogin.setPassword("password123");

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
            .andExpect(status().isOk())
            .andReturn();

        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();

        // Create first instructor user
        RegisterRequest instructorRequest1 = new RegisterRequest();
        instructorRequest1.setUsername("instructor1");
        instructorRequest1.setEmail("instructor1@example.com");
        instructorRequest1.setPassword("password123");
        instructorRequest1.setFirstName("Instructor");
        instructorRequest1.setLastName("One");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instructorRequest1)))
            .andExpect(status().isCreated());

        // Promote to instructor and create Instructor entity
        var instructorUser1 = userRepository.findByEmail("instructor1@example.com").get();
        instructorUser1.setRole("ROLE_INSTRUCTOR");
        userRepository.save(instructorUser1);

        Instructor instructor1 = new Instructor();
        instructor1.setUser(instructorUser1);
        instructor1.setBio("Experienced yoga instructor");
        instructor1.setSpecialization("Yoga");
        instructorRepository.save(instructor1);
        instructorId1 = instructor1.getId();

        // Create second instructor user
        RegisterRequest instructorRequest2 = new RegisterRequest();
        instructorRequest2.setUsername("instructor2");
        instructorRequest2.setEmail("instructor2@example.com");
        instructorRequest2.setPassword("password123");
        instructorRequest2.setFirstName("Instructor");
        instructorRequest2.setLastName("Two");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instructorRequest2)))
            .andExpect(status().isCreated());

        // Promote to instructor and create Instructor entity
        var instructorUser2 = userRepository.findByEmail("instructor2@example.com").get();
        instructorUser2.setRole("ROLE_INSTRUCTOR");
        userRepository.save(instructorUser2);

        Instructor instructor2 = new Instructor();
        instructor2.setUser(instructorUser2);
        instructor2.setBio("Pilates specialist");
        instructor2.setSpecialization("Pilates");
        instructorRepository.save(instructor2);
        instructorId2 = instructor2.getId();

        // Login as first instructor
        LoginRequest instructorLogin = new LoginRequest();
        instructorLogin.setEmail("instructor1@example.com");
        instructorLogin.setPassword("password123");

        MvcResult instructorResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(instructorLogin)))
            .andExpect(status().isOk())
            .andReturn();

        instructorToken = objectMapper.readTree(instructorResult.getResponse().getContentAsString()).get("token").asText();

        // Create various test classes as admin
        LocalDateTime now = LocalDateTime.now();

        // 1. Scheduled class (future, available)
        String scheduledClassJson = String.format(
            "{\"name\":\"Scheduled Yoga\",\"description\":\"Morning yoga class\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":10,\"location\":\"Studio A\"}",
            instructorId1,
            now.plusDays(1),
            now.plusDays(1).plusHours(1)
        );

        MvcResult scheduledResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduledClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        scheduledClassId = objectMapper.readTree(scheduledResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Cancelled class
        String cancelledClassJson = String.format(
            "{\"name\":\"Cancelled Pilates\",\"description\":\"Cancelled class\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":5,\"location\":\"Studio B\"}",
            instructorId2,
            now.plusDays(2),
            now.plusDays(2).plusHours(1)
        );

        MvcResult cancelledResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancelledClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long cancelledClassTempId = objectMapper.readTree(cancelledResult.getResponse().getContentAsString()).get("id").asLong();

        // Add a booking to the class so it gets marked as CANCELLED instead of deleted
        var cancelledClass = classScheduleRepository.findById(cancelledClassTempId).get();
        cancelledClass.setCurrentBookings(1);
        classScheduleRepository.save(cancelledClass);

        // Cancel the class
        mockMvc.perform(delete("/api/v1/classes/" + cancelledClassTempId)
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf()))
            .andExpect(status().isNoContent());

        cancelledClassId = cancelledClassTempId;

        // 3. Past class (already started)
        String pastClassJson = String.format(
            "{\"name\":\"Past Meditation\",\"description\":\"Already started class\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":8,\"location\":\"Studio C\"}",
            now.minusDays(1),
            now.minusDays(1).plusHours(1)
        );

        // Need to bypass validation for past date - create directly via repository
        // Or update the class start time after creation
        MvcResult pastResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(pastClassJson))
            .andExpect(status().isBadRequest()) // Should fail due to @Future validation
            .andReturn();

        // Create past class by directly updating after creation with future time
        String futureClassJson = String.format(
            "{\"name\":\"Past Meditation\",\"description\":\"Already started class\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":8,\"location\":\"Studio C\"}",
            now.plusDays(3),
            now.plusDays(3).plusHours(1)
        );

        MvcResult futureResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(futureClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long futureClassId = objectMapper.readTree(futureResult.getResponse().getContentAsString()).get("id").asLong();

        // Update to past time via repository
        var pastClass = classScheduleRepository.findById(futureClassId).get();
        pastClass.setStartTime(now.minusDays(1));
        pastClass.setEndTime(now.minusDays(1).plusHours(1));
        classScheduleRepository.save(pastClass);
        pastClassId = futureClassId;

        // 4. Full class (currentBookings = capacity)
        String fullClassJson = String.format(
            "{\"name\":\"Full Zumba\",\"description\":\"Popular Zumba class\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":3,\"location\":\"Studio D\"}",
            instructorId1,
            now.plusDays(4),
            now.plusDays(4).plusHours(1)
        );

        MvcResult fullResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(fullClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        Long fullClassTempId = objectMapper.readTree(fullResult.getResponse().getContentAsString()).get("id").asLong();

        // Update to be full
        var fullClass = classScheduleRepository.findById(fullClassTempId).get();
        fullClass.setCurrentBookings(3); // capacity is 3
        classScheduleRepository.save(fullClass);
        fullClassId = fullClassTempId;

        // 5. Class with specific instructor for filtering test
        String instructorClassJson = String.format(
            "{\"name\":\"Instructor2 Special\",\"description\":\"Special class by instructor 2\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":15,\"location\":\"Main Hall\"}",
            instructorId2,
            now.plusDays(5),
            now.plusDays(5).plusHours(2)
        );

        MvcResult instructorClassResult = mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(instructorClassJson))
            .andExpect(status().isCreated())
            .andReturn();

        instructorSpecificClassId = objectMapper.readTree(instructorClassResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        classScheduleRepository.deleteAll();
        instructorRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Public should get all classes")
    void publicShouldGetAllClasses() throws Exception {
        // When & Then - No authentication required
        mockMvc.perform(get("/api/v1/classes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5)) // All 5 classes created
            .andExpect(jsonPath("$[?(@.name == 'Scheduled Yoga')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Cancelled Pilates')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Past Meditation')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Full Zumba')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Instructor2 Special')]").exists());
    }

    @Test
    @DisplayName("Should filter classes by status")
    void shouldFilterClassesByStatus() throws Exception {
        // Filter by SCHEDULED status
        mockMvc.perform(get("/api/v1/classes")
                .param("status", "SCHEDULED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4)) // Scheduled Yoga, Past Meditation, Full Zumba, Instructor2 Special
            .andExpect(jsonPath("$[?(@.name == 'Scheduled Yoga')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Past Meditation')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Full Zumba')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Instructor2 Special')]").exists());

        // Filter by CANCELLED status
        mockMvc.perform(get("/api/v1/classes")
                .param("status", "CANCELLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Cancelled Pilates"));
    }

    @Test
    @DisplayName("Should filter classes by instructor")
    void shouldFilterClassesByInstructor() throws Exception {
        // Filter by instructorId1
        mockMvc.perform(get("/api/v1/classes")
                .param("instructorId", instructorId1.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)) // Scheduled Yoga, Full Zumba
            .andExpect(jsonPath("$[?(@.name == 'Scheduled Yoga')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Full Zumba')]").exists());

        // Filter by instructorId2
        mockMvc.perform(get("/api/v1/classes")
                .param("instructorId", instructorId2.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)) // Cancelled Pilates, Instructor2 Special
            .andExpect(jsonPath("$[?(@.name == 'Cancelled Pilates')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Instructor2 Special')]").exists());
    }

    @Test
    @DisplayName("Should get only available classes")
    void shouldGetOnlyAvailableClasses() throws Exception {
        // availableOnly = true should exclude: cancelled, past, and full classes
        mockMvc.perform(get("/api/v1/classes")
                .param("availableOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)) // Scheduled Yoga, Instructor2 Special
            .andExpect(jsonPath("$[?(@.name == 'Scheduled Yoga')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Instructor2 Special')]").exists())
            .andExpect(jsonPath("$[?(@.status == 'SCHEDULED')]").exists()) // All should be SCHEDULED
            .andExpect(jsonPath("$[?(@.currentBookings < @.capacity)]").exists()); // All should have available spots
    }

    @Test
    @DisplayName("Admin should create new class")
    void adminShouldCreateNewClass() throws Exception {
        // Given
        String newClassJson = String.format(
            "{\"name\":\"New Aerobics\",\"description\":\"New aerobics class\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":20,\"location\":\"Studio E\"}",
            instructorId1,
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(10).plusHours(1)
        );

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("New Aerobics"))
            .andExpect(jsonPath("$.capacity").value(20))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.currentBookings").value(0));
    }

    @Test
    @DisplayName("Instructor should create new class")
    void instructorShouldCreateNewClass() throws Exception {
        // Given
        String newClassJson = String.format(
            "{\"name\":\"Instructor's Yoga\",\"description\":\"Yoga by instructor\"," +
            "\"instructorId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":12,\"location\":\"Private Studio\"}",
            instructorId1,
            LocalDateTime.now().plusDays(11),
            LocalDateTime.now().plusDays(11).plusHours(1)
        );

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Instructor's Yoga"))
            .andExpect(jsonPath("$.instructorId").value(instructorId1));
    }

    @Test
    @DisplayName("Regular user should not create class")
    void regularUserShouldNotCreateClass() throws Exception {
        // Given
        String newClassJson = String.format(
            "{\"name\":\"User's Class\",\"description\":\"Attempt by user\"," +
            "\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":5,\"location\":\"Studio\"}",
            LocalDateTime.now().plusDays(12),
            LocalDateTime.now().plusDays(12).plusHours(1)
        );

        // When & Then
        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + userToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(newClassJson))
            .andExpect(status().isForbidden()); // USER role not allowed
    }

    @Test
    @DisplayName("Admin should update class")
    void adminShouldUpdateClass() throws Exception {
        // Given
        String updateJson = "{\"name\":\"Updated Yoga\",\"description\":\"Updated description\"," +
            "\"capacity\":15,\"location\":\"Updated Studio\"}";

        // When & Then
        mockMvc.perform(put("/api/v1/classes/" + scheduledClassId)
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Yoga"))
            .andExpect(jsonPath("$.capacity").value(15))
            .andExpect(jsonPath("$.location").value("Updated Studio"));
    }

    @Test
    @DisplayName("Instructor should update class")
    void instructorShouldUpdateClass() throws Exception {
        // Given - Instructor can update their own class
        String updateJson = "{\"description\":\"Updated by instructor\"}";

        // When & Then
        mockMvc.perform(put("/api/v1/classes/" + scheduledClassId) // Created by instructor1
                .header("Authorization", "Bearer " + instructorToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Updated by instructor"));
    }



    @Test
    @DisplayName("Regular user should not cancel class")
    void regularUserShouldNotCancelClass() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/classes/" + scheduledClassId)
                .header("Authorization", "Bearer " + userToken)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get class by ID")
    void shouldGetClassById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/classes/" + scheduledClassId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(scheduledClassId))
            .andExpect(jsonPath("$.name").value("Scheduled Yoga"))
            .andExpect(jsonPath("$.capacity").value(10))
            .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent class")
    void shouldReturn404ForNonExistentClass() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/classes/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should validate required fields when creating class")
    void shouldValidateRequiredFieldsWhenCreatingClass() throws Exception {
        // Missing required fields
        String invalidJson = "{\"description\":\"Missing name and times\"}";

        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate future start time when creating class")
    void shouldValidateFutureStartTimeWhenCreatingClass() throws Exception {
        // Past start time
        String invalidJson = String.format(
            "{\"name\":\"Invalid Class\",\"startTime\":\"%s\",\"endTime\":\"%s\"," +
            "\"capacity\":10,\"location\":\"Studio\"}",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusHours(1)
        );

        mockMvc.perform(post("/api/v1/classes")
                .header("Authorization", "Bearer " + adminToken)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
}