package com.careflow.patient.service;

import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.PatientAllergyResponse;
import com.careflow.patient.dto.UpdateAllergyStatusRequest;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;

import java.util.List;

/**
 * Service boundary managing patient allergy records and clinical safety alerts (§13, §16).
 */
public interface PatientAllergyService {

    PatientAllergyResponse recordAllergy(String patientId, CreatePatientAllergyRequest request);

    List<PatientAllergyResponse> getPatientAllergies(String patientId, AllergyStatus status);

    PatientAllergyResponse getAllergyById(String patientId, String allergyId);

    PatientAllergyResponse updateAllergy(String patientId, String allergyId, UpdatePatientAllergyRequest request);

    PatientAllergyResponse updateAllergyStatus(String patientId, String allergyId, UpdateAllergyStatusRequest request);

    void removeAllergy(String patientId, String allergyId);

    boolean hasHighRiskAllergies(String patientId);
}
