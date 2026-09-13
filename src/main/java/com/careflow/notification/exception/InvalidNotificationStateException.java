package com.careflow.notification.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid notification lifecycle transition is attempted (§35).
 */
public class InvalidNotificationStateException extends BusinessRuleException {

    public InvalidNotificationStateException(String message) {
        super("INVALID_NOTIFICATION_STATE", message, HttpStatus.BAD_REQUEST);
    }
}
