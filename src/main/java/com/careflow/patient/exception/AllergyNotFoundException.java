package com.careflow.patient.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an allergy record cannot be found for a given identifier.
 */
public class AllergyNotFoundException extends ResourceNotFoundException {

    public AllergyNotFoundException(String id) {
        super("PatientAllergy", id);
    }
}
