package com.booking.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {

    private Long id;
    private String name;
    private String description;
    private Long instructorId;
    private String instructorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer currentBookings;
    private Integer availableSpots;
    private String location;
    private String status;
    private LocalDateTime createdAt;
}
