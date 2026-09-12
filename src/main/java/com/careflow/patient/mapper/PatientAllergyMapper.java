package com.careflow.patient.mapper;

import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.PatientAllergy;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.PatientAllergyResponse;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper between PatientAllergy domain entity and API DTOs (§89, §90).
 */
@Component
public class PatientAllergyMapper {

    public PatientAllergy toEntity(String id, String patientId, CreatePatientAllergyRequest request) {
        if (request == null) {
            return null;
        }

        return new PatientAllergy(
                id,
                patientId,
                request.allergen().trim(),
                request.category(),
                request.severity(),
                request.reaction() != null ? request.reaction().trim() : null,
                AllergyStatus.ACTIVE,
                request.notes() != null ? request.notes().trim() : null,
                request.diagnosedDate()
        );
    }

    public void updateEntity(PatientAllergy allergy, UpdatePatientAllergyRequest request) {
        if (allergy == null || request == null) {
            return;
        }

        allergy.setSeverity(request.severity());
        allergy.setReaction(request.reaction() != null ? request.reaction().trim() : null);
        allergy.setNotes(request.notes() != null ? request.notes().trim() : null);
        allergy.setDiagnosedDate(request.diagnosedDate());
    }

    public PatientAllergyResponse toResponse(PatientAllergy allergy) {
        if (allergy == null) {
            return null;
        }

        return new PatientAllergyResponse(
                allergy.getId(),
                allergy.getPatientId(),
                allergy.getAllergen(),
                allergy.getCategory(),
                allergy.getSeverity(),
                allergy.getReaction(),
                allergy.getStatus(),
                allergy.getNotes(),
                allergy.getDiagnosedDate(),
                allergy.isHighRisk(),
                allergy.getCreatedAt(),
                allergy.getUpdatedAt(),
                allergy.getVersion()
        );
    }

    public List<PatientAllergyResponse> toResponseList(List<PatientAllergy> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return Collections.emptyList();
        }
        return allergies.stream()
                .map(this::toResponse)
                .toList();
    }
}
