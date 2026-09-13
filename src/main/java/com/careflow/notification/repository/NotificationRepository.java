package com.careflow.notification.repository;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationStatus;
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
 * Spring Data JPA repository for Notification entities (§35).
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            String recipientUserId,
            NotificationStatus status,
            Pageable pageable
    );

    List<Notification> findByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    long countByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    Page<Notification> findByPatientIdOrderByCreatedAtDesc(String patientId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") String id);
}
