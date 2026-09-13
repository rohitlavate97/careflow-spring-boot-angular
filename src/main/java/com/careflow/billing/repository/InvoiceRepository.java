package com.careflow.billing.repository;

import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Invoice aggregate with pessimistic locking support (§29, §31, §57 Lab 5).
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the invoice row,
     * serializing concurrent payment settlements against this invoice (§57 Lab 5, §92).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") String id);

    Page<Invoice> findByPatientId(String patientId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    List<Invoice> findByEncounterId(String encounterId);

    List<Invoice> findByAdmissionId(String admissionId);
}
