package com.careflow.pharmacy.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.pharmacy.domain.MedicationStatus;
import com.careflow.pharmacy.dto.AddInventoryBatchRequest;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.dto.DispenseRecordResponse;
import com.careflow.pharmacy.dto.MedicationResponse;
import com.careflow.pharmacy.dto.PharmacyInventoryBatchResponse;
import com.careflow.pharmacy.dto.UpdateMedicationRequest;
import com.careflow.pharmacy.service.PharmacyService;
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
 * REST controller exposing medication catalog, inventory batches, and dispensing APIs (§25, §26, §39, §44, §103 Phase 8).
 */
@RestController
@RequestMapping("/api/v1/pharmacy")
@Validated
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @PostMapping("/medications")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicationResponse> registerMedication(@Valid @RequestBody CreateMedicationRequest request) {
        MedicationResponse response = pharmacyService.registerMedication(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/medications/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicationResponse> updateMedication(@PathVariable String id,
                                                               @Valid @RequestBody UpdateMedicationRequest request) {
        MedicationResponse response = pharmacyService.updateMedication(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/medications/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<MedicationResponse> getMedicationById(@PathVariable String id) {
        MedicationResponse response = pharmacyService.getMedicationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/medications")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<PageResponse<MedicationResponse>> searchMedications(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MedicationStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(pharmacyService.searchMedications(query, status, pageable)));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<PharmacyInventoryBatchResponse> addInventoryBatch(@Valid @RequestBody AddInventoryBatchRequest request) {
        PharmacyInventoryBatchResponse response = pharmacyService.addInventoryBatch(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/batches/medication/{medicationId}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<PharmacyInventoryBatchResponse>> getBatchesForMedication(@PathVariable String medicationId) {
        return ResponseEntity.ok(pharmacyService.getBatchesForMedication(medicationId));
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<DispenseRecordResponse> dispenseMedication(@Valid @RequestBody DispenseMedicationRequest request) {
        DispenseRecordResponse response = pharmacyService.dispenseMedication(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dispenses/prescription/{prescriptionId}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<DispenseRecordResponse>> getDispensesForPrescription(@PathVariable String prescriptionId) {
        return ResponseEntity.ok(pharmacyService.getDispensesForPrescription(prescriptionId));
    }
}
