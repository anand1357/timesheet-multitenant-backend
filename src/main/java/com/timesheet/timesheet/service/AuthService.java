package com.timesheet.timesheet.service;

import com.timesheet.timesheet.dto.request.LoginRequest;
import com.timesheet.timesheet.dto.request.RegisterRequest;
import com.timesheet.timesheet.dto.response.AuthResponse;
import com.timesheet.timesheet.dto.response.UserResponse;
import com.timesheet.timesheet.exception.BadRequestException;
import com.timesheet.timesheet.model.Tenant;
import com.timesheet.timesheet.model.User;
import com.timesheet.timesheet.repository.TenantRepository;
import com.timesheet.timesheet.repository.UserRepository;
import com.timesheet.timesheet.security.JwtTokenProvider;
import com.timesheet.timesheet.security.UserPrincipal;
import com.timesheet.timesheet.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ModelMapper modelMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate input
        if (!ValidationUtils.isValidEmail(request.getEmail())) {
            throw new BadRequestException("Invalid email format");
        }

        if (!ValidationUtils.isValidSubdomain(request.getSubdomain())) {
            throw new BadRequestException("Invalid subdomain format. Use only lowercase letters, numbers, and hyphens");
        }

        if (!ValidationUtils.isValidPassword(request.getPassword())) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use");
        }

        // Check if subdomain already exists
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BadRequestException("Subdomain already taken");
        }

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(request.getOrganizationName())
                .subdomain(request.getSubdomain())
                .isActive(true)
                .subscriptionPlan("FREE")
                .maxUsers(10)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create admin user
        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.ADMIN)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // Generate tokens
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create(user), null, UserPrincipal.create(user).getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Convert to response
        UserResponse userResponse = convertToUserResponse(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate tokens
        String token = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Get user details
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserResponse userResponse = convertToUserResponse(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return convertToUserResponse(user);
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setTenantId(user.getTenant().getId());
        response.setTenantName(user.getTenant().getName());
        return response;
    }
}
