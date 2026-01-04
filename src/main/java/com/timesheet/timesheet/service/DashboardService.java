package com.timesheet.timesheet.service;

import com.timesheet.timesheet.dto.response.DashboardStatsResponse;
import com.timesheet.timesheet.dto.response.HoursChartResponse;
import com.timesheet.timesheet.model.Project;
import com.timesheet.timesheet.model.Timesheet;
import com.timesheet.timesheet.repository.ProjectRepository;
import com.timesheet.timesheet.repository.TimesheetRepository;
import com.timesheet.timesheet.repository.UserRepository;
import com.timesheet.timesheet.util.DateUtils;
import com.timesheet.timesheet.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TimesheetRepository timesheetRepository;

    public DashboardStatsResponse getDashboardStats() {
        Long tenantId = TenantUtils.getCurrentTenantId();

        // Get total projects
        Long totalProjects = projectRepository.countByTenantId(tenantId);

        // Get active projects
        Long activeProjects = projectRepository.countByTenantIdAndStatus(
                tenantId, Project.ProjectStatus.ACTIVE);

        // Get total users
        Long totalUsers = userRepository.countByTenantId(tenantId);

        // Get total hours this month
        LocalDate firstDayOfMonth = DateUtils.getFirstDayOfMonth();
        LocalDate lastDayOfMonth = DateUtils.getLastDayOfMonth();
        BigDecimal totalHoursThisMonth = timesheetRepository.sumHoursByTenantIdAndDateBetween(
                tenantId, firstDayOfMonth, lastDayOfMonth);

        // Get total hours last month
        LocalDate firstDayOfPreviousMonth = DateUtils.getFirstDayOfPreviousMonth();
        LocalDate lastDayOfPreviousMonth = DateUtils.getLastDayOfPreviousMonth();
        BigDecimal totalHoursLastMonth = timesheetRepository.sumHoursByTenantIdAndDateBetween(
                tenantId, firstDayOfPreviousMonth, lastDayOfPreviousMonth);

        // Get pending timesheets
        Long pendingTimesheets = timesheetRepository.countByTenantIdAndStatus(
                tenantId, Timesheet.TimesheetStatus.PENDING);

        return DashboardStatsResponse.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .totalUsers(totalUsers)
                .totalHoursThisMonth(totalHoursThisMonth != null ? totalHoursThisMonth : BigDecimal.ZERO)
                .totalHoursLastMonth(totalHoursLastMonth != null ? totalHoursLastMonth : BigDecimal.ZERO)
                .pendingTimesheets(pendingTimesheets)
                .build();
    }

    public List<HoursChartResponse> getHoursChartData(int days) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        // Get date range
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // Get timesheets for date range
        List<Timesheet> timesheets = timesheetRepository.findByTenantIdAndDateBetween(
                tenantId, startDate, endDate);

        // Filter by current user for "my hours"
        List<Timesheet> userTimesheets = timesheets.stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        // Group by date and sum hours
        Map<LocalDate, BigDecimal> hoursByDate = userTimesheets.stream()
                .collect(Collectors.groupingBy(
                        Timesheet::getDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Timesheet::getHours,
                                BigDecimal::add
                        )
                ));

        // Create chart data for all days in range
        List<HoursChartResponse> chartData = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            BigDecimal hours = hoursByDate.getOrDefault(date, BigDecimal.ZERO);

            chartData.add(HoursChartResponse.builder()
                    .name(dayName)
                    .date(date.format(dateFormatter))
                    .hours(hours)
                    .build());
        }

        return chartData;
    }
}