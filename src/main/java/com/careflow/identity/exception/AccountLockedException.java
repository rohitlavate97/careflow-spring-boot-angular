package com.careflow.identity.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an authentication attempt is made against a locked user account (§14).
 */
public class AccountLockedException extends BusinessRuleException {

    public AccountLockedException(String message) {
        super("ACCOUNT_LOCKED", message, HttpStatus.UNAUTHORIZED);
    }
}
