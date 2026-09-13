package com.careflow.laboratory.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.laboratory.domain.AbnormalityFlag;
import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderItem;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabResult;
import com.careflow.laboratory.domain.LabSample;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.dto.CancelLabOrderRequest;
import com.careflow.laboratory.dto.CollectSampleRequest;
import com.careflow.laboratory.dto.CreateLabOrderItemRequest;
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
import com.careflow.laboratory.exception.LabOrderItemNotFoundException;
import com.careflow.laboratory.exception.LabOrderNotFoundException;
import com.careflow.laboratory.exception.LabSampleNotFoundException;
import com.careflow.laboratory.exception.LabTestNotFoundException;
import com.careflow.laboratory.mapper.LaboratoryMapper;
import com.careflow.laboratory.repository.LabOrderItemRepository;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.laboratory.repository.LabResultRepository;
import com.careflow.laboratory.repository.LabSampleRepository;
import com.careflow.laboratory.repository.LabTestRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Core business service implementing hospital laboratory workflows (§27, §103 Phase 9).
 */
@Service
public class LaboratoryServiceImpl implements LaboratoryService {

    private static final Logger log = LoggerFactory.getLogger(LaboratoryServiceImpl.class);

    private final LabTestRepository labTestRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final LabSampleRepository labSampleRepository;
    private final LabResultRepository labResultRepository;
    private final LaboratoryMapper laboratoryMapper;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ConsultationRepository consultationRepository;

