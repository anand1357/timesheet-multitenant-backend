package com.timesheet.timesheet.repository;

import com.timesheet.timesheet.model.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    Page<Timesheet> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT t FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.id = :timesheetId")
    Optional<Timesheet> findByIdAndTenantId(@Param("timesheetId") Long timesheetId, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM Timesheet t WHERE t.user.id = :userId AND t.tenant.id = :tenantId")
    Page<Timesheet> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM Timesheet t WHERE t.project.id = :projectId AND t.tenant.id = :tenantId")
    List<Timesheet> findByProjectIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM Timesheet t WHERE t.user.id = :userId AND t.date = :date AND t.tenant.id = :tenantId")
    List<Timesheet> findByUserIdAndDateAndTenantId(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.status = :status")
    Page<Timesheet> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Timesheet.TimesheetStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Timesheet.TimesheetStatus status);

    @Query("SELECT SUM(t.hours) FROM Timesheet t WHERE t.user.id = :userId AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumHoursByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.hours) FROM Timesheet t WHERE t.project.id = :projectId AND t.status = 'APPROVED'")
    BigDecimal sumApprovedHoursByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT SUM(t.hours) FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumHoursByTenantIdAndDateBetween(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

//    @Query("SELECT t FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.date BETWEEN :startDate AND :endDate")
//    List<Timesheet> findByTenantIdAndDateBetween(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Timesheet t WHERE t.tenant.id = :tenantId AND t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findByTenantIdAndDateBetween(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
