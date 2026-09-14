package com.careflow.queue.repository;

import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for QueueEntry aggregate with concurrency-safe locking (§21, §92).
 */
@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, String> {

    @Query("SELECT COALESCE(MAX(q.tokenNumber), 0) FROM QueueEntry q WHERE q.departmentId = :departmentId AND q.queueDate = :queueDate")
    int findMaxTokenNumberByDepartmentIdAndQueueDate(
            @Param("departmentId") String departmentId,
            @Param("queueDate") LocalDate queueDate
    );

    @Query("""
        SELECT q.id FROM QueueEntry q
        WHERE q.departmentId = :departmentId
          AND q.queueDate = :queueDate
          AND q.status = com.careflow.queue.domain.QueueStatus.WAITING
          AND (:doctorId IS NULL OR q.doctorId IS NULL OR q.doctorId = :doctorId)
        ORDER BY CASE q.priority
            WHEN com.careflow.queue.domain.QueuePriority.EMERGENCY THEN 1
            WHEN com.careflow.queue.domain.QueuePriority.URGENT THEN 2
            ELSE 3 END ASC,
            q.entryTime ASC,
            q.tokenNumber ASC
    """)
    List<String> findNextWaitingCandidateIds(
            @Param("departmentId") String departmentId,
            @Param("queueDate") LocalDate queueDate,
            @Param("doctorId") String doctorId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueEntry q WHERE q.id = :id")
    Optional<QueueEntry> findByIdForUpdate(@Param("id") String id);

    @Query("""
        SELECT COUNT(q) FROM QueueEntry q
        WHERE q.departmentId = :departmentId
          AND q.queueDate = :queueDate
          AND q.status = com.careflow.queue.domain.QueueStatus.WAITING
          AND (
              (CASE q.priority
                  WHEN com.careflow.queue.domain.QueuePriority.EMERGENCY THEN 1
                  WHEN com.careflow.queue.domain.QueuePriority.URGENT THEN 2
                  ELSE 3 END < :priorityRank)
              OR
              (CASE q.priority
                  WHEN com.careflow.queue.domain.QueuePriority.EMERGENCY THEN 1
                  WHEN com.careflow.queue.domain.QueuePriority.URGENT THEN 2
                  ELSE 3 END = :priorityRank AND q.entryTime < :entryTime)
          )
    """)
    long countPatientsAhead(
            @Param("departmentId") String departmentId,
            @Param("queueDate") LocalDate queueDate,
            @Param("priorityRank") int priorityRank,
            @Param("entryTime") Instant entryTime
    );

    List<QueueEntry> findByDepartmentIdAndQueueDateAndStatusInOrderByPriorityAscEntryTimeAsc(
            String departmentId,
            LocalDate queueDate,
            Collection<QueueStatus> statuses
    );

    long countByDepartmentIdAndQueueDateAndStatus(
            String departmentId,
            LocalDate queueDate,
            QueueStatus status
    );

    Optional<QueueEntry> findFirstByPatientIdAndStatusIn(
            String patientId,
            Collection<QueueStatus> statuses
    );

    Optional<QueueEntry> findByAppointmentId(String appointmentId);

    List<QueueEntry> findByDepartmentIdAndQueueDateOrderByTokenNumberAsc(
            String departmentId,
            LocalDate queueDate
    );

    @Query("SELECT q.status, COUNT(q) FROM QueueEntry q WHERE q.queueDate = :queueDate " +
           "AND (:departmentId IS NULL OR q.departmentId = :departmentId) " +
           "GROUP BY q.status")
    List<Object[]> countQueueByStatus(
            @Param("queueDate") LocalDate queueDate,
            @Param("departmentId") String departmentId
    );

    @Query("SELECT q.priority, COUNT(q) FROM QueueEntry q WHERE q.queueDate = :queueDate " +
           "AND (:departmentId IS NULL OR q.departmentId = :departmentId) " +
           "GROUP BY q.priority")
    List<Object[]> countQueueByPriority(
            @Param("queueDate") LocalDate queueDate,
            @Param("departmentId") String departmentId
    );

    @Query("SELECT q.entryTime, q.calledTime FROM QueueEntry q WHERE q.queueDate = :queueDate " +
           "AND q.calledTime IS NOT NULL " +
           "AND (:departmentId IS NULL OR q.departmentId = :departmentId)")
    List<Object[]> findQueueWaitTimes(
            @Param("queueDate") LocalDate queueDate,
            @Param("departmentId") String departmentId
    );
}
