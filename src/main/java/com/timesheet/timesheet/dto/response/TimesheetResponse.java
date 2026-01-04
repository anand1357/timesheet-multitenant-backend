package com.timesheet.timesheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetResponse {
    private Long id;
    private UserResponse user;
    private ProjectResponse project;
    private LocalDate date;
    private BigDecimal hours;
    private String description;
    private Boolean isBillable;
    private String status;
    private UserResponse approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
