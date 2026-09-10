package com.careflow.patient.mapper;

import com.careflow.patient.domain.Address;
import com.careflow.patient.domain.EmergencyContact;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.dto.AddressDto;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.EmergencyContactDto;
import com.careflow.patient.dto.PatientResponse;
import com.careflow.patient.dto.PatientSummaryResponse;
import com.careflow.patient.dto.UpdatePatientRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * Mapper for converting between Patient domain entities and DTOs (§89, §90).
 * Implemented as pure Java without reflection or code generation overhead.
 */
@Component
public class PatientMapper {

    public Patient toEntity(String id, String mrn, CreatePatientRequest request) {
        if (request == null) {
            return null;
        }

        Patient patient = new Patient(
                id,
                mrn,
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender(),
                request.phone().trim()
        );

        if (request.middleName() != null) {
            patient.setMiddleName(request.middleName().trim());
        }
        patient.setBloodGroup(request.bloodGroup());
        if (request.email() != null) {
            patient.setEmail(request.email().trim().toLowerCase());
        }

        if (request.address() != null) {
            patient.setAddress(toAddress(request.address()));
        }

        if (request.emergencyContact() != null) {
            patient.setEmergencyContact(toEmergencyContact(request.emergencyContact()));
        }

        return patient;
    }

    public void updateEntity(Patient patient, UpdatePatientRequest request) {
        if (patient == null || request == null) {
            return;
        }

        patient.setFirstName(request.firstName().trim());
        patient.setMiddleName(request.middleName() != null ? request.middleName().trim() : null);
        patient.setLastName(request.lastName().trim());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setBloodGroup(request.bloodGroup());
        patient.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);
        patient.setPhone(request.phone().trim());

        if (request.address() != null) {
            patient.setAddress(toAddress(request.address()));
        } else {
            patient.setAddress(null);
        }

        if (request.emergencyContact() != null) {
            patient.setEmergencyContact(toEmergencyContact(request.emergencyContact()));
        } else {
            patient.setEmergencyContact(null);
        }
    }

    public PatientResponse toResponse(Patient patient) {
        if (patient == null) {
            return null;
        }

        int age = calculateAge(patient.getDateOfBirth());

        return new PatientResponse(
                patient.getId(),
                patient.getMrn(),
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                age,
                patient.getGender(),
                patient.getBloodGroup(),
                patient.getEmail(),
                patient.getPhone(),
                toAddressDto(patient.getAddress()),
                toEmergencyContactDto(patient.getEmergencyContact()),
                patient.getStatus(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getVersion()
        );
    }

    public PatientSummaryResponse toSummaryResponse(Patient patient) {
        if (patient == null) {
            return null;
        }

        return new PatientSummaryResponse(
                patient.getId(),
                patient.getMrn(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getStatus()
        );
    }

    public Address toAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        return new Address(
                dto.addressLine1().trim(),
                dto.addressLine2() != null ? dto.addressLine2().trim() : null,
                dto.city().trim(),
                dto.state().trim(),
                dto.postalCode().trim(),
                dto.country().trim()
        );
    }

    public AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }

    public EmergencyContact toEmergencyContact(EmergencyContactDto dto) {
        if (dto == null) {
            return null;
        }
        return new EmergencyContact(
                dto.name().trim(),
                dto.relationship().trim(),
                dto.phone().trim()
        );
    }

    public EmergencyContactDto toEmergencyContactDto(EmergencyContact contact) {
        if (contact == null) {
            return null;
        }
        return new EmergencyContactDto(
                contact.getName(),
                contact.getRelationship(),
                contact.getPhone()
        );
    }

    private int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
