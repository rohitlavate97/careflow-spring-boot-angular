package com.careflow.billing.domain;

/**
 * Processing status of a payment transaction (§31, §32).
 */
public enum PaymentStatus {
    INITIATED,
    SUCCESS,
    FAILED,
    REFUNDED
}
