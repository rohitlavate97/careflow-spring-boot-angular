package com.careflow.staff.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a doctor's medical license number violates uniqueness constraints (§17, §92).
 */
public class DuplicateDoctorLicenseException extends BusinessRuleException {

    public DuplicateDoctorLicenseException(String licenseNumber) {
        super("DUPLICATE_DOCTOR_LICENSE",
                "Doctor with medical license number '" + licenseNumber + "' already exists.",
                HttpStatus.CONFLICT);
    }
}
