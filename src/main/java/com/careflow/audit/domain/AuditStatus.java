package com.careflow.audit.domain;

/**
 * Execution outcome status of an audited hospital or security event (§36, §74).
 */
public enum AuditStatus {
    SUCCESS,
    FAILURE,
    ACCESS_DENIED
}
