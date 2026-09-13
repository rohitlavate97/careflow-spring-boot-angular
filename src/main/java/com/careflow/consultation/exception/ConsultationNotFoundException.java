package com.careflow.consultation.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a requested consultation encounter does not exist (§22).
 */
public class ConsultationNotFoundException extends ResourceNotFoundException {

    public ConsultationNotFoundException(String id) {
        super("Consultation", id);
    }
}
