package com.careflow.insurance.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an insurance provider/payer record is not found (§33).
 */
public class InsuranceProviderNotFoundException extends ResourceNotFoundException {

    public InsuranceProviderNotFoundException(String id) {
        super("InsuranceProvider", id);
    }
}
