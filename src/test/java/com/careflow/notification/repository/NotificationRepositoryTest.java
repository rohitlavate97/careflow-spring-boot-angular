package com.careflow.notification.repository;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationPreference;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private String userId;
    private Notification notifSent;
    private Notification notifRead;

    @BeforeEach
    void setUp() {
        userId = "user-" + UUID.randomUUID();

        notifSent = new Notification(
                UUID.randomUUID().toString(),
                userId,
                "patient@careflow.local",
                "+1-555-0100",
                "pat-01",
                NotificationType.APPOINTMENT_CONFIRMATION,
                NotificationChannel.IN_APP,
                "Appointment Confirmed",
                "Your appointment has been successfully scheduled.",
                "APPOINTMENT",
                "apt-01"
        );
        notifSent.markAsSent();
        notifSent.markAsDelivered();
        notificationRepository.save(notifSent);

        notifRead = new Notification(
                UUID.randomUUID().toString(),
                userId,
                "patient@careflow.local",
                "+1-555-0100",
                "pat-01",
                NotificationType.INVOICE_GENERATED,
                NotificationChannel.IN_APP,
                "Invoice Issued",
                "New invoice INV-001 is ready for payment.",
                "INVOICE",
                "inv-01"
        );
        notifRead.markAsSent();
        notifRead.markAsRead();
        notificationRepository.save(notifRead);
    }

    @Test
    @DisplayName("Finds paginated notifications for recipient user")
    void findByRecipientUserId_Success() {
        Page<Notification> page = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Filters notifications by recipient and status")
    void findByRecipientUserIdAndStatus_Success() {
        Page<Notification> deliveredPage = notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                userId, NotificationStatus.DELIVERED, PageRequest.of(0, 10));

        assertThat(deliveredPage.getTotalElements()).isEqualTo(1);
        assertThat(deliveredPage.getContent().get(0).getTitle()).isEqualTo("Appointment Confirmed");

        Page<Notification> readPage = notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                userId, NotificationStatus.READ, PageRequest.of(0, 10));

        assertThat(readPage.getTotalElements()).isEqualTo(1);
        assertThat(readPage.getContent().get(0).getTitle()).isEqualTo("Invoice Issued");
    }

    @Test
    @DisplayName("Counts unread notifications by status")
    void countByRecipientUserIdAndStatus_Success() {
        long deliveredCount = notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.DELIVERED);
        long readCount = notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.READ);

        assertThat(deliveredCount).isEqualTo(1);
        assertThat(readCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Loads notification with pessimistic write lock")
    void findByIdForUpdate_Success() {
        Optional<Notification> locked = notificationRepository.findByIdForUpdate(notifSent.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(notifSent.getId());
    }

    @Test
    @DisplayName("Saves and finds notification preferences by user ID")
    void preferences_Success() {
        NotificationPreference preference = new NotificationPreference(
                UUID.randomUUID().toString(),
                userId,
                true,
                false,
                true
        );
        preferenceRepository.save(preference);

        Optional<NotificationPreference> found = preferenceRepository.findByUserId(userId);
        assertThat(found).isPresent();
        assertThat(found.get().isEmailEnabled()).isTrue();
        assertThat(found.get().isSmsEnabled()).isFalse();
        assertThat(found.get().isInAppEnabled()).isTrue();
    }
}
