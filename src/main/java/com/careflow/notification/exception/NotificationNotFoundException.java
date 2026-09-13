package com.careflow.notification.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a requested notification record is not found (§35).
 */
public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(String id) {
        super("Notification", id);
    }
}
