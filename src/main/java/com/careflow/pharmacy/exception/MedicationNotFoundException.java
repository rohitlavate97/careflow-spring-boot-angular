package com.careflow.pharmacy.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a requested medication cannot be found in the catalog (§25).
 */
public class MedicationNotFoundException extends ResourceNotFoundException {

    public MedicationNotFoundException(String id) {
        super("Medication", id);
    }
}
