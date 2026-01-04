package com.timesheet.timesheet.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "subscription_plan")
    private String subscriptionPlan;

    @Column(name = "max_users")
    private Integer maxUsers;
}

