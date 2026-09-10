package com.careflow.identity.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user credentials fail verification.
 */
public class InvalidCredentialsException extends BusinessRuleException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password.", HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message, HttpStatus.UNAUTHORIZED);
    }
}
