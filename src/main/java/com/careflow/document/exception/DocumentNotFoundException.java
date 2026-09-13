package com.careflow.document.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a requested medical document record is not found (§34).
 */
public class DocumentNotFoundException extends ResourceNotFoundException {

    public DocumentNotFoundException(String id) {
        super("MedicalDocument", id);
    }
}
