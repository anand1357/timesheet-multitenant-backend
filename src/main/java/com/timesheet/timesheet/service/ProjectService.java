package com.timesheet.timesheet.service;

import com.timesheet.timesheet.dto.request.ProjectRequest;
import com.timesheet.timesheet.dto.response.ProjectMemberResponse;
import com.timesheet.timesheet.dto.response.ProjectResponse;
import com.timesheet.timesheet.dto.response.UserResponse;
import com.timesheet.timesheet.exception.BadRequestException;
import com.timesheet.timesheet.exception.ResourceNotFoundException;
import com.timesheet.timesheet.model.*;
import com.timesheet.timesheet.repository.*;
import com.timesheet.timesheet.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TimesheetRepository timesheetRepository;
    private final ModelMapper modelMapper;

    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Page<Project> projects = projectRepository.findByTenantId(tenantId, pageable);
        return projects.map(this::convertToProjectResponse);
    }

    public List<ProjectResponse> getAllActiveProjects() {
        Long tenantId = TenantUtils.getCurrentTenantId();
        List<Project> projects = projectRepository.findByTenantIdAndStatus(
                tenantId, Project.ProjectStatus.ACTIVE);
        return projects.stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        return convertToProjectResponse(project);
    }

    public List<ProjectResponse> getMyProjects() {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        List<Project> projects = projectRepository.findProjectsByUserIdAndTenantId(userId, tenantId);
        return projects.stream()
                .map(this::convertToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        Long userId = TenantUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        User createdBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Project project = Project.builder()
                .tenant(tenant)
                .name(request.getName())
                .description(request.getDescription())
                .clientName(request.getClientName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .budget(request.getBudget())
                .status(Project.ProjectStatus.valueOf(
                        request.getStatus() != null ? request.getStatus() : "ACTIVE"))
                .colorCode(request.getColorCode())
                .createdBy(createdBy)
                .members(new HashSet<>())
                .build();

        project = projectRepository.save(project);

        // Add members if provided
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                addMemberToProject(project.getId(), memberId);
            }
        }

        return convertToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setClientName(request.getClientName());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setBudget(request.getBudget());
        project.setColorCode(request.getColorCode());

        if (request.getStatus() != null) {
            project.setStatus(Project.ProjectStatus.valueOf(request.getStatus()));
        }

        project = projectRepository.save(project);
        return convertToProjectResponse(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        projectRepository.delete(project);
    }

    @Transactional
    public void addMemberToProject(Long projectId, Long userId) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("User does not belong to this tenant");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BadRequestException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectMember.ProjectRole.MEMBER)
                .build();

        projectMemberRepository.save(member);
    }

    @Transactional
    public void removeMemberFromProject(Long projectId, Long userId) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    private ProjectResponse convertToProjectResponse(Project project) {
        ProjectResponse response = modelMapper.map(project, ProjectResponse.class);

        // Convert members
        List<ProjectMemberResponse> members = project.getMembers().stream()
                .map(this::convertToProjectMemberResponse)
                .collect(Collectors.toList());
        response.setMembers(members);

        // Calculate total hours and cost
        BigDecimal totalHours = timesheetRepository.sumApprovedHoursByProjectId(project.getId());
        response.setTotalHours(totalHours != null ? totalHours : BigDecimal.ZERO);

        // Calculate total cost based on hourly rates
        BigDecimal totalCost = calculateProjectCost(project.getId());
        response.setTotalCost(totalCost);

        return response;
    }

    private ProjectMemberResponse convertToProjectMemberResponse(ProjectMember member) {
        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setId(member.getId());
        response.setRole(member.getRole().name());
        response.setAssignedAt(member.getAssignedAt());

        UserResponse userResponse = modelMapper.map(member.getUser(), UserResponse.class);
        response.setUser(userResponse);

        return response;
    }

    private BigDecimal calculateProjectCost(Long projectId) {
        List<Timesheet> timesheets = timesheetRepository.findByProjectIdAndTenantId(
                projectId, TenantUtils.getCurrentTenantId());

        return timesheets.stream()
                .filter(t -> t.getStatus() == Timesheet.TimesheetStatus.APPROVED)
                .filter(t -> t.getIsBillable())
                .map(t -> {
                    BigDecimal hours = t.getHours();
                    BigDecimal rate = t.getUser().getHourlyRate();
                    return rate != null ? hours.multiply(rate) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
