package com.careflow.audit.domain;

/**
 * Standard enterprise healthcare operational and compliance actions (§36, §74).
 */
public enum AuditAction {
    // Patient lifecycle
    PATIENT_VIEWED,
    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,

    // Clinical lifecycle
    CLINICAL_RECORD_VIEWED,
    CLINICAL_RECORD_CREATED,
    CLINICAL_RECORD_UPDATED,

    // Pharmacy and Prescriptions
    PRESCRIPTION_CREATED,
    PRESCRIPTION_UPDATED,
    PRESCRIPTION_DISPENSED,
    INVENTORY_ADJUSTED,

    // Diagnostic Laboratory
    LAB_ORDER_CREATED,
    LAB_ORDER_CANCELLED,
    LAB_SPECIMEN_COLLECTED,
    LAB_RESULT_RECORDED,
    LAB_RESULT_VIEWED,

    // Inpatient Admissions & Bed Management
    ADMISSION_CREATED,
    BED_ALLOCATED,
    BED_TRANSFERRED,
    DISCHARGE_PROCESSED,

    // Billing & Financial
    INVOICE_GENERATED,
    INVOICE_UPDATED,
    PAYMENT_PROCESSED,
    REFUND_ISSUED,

    // Insurance Claims
    CLAIM_SUBMITTED,
    CLAIM_APPROVED,
    CLAIM_REJECTED,
    CLAIM_SETTLED,

    // Medical Documents
    DOCUMENT_VIEWED,
    DOCUMENT_UPLOADED,
    DOCUMENT_DOWNLOADED,
    DOCUMENT_SUPERSEDED,

    // Security & Administration
    USER_LOGIN_SUCCESS,
    USER_LOGIN_FAILURE,
    ACCESS_DENIED,
    USER_ROLE_CHANGED,
    USER_STATUS_CHANGED,
    USER_PASSWORD_RESET,
    CONFIGURATION_CHANGED
}
