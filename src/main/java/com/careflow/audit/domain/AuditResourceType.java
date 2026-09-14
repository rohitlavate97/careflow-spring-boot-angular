package com.careflow.audit.domain;

/**
 * Hospital domain entity resource categories tracked in the audit trail (§36, §74).
 */
public enum AuditResourceType {
    PATIENT,
    CLINICAL_RECORD,
    PRESCRIPTION,
    PHARMACY_INVENTORY,
    LAB_ORDER,
    LAB_RESULT,
    ADMISSION,
    BED,
    INVOICE,
    PAYMENT,
    INSURANCE_CLAIM,
    DOCUMENT,
    APPOINTMENT,
    QUEUE_ENTRY,
    USER,
    SYSTEM
}
