package com.careflow.appointment.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when an appointment record is not found (§19).
 */
public class AppointmentNotFoundException extends ResourceNotFoundException {

    public AppointmentNotFoundException(String id) {
        super("Appointment", id);
    }
}
