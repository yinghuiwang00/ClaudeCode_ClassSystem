package com.booking.system.controller;

import com.booking.system.dto.request.CreateClassRequest;
import com.booking.system.dto.request.UpdateClassRequest;
import com.booking.system.dto.response.ClassResponse;
import com.booking.system.service.ClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@Tag(name = "Class Management", description = "Class schedule management APIs")
public class ClassController {

    @Autowired
    private ClassScheduleService classScheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a new class (Admin/Instructor only)")
    public ResponseEntity<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
        ClassResponse response = classScheduleService.createClass(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get class by ID")
    public ResponseEntity<ClassResponse> getClassById(@PathVariable Long id) {
        ClassResponse response = classScheduleService.getClassById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all classes or filter by status/instructor")
    public ResponseEntity<List<ClassResponse>> getClasses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false, defaultValue = "false") Boolean availableOnly) {

        List<ClassResponse> classes;

        if (availableOnly) {
            classes = classScheduleService.getAvailableClasses();
        } else if (status != null) {
            classes = classScheduleService.getClassesByStatus(status);
        } else if (instructorId != null) {
            classes = classScheduleService.getClassesByInstructor(instructorId);
        } else {
            classes = classScheduleService.getAllClasses();
        }

        return ResponseEntity.ok(classes);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update a class (Admin/Instructor only)")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {
        ClassResponse response = classScheduleService.updateClass(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cancel/Delete a class (Admin/Instructor only)")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classScheduleService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
}
