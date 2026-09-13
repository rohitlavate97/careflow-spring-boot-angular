package com.careflow.insurance.domain;

/**
 * Beneficiary relationship to the primary insurance policyholder (§33).
 */
public enum PolicyRelationship {
    SELF,
    SPOUSE,
    CHILD,
    OTHER
}
