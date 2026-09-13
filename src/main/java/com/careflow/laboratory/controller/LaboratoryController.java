package com.careflow.laboratory.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.dto.CancelLabOrderRequest;
import com.careflow.laboratory.dto.CollectSampleRequest;
import com.careflow.laboratory.dto.CreateLabOrderRequest;
import com.careflow.laboratory.dto.CreateLabTestRequest;
import com.careflow.laboratory.dto.EnterLabResultRequest;
import com.careflow.laboratory.dto.LabOrderResponse;
import com.careflow.laboratory.dto.LabOrderSummaryResponse;
import com.careflow.laboratory.dto.LabResultResponse;
import com.careflow.laboratory.dto.LabSampleResponse;
import com.careflow.laboratory.dto.LabTestResponse;
import com.careflow.laboratory.dto.ReviewLabOrderRequest;
import com.careflow.laboratory.dto.UpdateLabTestRequest;
import com.careflow.laboratory.service.LaboratoryService;
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

/**
 * REST controller for Laboratory catalog, requisitions, specimen accessioning,
 * results, and clinical reviews (§27, §39, §44, §103 Phase 9).
 */
@RestController
@RequestMapping("/api/v1/laboratory")
@Validated
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Catalog Endpoints
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/tests")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECHNICIAN')")
    public ResponseEntity<LabTestResponse> createLabTest(@Valid @RequestBody CreateLabTestRequest request) {
        LabTestResponse response = laboratoryService.createLabTest(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/tests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECHNICIAN')")
    public ResponseEntity<LabTestResponse> updateLabTest(@PathVariable String id,
                                                         @Valid @RequestBody UpdateLabTestRequest request) {
        LabTestResponse response = laboratoryService.updateLabTest(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LabTestResponse> getLabTestById(@PathVariable String id) {
        return ResponseEntity.ok(laboratoryService.getLabTestById(id));
    }

    @GetMapping("/tests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<LabTestResponse>> getLabTests(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) LabTestCategory category,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(laboratoryService.getLabTests(activeOnly, category, pageable)));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Lab Order Requisitions
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> createLabOrder(@Valid @RequestBody CreateLabOrderRequest request) {
        LabOrderResponse response = laboratoryService.createLabOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_TECHNICIAN', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<LabOrderResponse> getLabOrderById(@PathVariable String id) {
        return ResponseEntity.ok(laboratoryService.getLabOrderById(id));
    }

    @GetMapping("/orders/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_TECHNICIAN', 'NURSE', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> getLabOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(laboratoryService.getLabOrderByOrderNumber(orderNumber));
    }

    @GetMapping("/orders/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_TECHNICIAN', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<PageResponse<LabOrderSummaryResponse>> getLabOrdersByPatient(
            @PathVariable String patientId,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(laboratoryService.getLabOrdersByPatient(patientId, pageable)));
    }

    @GetMapping("/orders/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PageResponse<LabOrderSummaryResponse>> getLabOrdersByDoctor(
            @PathVariable String doctorId,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(laboratoryService.getLabOrdersByDoctor(doctorId, pageable)));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_TECHNICIAN', 'NURSE', 'ADMIN')")
    public ResponseEntity<PageResponse<LabOrderSummaryResponse>> getLabOrders(
            @RequestParam(required = false) LabOrderStatus status,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(PageResponse.from(laboratoryService.getLabOrdersByStatus(status, pageable)));
        }
        return ResponseEntity.ok(PageResponse.from(laboratoryService.getAllLabOrders(pageable)));
    }

    @PutMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> cancelLabOrder(@PathVariable String id,
                                                           @Valid @RequestBody CancelLabOrderRequest request) {
        return ResponseEntity.ok(laboratoryService.cancelLabOrder(id, request));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Specimen Collection & Accessioning
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/orders/{id}/samples")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'NURSE', 'ADMIN')")
    public ResponseEntity<LabSampleResponse> collectSample(@PathVariable String id,
                                                           @Valid @RequestBody CollectSampleRequest request) {
        LabSampleResponse response = laboratoryService.collectSample(id, request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/samples/{id}/process")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN')")
    public ResponseEntity<LabSampleResponse> processSample(
            @PathVariable String id,
            @RequestParam boolean accept,
            @RequestParam(required = false) String rejectionReason) {
        return ResponseEntity.ok(laboratoryService.processSample(id, accept, rejectionReason));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Analytical Processing & Results
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/orders/{id}/process")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> startProcessing(@PathVariable String id) {
        return ResponseEntity.ok(laboratoryService.startProcessing(id));
    }

    @PostMapping("/orders/{id}/results")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN')")
    public ResponseEntity<LabResultResponse> enterResult(@PathVariable String id,
                                                         @Valid @RequestBody EnterLabResultRequest request) {
        LabResultResponse response = laboratoryService.enterResult(id, request);
        return ResponseEntity.status(201).body(response);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Clinical Review & Sign-off
    // -----------------------------------------------------------------------------------------------------------------

    @PostMapping("/orders/{id}/review")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LabOrderResponse> reviewOrder(@PathVariable String id,
                                                        @Valid @RequestBody ReviewLabOrderRequest request) {
        return ResponseEntity.ok(laboratoryService.reviewOrder(id, request));
    }
}
