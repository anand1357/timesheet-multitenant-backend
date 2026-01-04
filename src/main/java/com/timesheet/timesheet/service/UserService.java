package com.timesheet.timesheet.service;

import com.timesheet.timesheet.dto.request.UserRequest;
import com.timesheet.timesheet.dto.response.UserResponse;
import com.timesheet.timesheet.exception.BadRequestException;
import com.timesheet.timesheet.exception.ResourceNotFoundException;
import com.timesheet.timesheet.model.Tenant;
import com.timesheet.timesheet.model.User;
import com.timesheet.timesheet.repository.TenantRepository;
import com.timesheet.timesheet.repository.UserRepository;
import com.timesheet.timesheet.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public List<UserResponse> getAllUsers() {
        Long tenantId = TenantUtils.getCurrentTenantId();
        List<User> users = userRepository.findByTenantId(tenantId);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        // Check user limit
        Long userCount = userRepository.countByTenantId(tenantId);
        if (userCount >= tenant.getMaxUsers()) {
            throw new BadRequestException("User limit reached for your plan");
        }

        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.valueOf(request.getRole()))
                .hourlyRate(request.getHourlyRate())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        // Check if email is being changed and if it's already in use
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(User.Role.valueOf(request.getRole()));
        user.setHourlyRate(request.getHourlyRate());
        user.setPhoneNumber(request.getPhoneNumber());

        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        return convertToUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        // Soft delete - just deactivate
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        Long tenantId = TenantUtils.getCurrentTenantId();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("Access denied");
        }

        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);
        return convertToUserResponse(user);
    }

    public List<UserResponse> getActiveUsers() {
        Long tenantId = TenantUtils.getCurrentTenantId();
        List<User> users = userRepository.findActiveUsersByTenantId(tenantId);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setTenantId(user.getTenant().getId());
        response.setTenantName(user.getTenant().getName());
        return response;
    }
}