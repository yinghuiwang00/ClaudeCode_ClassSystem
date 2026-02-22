package com.booking.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private Long classScheduleId;
    private String className;
    private LocalDateTime classStartTime;
    private String bookingStatus;
    private LocalDateTime bookingDate;
    private LocalDateTime cancellationDate;
    private String notes;
}
