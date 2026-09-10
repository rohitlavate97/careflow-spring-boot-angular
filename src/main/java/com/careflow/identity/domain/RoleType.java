package com.careflow.identity.domain;

/**
 * Standard hospital system role designations (§14).
 * Prefixed with ROLE_ to follow Spring Security conventions.
 */
public enum RoleType {
    ROLE_ADMIN,
    ROLE_DOCTOR,
    ROLE_NURSE,
    ROLE_RECEPTIONIST,
    ROLE_LAB_TECHNICIAN,
    ROLE_PHARMACIST,
    ROLE_BILLING_OFFICER,
    ROLE_PATIENT
}
