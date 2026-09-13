package com.careflow.admission.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an inpatient admission record is not found.
 */
public class AdmissionNotFoundException extends ResourceNotFoundException {

    public AdmissionNotFoundException(String id) {
        super("Admission", id);
    }
}
