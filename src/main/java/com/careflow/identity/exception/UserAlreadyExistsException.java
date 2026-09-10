package com.careflow.identity.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to register a user with an already existing username or email.
 */
public class UserAlreadyExistsException extends BusinessRuleException {

    public UserAlreadyExistsException(String field, String value) {
        super("USER_ALREADY_EXISTS",
              String.format("User with %s '%s' already exists.", field, value),
              HttpStatus.CONFLICT);
    }
}
