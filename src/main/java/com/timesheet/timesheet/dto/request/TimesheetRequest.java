package com.timesheet.timesheet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Hours is required")
    @Positive(message = "Hours must be positive")
    private BigDecimal hours;

    private String description;

    private Boolean isBillable = true;
}
