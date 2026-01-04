package com.timesheet.timesheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private Long totalProjects;
    private Long activeProjects;
    private Long totalUsers;
    private BigDecimal totalHoursThisMonth;
    private BigDecimal totalHoursLastMonth;
    private Long pendingTimesheets;
}
