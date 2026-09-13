package com.careflow.billing.domain;

/**
 * Clinical or operational source generating an invoice line item (§29).
 */
public enum BillingSource {
    CONSULTATION,
    LABORATORY,
    PHARMACY,
    ADMISSION,
    PROCEDURE,
    MISCELLANEOUS
}
