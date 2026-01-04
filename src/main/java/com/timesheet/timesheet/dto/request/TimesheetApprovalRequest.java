package com.timesheet.timesheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetApprovalRequest {

    @NotNull(message = "Status is required")
    private String status; // APPROVED or REJECTED

    private String rejectionReason;
}
