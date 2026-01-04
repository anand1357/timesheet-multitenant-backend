package com.timesheet.timesheet.repository;

import com.timesheet.timesheet.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByTenantId(Long tenantId, Pageable pageable);

    List<Project> findByTenantId(Long tenantId);

    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.id = :projectId")
    Optional<Project> findByIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Project p WHERE p.tenant.id = :tenantId AND p.status = :status")
    List<Project> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Project.ProjectStatus status);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenant.id = :tenantId")
    Long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenant.id = :tenantId AND p.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Project.ProjectStatus status);

    @Query("SELECT p FROM Project p JOIN p.members pm WHERE pm.user.id = :userId AND p.tenant.id = :tenantId")
    List<Project> findProjectsByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}