package com.booking.system.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClassRequest {

    private String name;
    private String description;
    private Long instructorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    private String location;
    private String status;
}
