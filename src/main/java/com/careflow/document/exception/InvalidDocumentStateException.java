package com.careflow.document.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a document operation is invalid due to its lifecycle state or revision mismatch (§34).
 */
public class InvalidDocumentStateException extends BusinessRuleException {

    public InvalidDocumentStateException(String message) {
        super("INVALID_DOCUMENT_STATE", message, HttpStatus.BAD_REQUEST);
    }
}
