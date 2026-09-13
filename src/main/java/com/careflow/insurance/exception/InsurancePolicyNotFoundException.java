package com.careflow.insurance.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a patient's insurance policy record is not found (§33).
 */
public class InsurancePolicyNotFoundException extends ResourceNotFoundException {

    public InsurancePolicyNotFoundException(String id) {
        super("InsurancePolicy", id);
    }
}
