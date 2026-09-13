package com.careflow.consultation.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.consultation.dto.AddDiagnosisRequest;
import com.careflow.consultation.dto.ConsultationResponse;
import com.careflow.consultation.dto.ConsultationSummaryResponse;
import com.careflow.consultation.dto.CreateConsultationRequest;
import com.careflow.consultation.dto.RecordVitalsRequest;
import com.careflow.consultation.dto.UpdateConsultationRequest;
import com.careflow.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller exposing patient consultation encounters, vitals, diagnoses,
 * and clinical lifecycle management endpoints (§22, §39, §44, §103 Phase 7).
 */
@RestController
@RequestMapping("/api/v1/consultations")
@Validated
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> startConsultation(@Valid @RequestBody CreateConsultationRequest request) {
        ConsultationResponse response = consultationService.startConsultation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> getConsultationById(@PathVariable String id) {
        ConsultationResponse response = consultationService.getConsultationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/findings")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> updateFindings(@PathVariable String id,
                                                               @Valid @RequestBody UpdateConsultationRequest request) {
        ConsultationResponse response = consultationService.updateFindings(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/vitals")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> recordVitals(@PathVariable String id,
                                                             @Valid @RequestBody RecordVitalsRequest request) {
        ConsultationResponse response = consultationService.recordVitals(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/diagnoses")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> addDiagnosis(@PathVariable String id,
                                                             @Valid @RequestBody AddDiagnosisRequest request) {
        ConsultationResponse response = consultationService.addDiagnosis(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/diagnoses/{diagnosisId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> removeDiagnosis(@PathVariable String id,
                                                                @PathVariable String diagnosisId) {
        ConsultationResponse response = consultationService.removeDiagnosis(id, diagnosisId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> completeConsultation(@PathVariable String id) {
        ConsultationResponse response = consultationService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> cancelConsultation(@PathVariable String id,
                                                                   @RequestParam(required = false, defaultValue = "Consultation cancelled by doctor") String reason) {
        ConsultationResponse response = consultationService.cancelConsultation(id, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<PageResponse<ConsultationSummaryResponse>> getConsultationsByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(consultationService.getConsultationsByPatient(patientId, pageable)));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PageResponse<ConsultationSummaryResponse>> getConsultationsByDoctor(
            @PathVariable String doctorId,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(consultationService.getConsultationsByDoctor(doctorId, pageable)));
    }
}
