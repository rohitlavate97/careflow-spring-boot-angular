package com.careflow.patient.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a patient cannot be found by their unique ID or MRN.
 */
public class PatientNotFoundException extends ResourceNotFoundException {

    public PatientNotFoundException(String id) {
        super("Patient", id);
    }

    public static PatientNotFoundException forMrn(String mrn) {
        return new PatientNotFoundException("Patient with MRN '" + mrn + "' was not found.", true);
    }

    private PatientNotFoundException(String message, boolean isDirectMessage) {
        super(message);
    }
}
