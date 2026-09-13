package com.careflow.pharmacy.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.pharmacy.domain.DispenseRecord;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.dto.AddInventoryBatchRequest;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.dto.DispenseRecordResponse;
import com.careflow.pharmacy.dto.MedicationResponse;
import com.careflow.pharmacy.dto.PharmacyInventoryBatchResponse;
import com.careflow.pharmacy.exception.ExpiredMedicationException;
import com.careflow.pharmacy.exception.InsufficientInventoryException;
import com.careflow.pharmacy.mapper.PharmacyMapper;
import com.careflow.pharmacy.repository.DispenseRecordRepository;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.domain.PrescriptionItemStatus;
import com.careflow.prescription.domain.PrescriptionStatus;
import com.careflow.prescription.repository.PrescriptionItemRepository;
import com.careflow.prescription.repository.PrescriptionRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private PharmacyInventoryBatchRepository batchRepository;

    @Mock
    private DispenseRecordRepository dispenseRecordRepository;

    @Spy
    private PharmacyMapper pharmacyMapper = new PharmacyMapper();

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionItemRepository prescriptionItemRepository;

    @InjectMocks
    private PharmacyServiceImpl pharmacyService;

    private StaffMember pharmacist;
    private StaffMember doctor;
    private Medication medication;
    private PharmacyInventoryBatch batch;
    private Prescription prescription;
    private PrescriptionItem prescriptionItem;

    @BeforeEach
    void setUp() {
        pharmacist = new StaffMember("pharm-1", "PHARM-001", "dept-pharm", "Amy", "Pond", "amy@careflow.local", "+1234567895", StaffType.PHARMACIST, LocalDate.now());
        doctor = new StaffMember("doc-1", "DOC-001", "dept-med", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());

        medication = new Medication("med-1", "MED-AMOX-500", "Amoxicillin", "Amoxicillin Trihydrate", MedicationForm.CAPSULE, "500 mg", BigDecimal.valueOf(15.00), 20);
        batch = new PharmacyInventoryBatch("batch-1", "med-1", "BATCH-2026-A", LocalDate.now().plusMonths(12), 50, 10);

        prescription = new Prescription("rx-1", "pat-1", "doc-1", null, null, null);
        prescriptionItem = new PrescriptionItem("item-1", prescription, "med-1", "500 mg", "Twice daily", "5 days", 10, null);
        prescription.addItem(prescriptionItem);
    }

    @Test
    @DisplayName("Should successfully register a new medication")
    void registerMedication_success() {
        CreateMedicationRequest request = new CreateMedicationRequest(
                "MED-PARA-500", "Paracetamol", "Acetaminophen", MedicationForm.TABLET, "500 mg", BigDecimal.valueOf(5.00), 30
        );

        when(medicationRepository.existsByCodeIgnoreCase("MED-PARA-500")).thenReturn(false);
        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicationResponse response = pharmacyService.registerMedication(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("MED-PARA-500");
        assertThat(response.name()).isEqualTo("Paracetamol");
        assertThat(response.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException on duplicate medication code")
    void registerMedication_duplicateCodeThrows() {
        CreateMedicationRequest request = new CreateMedicationRequest(
                "MED-AMOX-500", "Amoxicillin", "Amoxicillin", MedicationForm.CAPSULE, "500 mg", BigDecimal.valueOf(10.00), 10
        );
        when(medicationRepository.existsByCodeIgnoreCase("MED-AMOX-500")).thenReturn(true);

        assertThatThrownBy(() -> pharmacyService.registerMedication(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists in the formulary");
    }

    @Test
    @DisplayName("Should successfully add an inventory batch")
    void addInventoryBatch_success() {
        AddInventoryBatchRequest request = new AddInventoryBatchRequest(
                "med-1", "BATCH-NEW-01", LocalDate.now().plusYears(1), 100, 20
        );

        when(medicationRepository.existsById("med-1")).thenReturn(true);
        when(batchRepository.findByMedicationIdAndBatchNumber("med-1", "BATCH-NEW-01")).thenReturn(Optional.empty());
        when(batchRepository.save(any(PharmacyInventoryBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PharmacyInventoryBatchResponse response = pharmacyService.addInventoryBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.batchNumber()).isEqualTo("BATCH-NEW-01");
        assertThat(response.quantityAvailable()).isEqualTo(100);
        assertThat(response.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should dispense medication, decrement stock atomically, and transition prescription to DISPENSED")
    void dispenseMedication_success() {
        DispenseMedicationRequest request = new DispenseMedicationRequest(
                "rx-1", "item-1", "batch-1", "pharm-1", 10, "Dispensed at counter 1"
        );

        when(staffMemberRepository.findById("pharm-1")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepository.findWithItemsById("rx-1")).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findById("item-1")).thenReturn(Optional.of(prescriptionItem));
        when(batchRepository.findByIdForUpdate("batch-1")).thenReturn(Optional.of(batch));
        when(dispenseRecordRepository.save(any(DispenseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DispenseRecordResponse response = pharmacyService.dispenseMedication(request);

        assertThat(response).isNotNull();
        assertThat(response.quantityDispensed()).isEqualTo(10);
        assertThat(batch.getQuantityAvailable()).isEqualTo(40); // 50 - 10
        assertThat(prescriptionItem.getStatus()).isEqualTo(PrescriptionItemStatus.DISPENSED);
        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.DISPENSED);

        verify(batchRepository).saveAndFlush(batch);
        verify(prescriptionItemRepository).saveAndFlush(prescriptionItem);
        verify(dispenseRecordRepository).save(any(DispenseRecord.class));
    }

    @Test
    @DisplayName("Should throw InsufficientInventoryException when requested quantity exceeds available stock")
    void dispenseMedication_insufficientInventoryThrows() {
        DispenseMedicationRequest request = new DispenseMedicationRequest(
                "rx-1", "item-1", "batch-1", "pharm-1", 60, null // Only 50 in batch
        );

        when(staffMemberRepository.findById("pharm-1")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepository.findWithItemsById("rx-1")).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findById("item-1")).thenReturn(Optional.of(prescriptionItem));
        when(batchRepository.findByIdForUpdate("batch-1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> pharmacyService.dispenseMedication(request))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("only 50 units available");
    }

    @Test
    @DisplayName("Should throw ExpiredMedicationException when batch is expired")
    void dispenseMedication_expiredBatchThrows() {
        PharmacyInventoryBatch expiredBatch = new PharmacyInventoryBatch(
                "batch-exp", "med-1", "BATCH-OLD", LocalDate.now().minusDays(1), 10, 5
        );

        DispenseMedicationRequest request = new DispenseMedicationRequest(
                "rx-1", "item-1", "batch-exp", "pharm-1", 5, null
        );

        when(staffMemberRepository.findById("pharm-1")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepository.findWithItemsById("rx-1")).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findById("item-1")).thenReturn(Optional.of(prescriptionItem));
        when(batchRepository.findByIdForUpdate("batch-exp")).thenReturn(Optional.of(expiredBatch));

        assertThatThrownBy(() -> pharmacyService.dispenseMedication(request))
                .isInstanceOf(ExpiredMedicationException.class)
                .hasMessageContaining("expired on");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when non-pharmacist attempts dispensing")
    void dispenseMedication_nonPharmacistThrowsForbidden() {
        DispenseMedicationRequest request = new DispenseMedicationRequest(
                "rx-1", "item-1", "batch-1", "doc-1", 5, null
        );
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> pharmacyService.dispenseMedication(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only PHARMACIST or ADMIN can dispense medications");
    }
}
