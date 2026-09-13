package com.careflow.notification.dto;

/**
 * Request payload for updating notification channel preferences (§35).
 */
public record NotificationPreferenceRequest(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled
) {
}
