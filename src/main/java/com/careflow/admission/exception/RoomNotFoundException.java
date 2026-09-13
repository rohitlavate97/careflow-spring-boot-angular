package com.careflow.admission.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an inpatient room is not found.
 */
public class RoomNotFoundException extends ResourceNotFoundException {

    public RoomNotFoundException(String id) {
        super("Room", id);
    }
}
