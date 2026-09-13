package com.careflow.notification.dto;

import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for dispatching an outbound notification (§35).
 */
public record SendNotificationRequest(
        String recipientUserId,

        String recipientEmail,

        String recipientPhone,

        String patientId,

        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotNull(message = "Notification channel is required")
        NotificationChannel channel,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        String message,

        String referenceType,

        String referenceId
) {
}
