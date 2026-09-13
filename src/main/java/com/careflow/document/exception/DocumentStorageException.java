package com.careflow.document.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when binary storage operations fail or a requested file resource cannot be read (§34).
 */
public class DocumentStorageException extends BusinessRuleException {

    public DocumentStorageException(String message) {
        super("DOCUMENT_STORAGE_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public DocumentStorageException(String message, HttpStatus status) {
        super("DOCUMENT_STORAGE_ERROR", message, status);
    }

    public DocumentStorageException(String message, Throwable cause) {
        super("DOCUMENT_STORAGE_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
