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
public class HoursChartResponse {
    private String name;  // Day name (Mon, Tue, etc.)
    private String date;  // Actual date (2026-01-06)
    private BigDecimal hours;
}
