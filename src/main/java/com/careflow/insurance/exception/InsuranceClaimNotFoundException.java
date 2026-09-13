package com.careflow.insurance.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an insurance reimbursement claim record is not found (§33).
 */
public class InsuranceClaimNotFoundException extends ResourceNotFoundException {

    public InsuranceClaimNotFoundException(String id) {
        super("InsuranceClaim", id);
    }
}
