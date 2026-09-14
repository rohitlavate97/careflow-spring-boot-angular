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

    long countByStatusIn(java.util.Collection<LabOrderStatus> statuses);

    @Query("SELECT o.status, COUNT(o) FROM LabOrder o WHERE o.orderedAt >= :start AND o.orderedAt <= :end GROUP BY o.status")
    java.util.List<Object[]> countLabOrdersByStatus(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT o.orderedAt, o.completedAt FROM LabOrder o WHERE o.status = com.careflow.laboratory.domain.LabOrderStatus.COMPLETED " +
           "AND o.completedAt IS NOT NULL AND o.orderedAt >= :start AND o.orderedAt <= :end")
    java.util.List<Object[]> findCompletedLabOrderTimes(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT t.id, t.name, t.code, COUNT(i) FROM LabOrderItem i JOIN i.labTest t JOIN i.labOrder o " +
           "WHERE o.orderedAt >= :start AND o.orderedAt <= :end GROUP BY t.id, t.name, t.code ORDER BY COUNT(i) DESC")
    java.util.List<Object[]> findTopRequestedLabTests(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end, Pageable pageable);
}
