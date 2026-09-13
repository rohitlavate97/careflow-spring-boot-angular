package com.careflow.notification.domain;

/**
 * Domain event types supported by the CareFlow notification center (§35).
 */
public enum NotificationType {
    APPOINTMENT_CONFIRMATION,
    APPOINTMENT_CANCELLATION,
    APPOINTMENT_REMINDER,
    LAB_RESULT_AVAILABLE,
    PRESCRIPTION_READY,
    INVOICE_GENERATED,
    INSURANCE_CLAIM_UPDATE,
    SYSTEM_ALERT
}
