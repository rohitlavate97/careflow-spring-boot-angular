package com.careflow.patient.controller;

import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.PatientAllergyResponse;
import com.careflow.patient.dto.UpdateAllergyStatusRequest;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;
import com.careflow.patient.service.PatientAllergyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing Patient Allergy & Clinical Safety Alert APIs (§16, §39, §89).
 */
@RestController
@RequestMapping("/api/v1/patients/{patientId}/allergies")
@Validated
public class PatientAllergyController {

    private final PatientAllergyService allergyService;

    public PatientAllergyController(PatientAllergyService allergyService) {
        this.allergyService = allergyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientAllergyResponse> recordAllergy(
            @PathVariable String patientId,
            @Valid @RequestBody CreatePatientAllergyRequest request
    ) {
        PatientAllergyResponse response = allergyService.recordAllergy(patientId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{allergyId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<List<PatientAllergyResponse>> getPatientAllergies(
            @PathVariable String patientId,
            @RequestParam(required = false) AllergyStatus status
    ) {
        List<PatientAllergyResponse> allergies = allergyService.getPatientAllergies(patientId, status);
        return ResponseEntity.ok(allergies);
    }

    @GetMapping("/{allergyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientAllergyResponse> getAllergyById(
            @PathVariable String patientId,
            @PathVariable String allergyId
    ) {
        PatientAllergyResponse response = allergyService.getAllergyById(patientId, allergyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{allergyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientAllergyResponse> updateAllergy(
            @PathVariable String patientId,
            @PathVariable String allergyId,
            @Valid @RequestBody UpdatePatientAllergyRequest request
    ) {
        PatientAllergyResponse response = allergyService.updateAllergy(patientId, allergyId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{allergyId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientAllergyResponse> updateAllergyStatus(
            @PathVariable String patientId,
            @PathVariable String allergyId,
            @Valid @RequestBody UpdateAllergyStatusRequest request
    ) {
        PatientAllergyResponse response = allergyService.updateAllergyStatus(patientId, allergyId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{allergyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<Void> removeAllergy(
            @PathVariable String patientId,
            @PathVariable String allergyId
    ) {
        allergyService.removeAllergy(patientId, allergyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<Map<String, Object>> getHighRiskAllergyStatus(@PathVariable String patientId) {
        boolean hasHighRisk = allergyService.hasHighRiskAllergies(patientId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "hasHighRiskAllergies", hasHighRisk
        ));
    }
}
