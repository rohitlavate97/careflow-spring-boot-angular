package com.careflow.audit.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an illegal attempt is made to update or delete an immutable audit log (§36, §105).
 */
public class AuditImmutabilityException extends BusinessRuleException {

    public AuditImmutabilityException(String message) {
        super("AUDIT_IMMUTABILITY_VIOLATION", message, HttpStatus.BAD_REQUEST);
    }
}
