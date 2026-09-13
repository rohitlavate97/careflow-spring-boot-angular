package com.careflow.billing.domain;

/**
 * Accepted payment tender methods (§31).
 */
public enum PaymentMethod {
    CASH,
    CREDIT_CARD,
    DEBIT_CARD,
    INSURANCE,
    BANK_TRANSFER
}
