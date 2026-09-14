package com.careflow.audit.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an audit record is not found (§36, §42).
 */
public class AuditLogNotFoundException extends ResourceNotFoundException {

    public AuditLogNotFoundException(String id) {
        super("AuditLog", id);
    }
}
