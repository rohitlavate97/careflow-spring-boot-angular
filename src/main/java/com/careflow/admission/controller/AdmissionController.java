package com.careflow.admission.controller;

import com.careflow.admission.domain.AdmissionStatus;
import com.careflow.admission.dto.AdmissionResponse;
import com.careflow.admission.dto.AdmissionSummaryResponse;
import com.careflow.admission.dto.AdmitPatientRequest;
import com.careflow.admission.dto.BedResponse;
import com.careflow.admission.dto.CreateBedRequest;
import com.careflow.admission.dto.CreateRoomRequest;
import com.careflow.admission.dto.CreateWardRequest;
import com.careflow.admission.dto.DischargePatientRequest;
import com.careflow.admission.dto.RoomResponse;
import com.careflow.admission.dto.TransferBedRequest;
import com.careflow.admission.dto.WardResponse;
import com.careflow.admission.service.AdmissionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Inpatient Admissions, Ward layouts, Bed allocations, and Transfers (§28, §39, §44, §103 Phase 10).
 */
@RestController
@RequestMapping("/api/v1/admissions")
@Validated
public class AdmissionController {

    private final AdmissionService admissionService;

    public AdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Wards, Rooms & Beds
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/wards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody CreateWardRequest request) {
        WardResponse response = admissionService.createWard(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/wards/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WardResponse> getWardById(@PathVariable String id) {
        return ResponseEntity.ok(admissionService.getWardById(id));
    }

    @GetMapping("/wards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<WardResponse>> getWards(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(admissionService.getWards(pageable)));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = admissionService.createRoom(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/rooms/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable String id) {
        return ResponseEntity.ok(admissionService.getRoomById(id));
    }

    @GetMapping("/wards/{wardId}/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoomResponse>> getRoomsByWard(@PathVariable String wardId) {
        return ResponseEntity.ok(admissionService.getRoomsByWard(wardId));
    }

    @PostMapping("/beds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BedResponse> createBed(@Valid @RequestBody CreateBedRequest request) {
        BedResponse response = admissionService.createBed(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/beds/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BedResponse> getBedById(@PathVariable String id) {
        return ResponseEntity.ok(admissionService.getBedById(id));
    }

    @GetMapping("/beds/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BedResponse>> getAvailableBeds(@RequestParam(required = false) String wardId) {
        return ResponseEntity.ok(admissionService.getAvailableBeds(wardId));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Inpatient Admissions
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> admitPatient(@Valid @RequestBody AdmitPatientRequest request) {
        AdmissionResponse response = admissionService.admitPatient(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<AdmissionResponse> getAdmissionById(@PathVariable String id) {
        return ResponseEntity.ok(admissionService.getAdmissionById(id));
    }

    @GetMapping("/number/{admissionNumber}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> getAdmissionByNumber(@PathVariable String admissionNumber) {
        return ResponseEntity.ok(admissionService.getAdmissionByNumber(admissionNumber));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PageResponse<AdmissionSummaryResponse>> getAdmissionsByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "admittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(admissionService.getAdmissionsByPatient(patientId, pageable)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'BILLING_OFFICER')")
    public ResponseEntity<PageResponse<AdmissionSummaryResponse>> getAdmissions(
            @RequestParam(required = false) AdmissionStatus status,
            @PageableDefault(size = 20, sort = "admittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(PageResponse.from(admissionService.getAdmissionsByStatus(status, pageable)));
        }
        return ResponseEntity.ok(PageResponse.from(admissionService.getAllAdmissions(pageable)));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> transferBed(@PathVariable String id,
                                                         @Valid @RequestBody TransferBedRequest request) {
        return ResponseEntity.ok(admissionService.transferBed(id, request));
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> dischargePatient(@PathVariable String id,
                                                              @Valid @RequestBody DischargePatientRequest request) {
        return ResponseEntity.ok(admissionService.dischargePatient(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AdmissionResponse> cancelAdmission(@PathVariable String id,
                                                             @RequestParam(required = false, defaultValue = "Cancelled by physician") String reason) {
        return ResponseEntity.ok(admissionService.cancelAdmission(id, reason));
    }
}
