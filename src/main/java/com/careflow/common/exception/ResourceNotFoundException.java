package com.careflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Standard exception thrown when a requested domain resource cannot be found.
 */
public class ResourceNotFoundException extends BusinessRuleException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with identifier '%s' was not found.", resourceName, identifier),
              HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
