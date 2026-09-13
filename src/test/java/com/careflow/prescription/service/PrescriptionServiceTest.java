package com.careflow.prescription.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionStatus;
import com.careflow.prescription.dto.CreatePrescriptionRequest;
import com.careflow.prescription.dto.PrescriptionItemRequest;
import com.careflow.prescription.dto.PrescriptionResponse;
import com.careflow.prescription.mapper.PrescriptionMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Spy
    private PrescriptionMapper prescriptionMapper = new PrescriptionMapper();

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    private StaffMember doctor;
    private StaffMember nurse;

    @BeforeEach
    void setUp() {
        doctor = new StaffMember("doc-1", "DOC-001", "dept-1", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());
        nurse = new StaffMember("nur-1", "NUR-001", "dept-1", "Clara", "Oswald", "clara@careflow.local", "+1234567891", StaffType.NURSE, LocalDate.now());
    }

    @Test
    @DisplayName("Should successfully issue prescription with items when ordered by DOCTOR")
    void issuePrescription_success() {
        PrescriptionItemRequest item = new PrescriptionItemRequest(
                "med-1", "500 mg", "Twice daily", "5 days", 10, "Take after food"
        );
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                "pat-1", "doc-1", null, "Patient in severe pain", List.of(item)
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(medicationRepository.existsById("med-1")).thenReturn(true);
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse response = prescriptionService.issuePrescription(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.doctorId()).isEqualTo("doc-1");
        assertThat(response.status()).isEqualTo(PrescriptionStatus.PENDING_DISPENSE);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).dosage()).isEqualTo("500 mg");
        assertThat(response.items().get(0).quantityPrescribed()).isEqualTo(10);

        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient does not exist")
    void issuePrescription_throwsWhenPatientNotFound() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest("missing-pat", "doc-1", null, null, List.of());
        when(patientRepository.existsById("missing-pat")).thenReturn(false);

        assertThatThrownBy(() -> prescriptionService.issuePrescription(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-pat");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when prescribing staff is not DOCTOR")
    void issuePrescription_throwsWhenStaffIsNotDoctor() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest("pat-1", "nur-1", null, null, List.of());
        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("nur-1")).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> prescriptionService.issuePrescription(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only DOCTOR can prescribe medications");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when prescribed medication does not exist")
    void issuePrescription_throwsWhenMedicationNotFound() {
        PrescriptionItemRequest item = new PrescriptionItemRequest("missing-med", "500 mg", "Once daily", "3 days", 5, null);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest("pat-1", "doc-1", null, null, List.of(item));

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(medicationRepository.existsById("missing-med")).thenReturn(false);

        assertThatThrownBy(() -> prescriptionService.issuePrescription(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-med");
    }

    @Test
    @DisplayName("Should successfully cancel prescription in PENDING_DISPENSE state")
    void cancelPrescription_success() {
        Prescription prescription = new Prescription("rx-1", "pat-1", "doc-1", null, null, null);
        when(prescriptionRepository.findWithItemsById("rx-1")).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse response = prescriptionService.cancelPrescription("rx-1", "Adverse reaction reported");

        assertThat(response.status()).isEqualTo(PrescriptionStatus.CANCELLED);
    }
}
