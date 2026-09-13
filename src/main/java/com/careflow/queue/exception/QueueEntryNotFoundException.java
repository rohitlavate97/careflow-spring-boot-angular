package com.careflow.queue.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a queue ticket or entry is not found (§21).
 */
public class QueueEntryNotFoundException extends ResourceNotFoundException {

    public QueueEntryNotFoundException(String id) {
        super("QueueEntry", id);
    }
}
