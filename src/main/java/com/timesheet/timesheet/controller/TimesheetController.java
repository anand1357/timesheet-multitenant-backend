package com.timesheet.timesheet.controller;

import com.timesheet.timesheet.dto.request.TimesheetApprovalRequest;
import com.timesheet.timesheet.dto.request.TimesheetRequest;
import com.timesheet.timesheet.dto.response.ApiResponse;
import com.timesheet.timesheet.dto.response.PageResponse;
import com.timesheet.timesheet.dto.response.TimesheetResponse;
import com.timesheet.timesheet.service.TimesheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TimesheetController {

    private final TimesheetService timesheetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<TimesheetResponse>>> getAllTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TimesheetResponse> timesheetPage = timesheetService.getAllTimesheets(pageable);

        PageResponse<TimesheetResponse> pageResponse = PageResponse.<TimesheetResponse>builder()
                .content(timesheetPage.getContent())
                .pageNumber(timesheetPage.getNumber())
                .pageSize(timesheetPage.getSize())
                .totalElements(timesheetPage.getTotalElements())
                .totalPages(timesheetPage.getTotalPages())
                .last(timesheetPage.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/my-timesheets")
    public ResponseEntity<ApiResponse<PageResponse<TimesheetResponse>>> getMyTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TimesheetResponse> timesheetPage = timesheetService.getMyTimesheets(pageable);

        PageResponse<TimesheetResponse> pageResponse = PageResponse.<TimesheetResponse>builder()
                .content(timesheetPage.getContent())
                .pageNumber(timesheetPage.getNumber())
                .pageSize(timesheetPage.getSize())
                .totalElements(timesheetPage.getTotalElements())
                .totalPages(timesheetPage.getTotalPages())
                .last(timesheetPage.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<TimesheetResponse>>> getPendingTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<TimesheetResponse> timesheetPage = timesheetService.getPendingTimesheets(pageable);

        PageResponse<TimesheetResponse> pageResponse = PageResponse.<TimesheetResponse>builder()
                .content(timesheetPage.getContent())
                .pageNumber(timesheetPage.getNumber())
                .pageSize(timesheetPage.getSize())
                .totalElements(timesheetPage.getTotalElements())
                .totalPages(timesheetPage.getTotalPages())
                .last(timesheetPage.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TimesheetResponse>> getTimesheetById(@PathVariable Long id) {
        TimesheetResponse timesheet = timesheetService.getTimesheetById(id);
        return ResponseEntity.ok(ApiResponse.success(timesheet));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TimesheetResponse>> createTimesheet(
            @Valid @RequestBody TimesheetRequest request) {
        TimesheetResponse timesheet = timesheetService.createTimesheet(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Timesheet created successfully", timesheet));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TimesheetResponse>> updateTimesheet(
            @PathVariable Long id, @Valid @RequestBody TimesheetRequest request) {
        TimesheetResponse timesheet = timesheetService.updateTimesheet(id, request);
        return ResponseEntity.ok(ApiResponse.success("Timesheet updated successfully", timesheet));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTimesheet(@PathVariable Long id) {
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.ok(ApiResponse.success("Timesheet deleted successfully", null));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TimesheetResponse>> approveOrRejectTimesheet(
            @PathVariable Long id, @Valid @RequestBody TimesheetApprovalRequest request) {
        TimesheetResponse timesheet = timesheetService.approveOrRejectTimesheet(id, request);
        return ResponseEntity.ok(ApiResponse.success("Timesheet status updated", timesheet));
    }
}
