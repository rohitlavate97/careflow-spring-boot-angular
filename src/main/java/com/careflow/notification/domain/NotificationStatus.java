package com.careflow.notification.domain;

/**
 * Lifecycle and delivery status of a notification record (§35).
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
