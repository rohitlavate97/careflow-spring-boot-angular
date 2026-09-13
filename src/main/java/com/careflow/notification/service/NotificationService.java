package com.careflow.notification.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.dto.NotificationPreferenceRequest;
import com.careflow.notification.dto.NotificationPreferenceResponse;
import com.careflow.notification.dto.NotificationResponse;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.dto.UnreadCountResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for hospital notifications, preferences, and reminder triggers (§35, §103 Phase 14).
 */
public interface NotificationService {

    /**
     * Dispatches an outbound notification across transport channels.
     */
    NotificationResponse sendNotification(SendNotificationRequest request);

    /**
     * Retrieves paginated notifications targeted to a specific user.
     */
    PageResponse<NotificationResponse> getUserNotifications(String userId, NotificationStatus status, Pageable pageable);

    /**
     * Retrieves total unread in-app notification count for a user.
     */
    UnreadCountResponse getUnreadCount(String userId);

    /**
     * Acknowledges an in-app notification by transitioning it to READ status.
     */
    NotificationResponse markAsRead(String notificationId, String userId);

    /**
     * Bulk-acknowledges all unread notifications for a user.
     */
    void markAllAsRead(String userId);

    /**
     * Triggers an automated appointment reminder to the scheduled patient.
     */
    NotificationResponse sendAppointmentReminder(String appointmentId, NotificationChannel channel);

    /**
     * Retrieves notification transport channel preferences for a user.
     */
    NotificationPreferenceResponse getUserPreferences(String userId);

    /**
     * Updates notification transport channel preferences for a user.
     */
    NotificationPreferenceResponse updateUserPreferences(String userId, NotificationPreferenceRequest request);
}
