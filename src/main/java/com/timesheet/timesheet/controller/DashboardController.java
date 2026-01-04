package com.timesheet.timesheet.controller;

import com.timesheet.timesheet.dto.response.ApiResponse;
import com.timesheet.timesheet.dto.response.DashboardStatsResponse;
import com.timesheet.timesheet.dto.response.HoursChartResponse;
import com.timesheet.timesheet.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/hours-chart")
    public ResponseEntity<ApiResponse<List<HoursChartResponse>>> getHoursChart(
            @RequestParam(defaultValue = "7") int days) {
        List<HoursChartResponse> chartData = dashboardService.getHoursChartData(days);
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }
}
