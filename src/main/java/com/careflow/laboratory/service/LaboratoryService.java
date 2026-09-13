package com.careflow.laboratory.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for hospital laboratory operations, diagnostic catalog,
 * accessioning, result entry, and physician review (§27, §103 Phase 9).
 */
public interface LaboratoryService {

    // Catalog
    LabTestResponse createLabTest(CreateLabTestRequest request);

    LabTestResponse updateLabTest(String testId, UpdateLabTestRequest request);

    LabTestResponse getLabTestById(String testId);

    Page<LabTestResponse> getLabTests(Boolean activeOnly, LabTestCategory category, Pageable pageable);

    // Orders
    LabOrderResponse createLabOrder(CreateLabOrderRequest request);

    LabOrderResponse getLabOrderById(String orderId);

    LabOrderResponse getLabOrderByOrderNumber(String orderNumber);

    Page<LabOrderSummaryResponse> getLabOrdersByPatient(String patientId, Pageable pageable);

    Page<LabOrderSummaryResponse> getLabOrdersByDoctor(String doctorId, Pageable pageable);

    Page<LabOrderSummaryResponse> getLabOrdersByStatus(LabOrderStatus status, Pageable pageable);

    Page<LabOrderSummaryResponse> getAllLabOrders(Pageable pageable);

    LabOrderResponse cancelLabOrder(String orderId, CancelLabOrderRequest request);

    // Specimen Collection & Accessioning
    LabSampleResponse collectSample(String orderId, CollectSampleRequest request);

    LabSampleResponse processSample(String sampleId, boolean accept, String rejectionReason);

    // Analytical Processing & Results
    LabOrderResponse startProcessing(String orderId);

    LabResultResponse enterResult(String orderId, EnterLabResultRequest request);

    // Clinical Review & Sign-off
    LabOrderResponse reviewOrder(String orderId, ReviewLabOrderRequest request);
}
