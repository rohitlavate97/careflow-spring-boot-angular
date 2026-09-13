package com.careflow.notification.dto;

import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.domain.NotificationType;

import java.time.Instant;

/**
 * Standard response payload representing a notification record (§35, §89).
 */
public record NotificationResponse(
        String id,
        String recipientUserId,
        String recipientEmail,
        String recipientPhone,
        String patientId,
        NotificationType notificationType,
        NotificationChannel channel,
        String title,
        String message,
        String referenceType,
        String referenceId,
        NotificationStatus status,
        String failureReason,
        Instant sentAt,
        Instant readAt,
        Instant createdAt
) {
}
