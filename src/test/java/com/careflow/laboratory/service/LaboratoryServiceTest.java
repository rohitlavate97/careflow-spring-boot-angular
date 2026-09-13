package com.careflow.laboratory.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.laboratory.domain.AbnormalityFlag;
import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderItem;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabReviewStatus;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SampleStatus;
import com.careflow.laboratory.domain.SpecimenType;
import com.careflow.laboratory.dto.CancelLabOrderRequest;
import com.careflow.laboratory.dto.CollectSampleRequest;
import com.careflow.laboratory.dto.CreateLabOrderItemRequest;
import com.careflow.laboratory.dto.CreateLabOrderRequest;
import com.careflow.laboratory.dto.CreateLabTestRequest;
import com.careflow.laboratory.dto.EnterLabResultRequest;
import com.careflow.laboratory.dto.LabOrderResponse;
import com.careflow.laboratory.dto.LabResultResponse;
import com.careflow.laboratory.dto.LabSampleResponse;
import com.careflow.laboratory.dto.LabTestResponse;
import com.careflow.laboratory.dto.ReviewLabOrderRequest;
import com.careflow.laboratory.exception.InvalidLabOrderStatusTransitionException;
import com.careflow.laboratory.mapper.LaboratoryMapper;
import com.careflow.laboratory.repository.LabOrderItemRepository;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.laboratory.repository.LabResultRepository;
import com.careflow.laboratory.repository.LabSampleRepository;
import com.careflow.laboratory.repository.LabTestRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratoryServiceTest {

    @Mock
    private LabTestRepository labTestRepository;

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private LabOrderItemRepository labOrderItemRepository;

    @Mock
    private LabSampleRepository labSampleRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Spy
    private LaboratoryMapper laboratoryMapper = new LaboratoryMapper();

    @InjectMocks
    private LaboratoryServiceImpl laboratoryService;

    private StaffMember doctor;
    private StaffMember nurse;
    private StaffMember labTech;
    private LabTest cbcTest;

    @BeforeEach
    void setUp() {
        doctor = new StaffMember("doc-1", "DOC-001", "dept-1", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());
        nurse = new StaffMember("nur-1", "NUR-001", "dept-1", "Florence", "Nightingale", "nurse@careflow.local", "+1234567891", StaffType.NURSE, LocalDate.now());
        labTech = new StaffMember("staff-lab-001", "LAB-001", "dept-1", "Marie", "Curie", "lab@careflow.local", "+1234567892", StaffType.LAB_TECHNICIAN, LocalDate.now());

        cbcTest = new LabTest(
                "test-cbc", "CBC", "Complete Blood Count", LabTestCategory.HEMATOLOGY,
                SpecimenType.BLOOD, "4.5-11.0", "10^3/uL", 4, BigDecimal.valueOf(18.00), true
        );
    }

    @Test
    @DisplayName("Should successfully create a new lab test catalog definition")
    void createLabTest_success() {
        CreateLabTestRequest request = new CreateLabTestRequest(
                "BMP", "Basic Metabolic Panel", LabTestCategory.BIOCHEMISTRY,
                SpecimenType.BLOOD, "70-99", "mg/dL", 6, BigDecimal.valueOf(25.00)
        );

        when(labTestRepository.existsByCode("BMP")).thenReturn(false);
        when(labTestRepository.save(any(LabTest.class))).thenAnswer(i -> i.getArgument(0));

        LabTestResponse response = laboratoryService.createLabTest(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("BMP");
        assertThat(response.name()).isEqualTo("Basic Metabolic Panel");
        assertThat(response.price()).isEqualTo(BigDecimal.valueOf(25.00));
        verify(labTestRepository).save(any(LabTest.class));
    }

    @Test
    @DisplayName("Should reject duplicate lab test code")
    void createLabTest_duplicateCode_throwsException() {
        CreateLabTestRequest request = new CreateLabTestRequest(
                "CBC", "Duplicate CBC", LabTestCategory.HEMATOLOGY,
                SpecimenType.BLOOD, null, null, 4, BigDecimal.valueOf(18.00)
        );

        when(labTestRepository.existsByCode("CBC")).thenReturn(true);

        assertThatThrownBy(() -> laboratoryService.createLabTest(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should successfully create a lab order requisition when placed by DOCTOR")
    void createLabOrder_success() {
        CreateLabOrderRequest request = new CreateLabOrderRequest(
                "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, "Routine checkup",
                List.of(new CreateLabOrderItemRequest("test-cbc", "Check for anemia"))
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(labTestRepository.findById("test-cbc")).thenReturn(Optional.of(cbcTest));
        when(labOrderRepository.save(any(LabOrder.class))).thenAnswer(i -> i.getArgument(0));

        LabOrderResponse response = laboratoryService.createLabOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.orderingDoctorId()).isEqualTo("doc-1");
        assertThat(response.status()).isEqualTo(LabOrderStatus.ORDERED);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).testCode()).isEqualTo("CBC");
    }

    @Test
    @DisplayName("Should reject lab order creation when staff member is not DOCTOR")
    void createLabOrder_invalidDoctor_throwsException() {
        CreateLabOrderRequest request = new CreateLabOrderRequest(
                "pat-1", "nur-1", null, LabOrderPriority.ROUTINE, "Nurse order",
                List.of(new CreateLabOrderItemRequest("test-cbc", null))
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("nur-1")).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> laboratoryService.createLabOrder(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only DOCTOR can perform this action");
    }

    @Test
    @DisplayName("Should collect specimen and transition order to SAMPLE_COLLECTED")
    void collectSample_transitionsToSampleCollected() {
        LabOrder order = new LabOrder("ord-1", "ORD-LAB-001", "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, null, Instant.now());
        order.addItem(new LabOrderItem("item-1", cbcTest, null));

        when(labOrderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(labSampleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(labOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CollectSampleRequest request = new CollectSampleRequest(SpecimenType.BLOOD, "Tube adequate", "staff-lab-001");
        LabSampleResponse sampleResponse = laboratoryService.collectSample("ord-1", request);

        assertThat(sampleResponse).isNotNull();
        assertThat(sampleResponse.specimenType()).isEqualTo(SpecimenType.BLOOD);
        assertThat(sampleResponse.status()).isEqualTo(SampleStatus.COLLECTED);
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.SAMPLE_COLLECTED);
    }

    @Test
    @DisplayName("Should start processing and transition order to PROCESSING")
    void startProcessing_transitionsToProcessing() {
        LabOrder order = new LabOrder("ord-1", "ORD-LAB-001", "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, null, Instant.now());
        order.addItem(new LabOrderItem("item-1", cbcTest, null));
        order.setStatus(LabOrderStatus.SAMPLE_COLLECTED);

        when(labOrderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(labOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LabOrderResponse response = laboratoryService.startProcessing("ord-1");

        assertThat(response.status()).isEqualTo(LabOrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("Should enter result and automatically complete order when all items are tested")
    void enterResult_autoCompletesOrder() {
        LabOrder order = new LabOrder("ord-1", "ORD-LAB-001", "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, null, Instant.now());
        LabOrderItem item = new LabOrderItem("item-1", cbcTest, null);
        order.addItem(item);
        order.setStatus(LabOrderStatus.PROCESSING);

        when(labOrderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(labResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(labOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EnterLabResultRequest request = new EnterLabResultRequest(
                "item-1", null, "Hemoglobin", "14.5", 14.5, "g/dL", "13.5-17.5",
                AbnormalityFlag.NORMAL, "Normal levels", "staff-lab-001"
        );

        LabResultResponse resultResponse = laboratoryService.enterResult("ord-1", request);

        assertThat(resultResponse).isNotNull();
        assertThat(resultResponse.testParameter()).isEqualTo("Hemoglobin");
        assertThat(resultResponse.abnormalityFlag()).isEqualTo(AbnormalityFlag.NORMAL);
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should allow physician review when order is COMPLETED")
    void reviewOrder_success() {
        LabOrder order = new LabOrder("ord-1", "ORD-LAB-001", "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, null, Instant.now());
        order.complete();

        when(labOrderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(labOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReviewLabOrderRequest request = new ReviewLabOrderRequest(LabReviewStatus.REVIEWED, "All within normal limits", "doc-1");
        LabOrderResponse response = laboratoryService.reviewOrder("ord-1", request);

        assertThat(response.reviewStatus()).isEqualTo(LabReviewStatus.REVIEWED);
        assertThat(response.reviewedById()).isEqualTo("doc-1");
        assertThat(response.reviewNotes()).isEqualTo("All within normal limits");
    }

    @Test
    @DisplayName("Should reject cancellation on COMPLETED lab order")
    void cancelLabOrder_whenCompleted_throwsException() {
        LabOrder order = new LabOrder("ord-1", "ORD-LAB-001", "pat-1", "doc-1", null, LabOrderPriority.ROUTINE, null, Instant.now());
        order.complete();

        when(labOrderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        CancelLabOrderRequest request = new CancelLabOrderRequest("Patient decided not to test");

        assertThatThrownBy(() -> laboratoryService.cancelLabOrder("ord-1", request))
                .isInstanceOf(InvalidLabOrderStatusTransitionException.class)
                .hasMessageContaining("Cannot cancel a completed lab order");
    }
}
