package com.careflow.staff.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a doctor profile cannot be found by its identifier or medical license number (§17).
 */
public class DoctorNotFoundException extends ResourceNotFoundException {

    public DoctorNotFoundException(String id) {
        super("DoctorProfile", id);
    }

    public static DoctorNotFoundException forLicense(String licenseNumber) {
        return new DoctorNotFoundException("Doctor with medical license '" + licenseNumber + "' was not found.", true);
    }

    public static DoctorNotFoundException forStaffId(String staffId) {
        return new DoctorNotFoundException("Doctor profile for staff member '" + staffId + "' was not found.", true);
    }

    private DoctorNotFoundException(String message, boolean directMessage) {
        super(message);
    }
}
