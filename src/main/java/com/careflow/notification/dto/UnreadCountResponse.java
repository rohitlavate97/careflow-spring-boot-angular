package com.careflow.notification.dto;

/**
 * Payload indicating total unread in-app notifications for the authenticated user (§35).
 */
public record UnreadCountResponse(
        long unreadCount
) {
}
