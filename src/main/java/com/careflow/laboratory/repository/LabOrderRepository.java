package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for diagnostic lab orders (§27, §96).
 */
@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, String> {

    Optional<LabOrder> findByOrderNumber(String orderNumber);

    Page<LabOrder> findByPatientIdOrderByOrderedAtDesc(String patientId, Pageable pageable);

    Page<LabOrder> findByOrderingDoctorIdOrderByOrderedAtDesc(String orderingDoctorId, Pageable pageable);

    Page<LabOrder> findByStatusOrderByOrderedAtDesc(LabOrderStatus status, Pageable pageable);

    Page<LabOrder> findByReviewStatusOrderByOrderedAtDesc(LabReviewStatus reviewStatus, Pageable pageable);

    @Query("SELECT o FROM LabOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.labTest WHERE o.id = :id")
    Optional<LabOrder> findByIdWithDetails(@Param("id") String id);
}
