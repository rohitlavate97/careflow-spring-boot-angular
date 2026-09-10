package com.careflow.common.dto;

/**
 * Standardized detail item for field-level input validation failures.
 * Implemented as an immutable Java 21 record.
 */
public record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {}
