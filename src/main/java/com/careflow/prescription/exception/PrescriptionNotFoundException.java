package com.careflow.prescription.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a requested prescription cannot be found (§24).
 */
public class PrescriptionNotFoundException extends ResourceNotFoundException {

    public PrescriptionNotFoundException(String id) {
        super("Prescription", id);
    }
}
