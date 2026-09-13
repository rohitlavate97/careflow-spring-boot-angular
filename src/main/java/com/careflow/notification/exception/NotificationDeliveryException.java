package com.careflow.notification.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when outbound dispatch via a transport channel fails (§35).
 */
public class NotificationDeliveryException extends BusinessRuleException {

    public NotificationDeliveryException(String message) {
        super("NOTIFICATION_DELIVERY_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super("NOTIFICATION_DELIVERY_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