    public LaboratoryServiceImpl(LabTestRepository labTestRepository,
                                 LabOrderRepository labOrderRepository,
                                 LabOrderItemRepository labOrderItemRepository,
                                 LabSampleRepository labSampleRepository,
                                 LabResultRepository labResultRepository,
                                 LaboratoryMapper laboratoryMapper,
                                 PatientRepository patientRepository,
                                 StaffMemberRepository staffMemberRepository,
                                 ConsultationRepository consultationRepository) {
        this.labTestRepository = labTestRepository;
        this.labOrderRepository = labOrderRepository;
        this.labOrderItemRepository = labOrderItemRepository;
        this.labSampleRepository = labSampleRepository;
        this.labResultRepository = labResultRepository;
        this.laboratoryMapper = laboratoryMapper;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.consultationRepository = consultationRepository;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Catalog
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public LabTestResponse createLabTest(CreateLabTestRequest request) {
        log.info("Creating lab test definition with code='{}'", request.code());

        if (labTestRepository.existsByCode(request.code().trim())) {
            throw new BusinessRuleException(
                    "DUPLICATE_LAB_TEST_CODE",
                    "Lab test with code '" + request.code().trim() + "' already exists.",
                    HttpStatus.CONFLICT
            );
        }

        String testId = UUID.randomUUID().toString();
        LabTest labTest = new LabTest(
                testId,
                request.code().trim().toUpperCase(),
                request.name().trim(),
                request.category(),
                request.specimenType(),
                request.referenceRange() != null ? request.referenceRange().trim() : null,
                request.unit() != null ? request.unit().trim() : null,
                request.turnaroundHours(),
                request.price(),
                true
        );

        LabTest saved = labTestRepository.save(labTest);
        return laboratoryMapper.toLabTestResponse(saved);
    }

    @Override
    @Transactional
    public LabTestResponse updateLabTest(String testId, UpdateLabTestRequest request) {
        log.info("Updating lab test definition id='{}'", testId);
        LabTest labTest = labTestRepository.findById(testId.trim())
                .orElseThrow(() -> new LabTestNotFoundException(testId));

        labTest.setName(request.name().trim());
        labTest.setCategory(request.category());
        labTest.setSpecimenType(request.specimenType());
        labTest.setReferenceRange(request.referenceRange() != null ? request.referenceRange().trim() : null);
        labTest.setUnit(request.unit() != null ? request.unit().trim() : null);
        labTest.setTurnaroundHours(request.turnaroundHours());
        labTest.setPrice(request.price());
        labTest.setActive(request.active());

        LabTest updated = labTestRepository.save(labTest);
        return laboratoryMapper.toLabTestResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public LabTestResponse getLabTestById(String testId) {
        LabTest labTest = labTestRepository.findById(testId.trim())
                .orElseThrow(() -> new LabTestNotFoundException(testId));
        return laboratoryMapper.toLabTestResponse(labTest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabTestResponse> getLabTests(Boolean activeOnly, LabTestCategory category, Pageable pageable) {
        if (Boolean.TRUE.equals(activeOnly)) {
            if (category != null) {
                return labTestRepository.findByCategoryAndActiveTrue(category, pageable)
                        .map(laboratoryMapper::toLabTestResponse);
            }
            return labTestRepository.findByActiveTrue(pageable)
                    .map(laboratoryMapper::toLabTestResponse);
        }
        return labTestRepository.findAll(pageable)
                .map(laboratoryMapper::toLabTestResponse);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Lab Orders
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public LabOrderResponse createLabOrder(CreateLabOrderRequest request) {
        log.info("Creating lab order for patient id='{}', doctor id='{}'",
                request.patientId(), request.doctorId());

        validatePatient(request.patientId());
        validateDoctor(request.doctorId());

        String encounterId = null;
        if (request.encounterId() != null && !request.encounterId().isBlank()) {
            encounterId = request.encounterId().trim();
            if (!consultationRepository.existsById(encounterId)) {
                throw new ResourceNotFoundException("Consultation", encounterId);
            }
        }

        String orderId = UUID.randomUUID().toString();
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = String.format("ORD-LAB-%s-%s", datePart, uniqueSuffix);

        LabOrder order = new LabOrder(
                orderId,
                orderNumber,
                request.patientId().trim(),
                request.doctorId().trim(),
                encounterId,
                request.priority() != null ? request.priority() : LabOrderPriority.ROUTINE,
                request.clinicalNotes() != null ? request.clinicalNotes().trim() : null,
                Instant.now()
        );

        for (CreateLabOrderItemRequest itemReq : request.items()) {
            LabTest test = labTestRepository.findById(itemReq.labTestId().trim())
                    .orElseThrow(() -> new LabTestNotFoundException(itemReq.labTestId()));

            if (!test.isActive()) {
                throw new BusinessRuleException(
                        "INACTIVE_LAB_TEST",
                        "Cannot order inactive lab test: " + test.getName(),
                        HttpStatus.BAD_REQUEST
                );
            }

            LabOrderItem item = new LabOrderItem(
                    UUID.randomUUID().toString(),
                    test,
                    itemReq.notes() != null ? itemReq.notes().trim() : null
            );
            order.addItem(item);
        }

        LabOrder saved = labOrderRepository.save(order);
        return laboratoryMapper.toLabOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LabOrderResponse getLabOrderById(String orderId) {
        LabOrder order = labOrderRepository.findByIdWithDetails(orderId.trim())
                .or(() -> labOrderRepository.findById(orderId.trim()))
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));
        return laboratoryMapper.toLabOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public LabOrderResponse getLabOrderByOrderNumber(String orderNumber) {
        LabOrder order = labOrderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderNumber));
        return laboratoryMapper.toLabOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderSummaryResponse> getLabOrdersByPatient(String patientId, Pageable pageable) {
        validatePatient(patientId);
        return labOrderRepository.findByPatientIdOrderByOrderedAtDesc(patientId.trim(), pageable)
                .map(laboratoryMapper::toLabOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderSummaryResponse> getLabOrdersByDoctor(String doctorId, Pageable pageable) {
        validateDoctor(doctorId);
        return labOrderRepository.findByOrderingDoctorIdOrderByOrderedAtDesc(doctorId.trim(), pageable)
                .map(laboratoryMapper::toLabOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderSummaryResponse> getLabOrdersByStatus(LabOrderStatus status, Pageable pageable) {
        return labOrderRepository.findByStatusOrderByOrderedAtDesc(status, pageable)
                .map(laboratoryMapper::toLabOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderSummaryResponse> getAllLabOrders(Pageable pageable) {
        return labOrderRepository.findAll(pageable)
                .map(laboratoryMapper::toLabOrderSummaryResponse);
    }

    @Override
    @Transactional
    public LabOrderResponse cancelLabOrder(String orderId, CancelLabOrderRequest request) {
        log.info("Cancelling lab order id='{}', reason='{}'", orderId, request.cancellationReason());
        LabOrder order = labOrderRepository.findById(orderId.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));

        order.cancel(request.cancellationReason().trim());
        LabOrder saved = labOrderRepository.save(order);
        return laboratoryMapper.toLabOrderResponse(saved);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Specimen Collection & Accessioning
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public LabSampleResponse collectSample(String orderId, CollectSampleRequest request) {
        log.info("Collecting specimen for lab order id='{}', type='{}'", orderId, request.specimenType());
        LabOrder order = labOrderRepository.findById(orderId.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));

        String collectorId = request.collectorStaffId() != null && !request.collectorStaffId().isBlank()
                ? request.collectorStaffId().trim()
                : "staff-lab-001";

        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String barcode = String.format("SMP-%s-%s", datePart, UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        LabSample sample = new LabSample(
                UUID.randomUUID().toString(),
                barcode,
                request.specimenType(),
                collectorId,
                Instant.now(),
                request.conditionNotes() != null ? request.conditionNotes().trim() : null
        );

        order.addSample(sample);
        LabSample savedSample = labSampleRepository.save(sample);
        labOrderRepository.save(order);

        return laboratoryMapper.toLabSampleResponse(savedSample);
    }

    @Override
    @Transactional
    public LabSampleResponse processSample(String sampleId, boolean accept, String rejectionReason) {
        log.info("Accessioning specimen sample id='{}', accept='{}'", sampleId, accept);
        LabSample sample = labSampleRepository.findById(sampleId.trim())
                .orElseThrow(() -> new LabSampleNotFoundException(sampleId));

        if (accept) {
            sample.accept();
        } else {
            sample.reject(rejectionReason != null ? rejectionReason.trim() : "Rejected during lab accessioning");
        }

        LabSample saved = labSampleRepository.save(sample);
        return laboratoryMapper.toLabSampleResponse(saved);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Analytical Processing & Results
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public LabOrderResponse startProcessing(String orderId) {
        log.info("Starting processing on lab order id='{}'", orderId);
        LabOrder order = labOrderRepository.findById(orderId.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));

        order.startProcessing();
        LabOrder saved = labOrderRepository.save(order);
        return laboratoryMapper.toLabOrderResponse(saved);
    }

    @Override
    @Transactional
    public LabResultResponse enterResult(String orderId, EnterLabResultRequest request) {
        log.info("Entering lab result on order id='{}', parameter='{}'", orderId, request.testParameter());
        LabOrder order = labOrderRepository.findById(orderId.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));

        if (order.getStatus() == LabOrderStatus.ORDERED) {
            order.startProcessing();
        }

        LabOrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(request.orderItemId().trim()))
                .findFirst()
                .orElseThrow(() -> new LabOrderItemNotFoundException(request.orderItemId()));

        LabSample sample = null;
        if (request.sampleId() != null && !request.sampleId().isBlank()) {
            sample = labSampleRepository.findById(request.sampleId().trim()).orElse(null);
        }

        String technicianId = request.technicianStaffId() != null && !request.technicianStaffId().isBlank()
                ? request.technicianStaffId().trim()
                : "staff-lab-001";

        AbnormalityFlag flag = request.abnormalityFlag() != null
                ? request.abnormalityFlag()
                : AbnormalityFlag.NORMAL;

        LabResult result = new LabResult(
                UUID.randomUUID().toString(),
                item,
                sample,
                request.testParameter().trim(),
                request.resultValue().trim(),
                request.numericValue(),
                request.unit() != null ? request.unit().trim() : null,
                request.referenceRange() != null ? request.referenceRange().trim() : null,
                flag,
                technicianId,
                Instant.now(),
                request.technicianNotes() != null ? request.technicianNotes().trim() : null
        );

        item.addResult(result);
        LabResult savedResult = labResultRepository.save(result);

        // Check if all items in order have at least one result recorded -> auto-complete order
        boolean allItemsHaveResults = order.getItems().stream()
                .allMatch(i -> !i.getResults().isEmpty());
        if (allItemsHaveResults) {
            order.complete();
        }

        labOrderRepository.save(order);
        return laboratoryMapper.toLabResultResponse(savedResult);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Clinical Review & Sign-off
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public LabOrderResponse reviewOrder(String orderId, ReviewLabOrderRequest request) {
        log.info("Reviewing lab order id='{}', status='{}'", orderId, request.reviewStatus());
        LabOrder order = labOrderRepository.findById(orderId.trim())
                .orElseThrow(() -> new LabOrderNotFoundException(orderId));

        String reviewerId = request.reviewerDoctorId() != null && !request.reviewerDoctorId().isBlank()
                ? request.reviewerDoctorId().trim()
                : order.getOrderingDoctorId();

        validateDoctor(reviewerId);

        order.review(
                reviewerId,
                request.reviewStatus(),
                request.reviewNotes() != null ? request.reviewNotes().trim() : null
        );

        LabOrder saved = labOrderRepository.save(order);
        return laboratoryMapper.toLabOrderResponse(saved);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Validation Helpers
    // -----------------------------------------------------------------------------------------------------------------

    private void validatePatient(String patientId) {
        if (!patientRepository.existsById(patientId.trim())) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }

    private void validateDoctor(String doctorId) {
        StaffMember staff = staffMemberRepository.findById(doctorId.trim())
                .orElseThrow(() -> new StaffNotFoundException(doctorId));

        if (staff.getStaffType() != StaffType.DOCTOR) {
            throw new BusinessRuleException(
                    "INVALID_STAFF_TYPE",
                    String.format("Staff member '%s' is of type '%s', but only DOCTOR can perform this action.",
                            doctorId, staff.getStaffType()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
