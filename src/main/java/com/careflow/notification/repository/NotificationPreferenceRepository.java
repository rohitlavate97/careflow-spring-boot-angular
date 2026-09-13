package com.careflow.notification.repository;

import com.careflow.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for NotificationPreference entities (§35).
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    Optional<NotificationPreference> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
