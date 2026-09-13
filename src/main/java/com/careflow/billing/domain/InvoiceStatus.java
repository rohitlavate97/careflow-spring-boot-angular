package com.careflow.billing.domain;

/**
 * Lifecycle status of a billing invoice (§29, §30).
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
