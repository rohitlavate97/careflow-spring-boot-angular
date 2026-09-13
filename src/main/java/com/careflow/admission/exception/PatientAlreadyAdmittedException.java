package com.careflow.admission.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to admit a patient who already has an active inpatient admission (§28).
 */
public class PatientAlreadyAdmittedException extends BusinessRuleException {

    public PatientAlreadyAdmittedException(String patientId, String existingAdmissionNumber) {
        super("PATIENT_ALREADY_ADMITTED",
                String.format("Patient '%s' already has an active admission: %s", patientId, existingAdmissionNumber),
                HttpStatus.CONFLICT);
    }
}
