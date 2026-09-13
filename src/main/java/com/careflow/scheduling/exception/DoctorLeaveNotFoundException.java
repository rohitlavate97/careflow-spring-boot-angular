package com.careflow.scheduling.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a doctor leave request cannot be found (§18).
 */
public class DoctorLeaveNotFoundException extends ResourceNotFoundException {

    public DoctorLeaveNotFoundException(String id) {
        super("DoctorLeave", id);
    }
}
