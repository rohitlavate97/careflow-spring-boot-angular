package com.careflow.patient.dto;

import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.PatientStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Standard data transfer object representing a patient (§16, §89).
 * Encapsulates demographics, calculated age, contact, address, and audit metadata.
 */
public record PatientResponse(
        String id,
        String mrn,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        int age,
        Gender gender,
        BloodGroup bloodGroup,
        String email,
        String phone,
        AddressDto address,
        EmergencyContactDto emergencyContact,
        PatientStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
