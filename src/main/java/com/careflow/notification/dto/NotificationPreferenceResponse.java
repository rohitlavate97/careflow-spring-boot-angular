package com.careflow.notification.dto;

/**
 * Response payload representing notification channel preferences for a user (§35).
 */
public record NotificationPreferenceResponse(
        String id,
        String userId,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled
) {
}
