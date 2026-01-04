package com.timesheet.timesheet.repository;

import com.timesheet.timesheet.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.id = :invoiceId")
    Optional<Invoice> findByIdAndTenantId(@Param("invoiceId") Long invoiceId, @Param("tenantId") Long tenantId);

    @Query("SELECT i FROM Invoice i WHERE i.project.id = :projectId AND i.tenant.id = :tenantId")
    List<Invoice> findByProjectIdAndTenantId(@Param("projectId") Long projectId, @Param("tenantId") Long tenantId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.status = :status")
    List<Invoice> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Invoice.InvoiceStatus status);
}
