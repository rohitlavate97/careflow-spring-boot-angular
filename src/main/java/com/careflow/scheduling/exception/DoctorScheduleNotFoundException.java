package com.careflow.scheduling.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a doctor schedule rule cannot be found (§18).
 */
public class DoctorScheduleNotFoundException extends ResourceNotFoundException {

    public DoctorScheduleNotFoundException(String id) {
        super("DoctorSchedule", id);
    }
}
