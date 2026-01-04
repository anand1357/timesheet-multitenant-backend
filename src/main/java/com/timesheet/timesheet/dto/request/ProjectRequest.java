package com.timesheet.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private String clientName;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal budget;

    private String status;

    private String colorCode;

    private List<Long> memberIds;
}

