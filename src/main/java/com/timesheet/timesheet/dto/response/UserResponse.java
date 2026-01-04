package com.timesheet.timesheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private BigDecimal hourlyRate;
    private Boolean isActive;
    private String phoneNumber;
    private String avatarUrl;
    private Long tenantId;
    private String tenantName;
    private LocalDateTime createdAt;
}
