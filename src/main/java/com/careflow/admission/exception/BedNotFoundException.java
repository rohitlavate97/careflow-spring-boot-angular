package com.careflow.admission.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an inpatient bed is not found.
 */
public class BedNotFoundException extends ResourceNotFoundException {

    public BedNotFoundException(String id) {
        super("Bed", id);
    }
}
