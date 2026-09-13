package com.careflow.clinical.controller;

import com.careflow.clinical.dto.ClinicalNoteResponse;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import com.careflow.clinical.service.ClinicalRecordService;
import com.careflow.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller exposing clinical documentation notes and longitudinal patient history (§23, §39, §44, §103 Phase 7).
 */
@RestController
@RequestMapping("/api/v1/clinical-records")
@Validated
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    public ClinicalRecordController(ClinicalRecordService clinicalRecordService) {
        this.clinicalRecordService = clinicalRecordService;
    }

    @PostMapping("/notes")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<ClinicalNoteResponse> createClinicalNote(@Valid @RequestBody CreateClinicalNoteRequest request) {
        ClinicalNoteResponse response = clinicalRecordService.createClinicalNote(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/notes/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<ClinicalNoteResponse> getClinicalNoteById(@PathVariable String id) {
        ClinicalNoteResponse response = clinicalRecordService.getClinicalNoteById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}/notes")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<PageResponse<ClinicalNoteResponse>> getNotesByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(clinicalRecordService.getNotesByPatient(patientId, pageable)));
    }

    @GetMapping("/consultation/{consultationId}/notes")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<List<ClinicalNoteResponse>> getNotesByConsultation(@PathVariable String consultationId) {
        return ResponseEntity.ok(clinicalRecordService.getNotesByConsultation(consultationId));
    }
}
