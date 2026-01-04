package com.timesheet.timesheet.service;

import com.timesheet.timesheet.dto.request.TimesheetApprovalRequest;
import com.timesheet.timesheet.dto.request.TimesheetRequest;
import com.timesheet.timesheet.dto.response.TimesheetResponse;
import com.timesheet.timesheet.exception.BadRequestException;
import com.timesheet.timesheet.exception.ResourceNotFoundException;
import com.timesheet.timesheet.model.Project;
import com.timesheet.timesheet.model.Tenant;
import com.timesheet.timesheet.model.Timesheet;
import com.timesheet.timesheet.model.User;
import com.timesheet.timesheet.repository.ProjectRepository;
import com.timesheet.timesheet.repository.TenantRepository;
import com.timesheet.timesheet.repository.TimesheetRepository;
import com.timesheet.timesheet.repository.UserRepository;
import com.timesheet.timesheet.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final ModelMapper modelMapper;

    public Page<TimesheetResponse> getAllTimesheets(Pageable pageable) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Page<Timesheet> timesheets = timesheetRepository.findByTenantId(tenantId, pageable);
        return timesheets.map(this::convertToTimesheetResponse);
    }

    public Page<TimesheetResponse> getMyTimesheets(Pageable pageable) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        Page<Timesheet> timesheets = timesheetRepository.findByUserIdAndTenantId(
                userId, tenantId, pageable);
        return timesheets.map(this::convertToTimesheetResponse);
    }

    public Page<TimesheetResponse> getPendingTimesheets(Pageable pageable) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Page<Timesheet> timesheets = timesheetRepository.findByTenantIdAndStatus(
                tenantId, Timesheet.TimesheetStatus.PENDING, pageable);
        return timesheets.map(this::convertToTimesheetResponse);
    }

    public TimesheetResponse getTimesheetById(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Timesheet timesheet = timesheetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        return convertToTimesheetResponse(timesheet);
    }

    @Transactional
    public TimesheetResponse createTimesheet(TimesheetRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Project project = projectRepository.findByIdAndTenantId(request.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        // Validate hours
        if (request.getHours().doubleValue() <= 0 || request.getHours().doubleValue() > 24) {
            throw new BadRequestException("Hours must be between 0 and 24");
        }

        Timesheet timesheet = Timesheet.builder()
                .tenant(tenant)
                .user(user)
                .project(project)
                .date(request.getDate())
                .hours(request.getHours())
                .description(request.getDescription())
                .isBillable(request.getIsBillable())
                .status(Timesheet.TimesheetStatus.PENDING)
                .build();

        timesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetResponse(timesheet);
    }

    @Transactional
    public TimesheetResponse updateTimesheet(Long id, TimesheetRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        Timesheet timesheet = timesheetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        // Only allow user to update their own timesheets
        if (!timesheet.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only update your own timesheets");
        }

        // Only allow updates if status is PENDING
        if (timesheet.getStatus() != Timesheet.TimesheetStatus.PENDING) {
            throw new BadRequestException("Cannot update timesheet that has been approved or rejected");
        }

        Project project = projectRepository.findByIdAndTenantId(request.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        if (request.getHours().doubleValue() <= 0 || request.getHours().doubleValue() > 24) {
            throw new BadRequestException("Hours must be between 0 and 24");
        }

        timesheet.setProject(project);
        timesheet.setDate(request.getDate());
        timesheet.setHours(request.getHours());
        timesheet.setDescription(request.getDescription());
        timesheet.setIsBillable(request.getIsBillable());

        timesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetResponse(timesheet);
    }

    @Transactional
    public void deleteTimesheet(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        Timesheet timesheet = timesheetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        // Only allow user to delete their own timesheets
        if (!timesheet.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own timesheets");
        }

        // Only allow deletion if status is PENDING
        if (timesheet.getStatus() != Timesheet.TimesheetStatus.PENDING) {
            throw new BadRequestException("Cannot delete timesheet that has been approved or rejected");
        }

        timesheetRepository.delete(timesheet);
    }

    @Transactional
    public TimesheetResponse approveOrRejectTimesheet(Long id, TimesheetApprovalRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long approverId = TenantUtils.getCurrentUserId();

        Timesheet timesheet = timesheetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", approverId));

        // Only managers and admins can approve
        if (approver.getRole() != User.Role.MANAGER &&
                approver.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Only managers and admins can approve timesheets");
        }

        Timesheet.TimesheetStatus newStatus = Timesheet.TimesheetStatus.valueOf(request.getStatus());

        timesheet.setStatus(newStatus);
        timesheet.setApprovedBy(approver);
        timesheet.setApprovedAt(LocalDateTime.now());

        if (newStatus == Timesheet.TimesheetStatus.REJECTED) {
            timesheet.setRejectionReason(request.getRejectionReason());
        }

        timesheet = timesheetRepository.save(timesheet);
        return convertToTimesheetResponse(timesheet);
    }

    private TimesheetResponse convertToTimesheetResponse(Timesheet timesheet) {
        TimesheetResponse response = modelMapper.map(timesheet, TimesheetResponse.class);
        // The nested objects will be mapped automatically by ModelMapper
        return response;
    }
}
