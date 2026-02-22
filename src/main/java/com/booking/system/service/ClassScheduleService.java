package com.booking.system.service;

import com.booking.system.dto.request.CreateClassRequest;
import com.booking.system.dto.request.UpdateClassRequest;
import com.booking.system.dto.response.ClassResponse;
import com.booking.system.entity.ClassSchedule;
import com.booking.system.entity.Instructor;
import com.booking.system.exception.BookingException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.InstructorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassScheduleService {

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime()) ||
            request.getEndTime().isEqual(request.getStartTime())) {
            throw new BookingException("End time must be after start time");
        }

        ClassSchedule classSchedule = new ClassSchedule();
        classSchedule.setName(request.getName());
        classSchedule.setDescription(request.getDescription());
        classSchedule.setStartTime(request.getStartTime());
        classSchedule.setEndTime(request.getEndTime());
        classSchedule.setCapacity(request.getCapacity());
        classSchedule.setCurrentBookings(0);
        classSchedule.setLocation(request.getLocation());
        classSchedule.setStatus("SCHEDULED");

        if (request.getInstructorId() != null) {
            Instructor instructor = instructorRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            classSchedule.setInstructor(instructor);
        }

        classSchedule = classScheduleRepository.save(classSchedule);
        return convertToResponse(classSchedule);
    }

    public ClassResponse getClassById(Long id) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
        return convertToResponse(classSchedule);
    }

    public List<ClassResponse> getAllClasses() {
        return classScheduleRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ClassResponse> getAvailableClasses() {
        return classScheduleRepository.findUpcomingClassesByStatus("SCHEDULED", LocalDateTime.now())
                .stream()
                .filter(cs -> cs.getCurrentBookings() < cs.getCapacity())
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ClassResponse> getClassesByStatus(String status) {
        return classScheduleRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ClassResponse> getClassesByInstructor(Long instructorId) {
        return classScheduleRepository.findByInstructorId(instructorId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (request.getName() != null) {
            classSchedule.setName(request.getName());
        }
        if (request.getDescription() != null) {
            classSchedule.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            classSchedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            classSchedule.setEndTime(request.getEndTime());
        }
        if (request.getCapacity() != null) {
            if (request.getCapacity() < classSchedule.getCurrentBookings()) {
                throw new BookingException("Cannot reduce capacity below current bookings");
            }
            classSchedule.setCapacity(request.getCapacity());
        }
        if (request.getLocation() != null) {
            classSchedule.setLocation(request.getLocation());
        }
        if (request.getStatus() != null) {
            classSchedule.setStatus(request.getStatus());
        }
        if (request.getInstructorId() != null) {
            Instructor instructor = instructorRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            classSchedule.setInstructor(instructor);
        }

        classSchedule = classScheduleRepository.save(classSchedule);
        return convertToResponse(classSchedule);
    }

    @Transactional
    public void deleteClass(Long id) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        if (classSchedule.getCurrentBookings() > 0) {
            classSchedule.setStatus("CANCELLED");
            classScheduleRepository.save(classSchedule);
        } else {
            classScheduleRepository.delete(classSchedule);
        }
    }

    private ClassResponse convertToResponse(ClassSchedule classSchedule) {
        ClassResponse response = new ClassResponse();
        response.setId(classSchedule.getId());
        response.setName(classSchedule.getName());
        response.setDescription(classSchedule.getDescription());
        response.setStartTime(classSchedule.getStartTime());
        response.setEndTime(classSchedule.getEndTime());
        response.setCapacity(classSchedule.getCapacity());
        response.setCurrentBookings(classSchedule.getCurrentBookings());
        response.setAvailableSpots(classSchedule.getCapacity() - classSchedule.getCurrentBookings());
        response.setLocation(classSchedule.getLocation());
        response.setStatus(classSchedule.getStatus());
        response.setCreatedAt(classSchedule.getCreatedAt());

        if (classSchedule.getInstructor() != null) {
            response.setInstructorId(classSchedule.getInstructor().getId());
            if (classSchedule.getInstructor().getUser() != null) {
                response.setInstructorName(
                    classSchedule.getInstructor().getUser().getFirstName() + " " +
                    classSchedule.getInstructor().getUser().getLastName()
                );
            }
        }

        return response;
    }
}
