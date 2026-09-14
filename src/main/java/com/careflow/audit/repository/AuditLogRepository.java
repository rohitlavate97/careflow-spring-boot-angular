package com.careflow.audit.repository;

import com.careflow.audit.domain.AuditLog;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for immutable audit logs (§36, §74, §103 Phase 15).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByPatientIdOrderByTimestampDesc(String patientId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceIdOrderByTimestampDesc(
            AuditResourceType resourceType, String resourceId, Pageable pageable);

    Page<AuditLog> findByActorUserIdOrderByTimestampDesc(String actorUserId, Pageable pageable);

    long countByTimestampBetween(Instant from, Instant to);

    @Query("SELECT a.action as action, COUNT(a) as count FROM AuditLog a " +
           "WHERE a.timestamp >= :from AND a.timestamp <= :to GROUP BY a.action")
    List<AuditCountByAction> countGroupedByAction(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a.resourceType as resourceType, COUNT(a) as count FROM AuditLog a " +
           "WHERE a.timestamp >= :from AND a.timestamp <= :to GROUP BY a.resourceType")
    List<AuditCountByResourceType> countGroupedByResourceType(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a.status as status, COUNT(a) as count FROM AuditLog a " +
           "WHERE a.timestamp >= :from AND a.timestamp <= :to GROUP BY a.status")
    List<AuditCountByStatus> countGroupedByStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a.actorUserId as actorUserId, COUNT(a) as count FROM AuditLog a " +
           "WHERE a.timestamp >= :from AND a.timestamp <= :to GROUP BY a.actorUserId ORDER BY count DESC")
    List<TopActorSummary> findTopActors(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :from AND a.timestamp <= :to " +
           "AND a.status = :status ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByStatus(@Param("from") Instant from,
                                     @Param("to") Instant to,
                                     @Param("status") AuditStatus status,
                                     Pageable pageable);
}
