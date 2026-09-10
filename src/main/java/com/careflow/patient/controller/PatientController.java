package com.careflow.patient.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.PatientResponse;
import com.careflow.patient.dto.PatientSummaryResponse;
import com.careflow.patient.dto.UpdatePatientRequest;
import com.careflow.patient.dto.UpdatePatientStatusRequest;
import com.careflow.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller exposing Patient Management APIs (§16, §89).
 * Enforces role-based access control and input validation.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Validated
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientResponse> registerPatient(@Valid @RequestBody CreatePatientRequest request) {
        PatientResponse response = patientService.registerPatient(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable String id) {
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mrn/{mrn}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientResponse> getPatientByMrn(@PathVariable String mrn) {
        PatientResponse response = patientService.getPatientByMrn(mrn);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        PatientResponse response = patientService.updatePatient(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<PatientResponse> updatePatientStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientStatusRequest request
    ) {
        PatientResponse response = patientService.updatePatientStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PageResponse<PatientResponse>> searchPatients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) PatientStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<PatientResponse> response = patientService.searchPatients(query, gender, status, dateOfBirth, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quick-search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<List<PatientSummaryResponse>> quickSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<PatientSummaryResponse> response = patientService.quickSearch(query, limit);
        return ResponseEntity.ok(response);
    }
}
