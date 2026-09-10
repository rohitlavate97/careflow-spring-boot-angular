package com.careflow.identity.domain;

/**
 * Account lifecycle statuses for users in the CareFlow platform (§14).
 */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    SUSPENDED,
    INACTIVE
}
