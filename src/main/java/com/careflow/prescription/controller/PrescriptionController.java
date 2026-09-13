package com.careflow.prescription.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.prescription.dto.CreatePrescriptionRequest;
import com.careflow.prescription.dto.PrescriptionResponse;
import com.careflow.prescription.dto.PrescriptionSummaryResponse;
import com.careflow.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller exposing prescription ordering and lifecycle APIs (§24, §39, §44, §103 Phase 8).
 */
@RestController
@RequestMapping("/api/v1/prescriptions")
@Validated
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PrescriptionResponse> issuePrescription(@Valid @RequestBody CreatePrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.issuePrescription(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable String id) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PageResponse<PrescriptionSummaryResponse>> getPrescriptionsByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "prescribedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(prescriptionService.getPrescriptionsByPatient(patientId, pageable)));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PageResponse<PrescriptionSummaryResponse>> getPrescriptionsByDoctor(
            @PathVariable String doctorId,
            @PageableDefault(size = 20, sort = "prescribedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(prescriptionService.getPrescriptionsByDoctor(doctorId, pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PageResponse<PrescriptionSummaryResponse>> getPendingPrescriptions(
            @PageableDefault(size = 20, sort = "prescribedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(prescriptionService.getPendingPrescriptions(pageable)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PrescriptionResponse> cancelPrescription(@PathVariable String id,
                                                                   @RequestParam(required = false, defaultValue = "Cancelled by physician") String reason) {
        PrescriptionResponse response = prescriptionService.cancelPrescription(id, reason);
        return ResponseEntity.ok(response);
    }
}
