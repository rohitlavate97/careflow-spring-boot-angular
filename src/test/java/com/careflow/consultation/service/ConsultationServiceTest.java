package com.careflow.consultation.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.appointment.service.AppointmentService;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationDiagnosis;
import com.careflow.consultation.domain.ConsultationStatus;
import com.careflow.consultation.domain.ConsultationVitals;
import com.careflow.consultation.domain.DiagnosisType;
import com.careflow.consultation.dto.AddDiagnosisRequest;
import com.careflow.consultation.dto.ConsultationResponse;
import com.careflow.consultation.dto.CreateConsultationRequest;
import com.careflow.consultation.dto.RecordVitalsRequest;
import com.careflow.consultation.dto.UpdateConsultationRequest;
import com.careflow.consultation.exception.ConsultationLockedException;
import com.careflow.consultation.exception.ConsultationNotFoundException;
import com.careflow.consultation.exception.InvalidConsultationStatusTransitionException;
import com.careflow.consultation.mapper.ConsultationMapper;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.queue.service.QueueService;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Spy
    private ConsultationMapper consultationMapper = new ConsultationMapper();

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private ConsultationServiceImpl consultationService;

    private StaffMember doctor;
    private StaffMember nurse;
    private Consultation consultation;

    @BeforeEach
    void setUp() {
        doctor = new StaffMember("doc-1", "DOC-001", "dept-1", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());
        nurse = new StaffMember("nur-1", "NUR-001", "dept-1", "Clara", "Oswald", "clara@careflow.local", "+1234567891", StaffType.NURSE, LocalDate.now());

        consultation = new Consultation(
                "c-1",
                "pat-1",
                "doc-1",
                "apt-1",
                "queue-1",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should successfully start consultation encounter and sync appointment/queue")
    void startConsultation_success() {
        CreateConsultationRequest request = new CreateConsultationRequest("pat-1", "doc-1", "apt-1", "queue-1", "Fever and cough");

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));

        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CHECKED_IN);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(appointment));

        QueueEntry queueEntry = org.mockito.Mockito.mock(QueueEntry.class);
        when(queueEntry.getStatus()).thenReturn(QueueStatus.CALLED);
        when(queueEntryRepository.findById("queue-1")).thenReturn(Optional.of(queueEntry));

        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.startConsultation(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.doctorId()).isEqualTo("doc-1");
        assertThat(response.status()).isEqualTo(ConsultationStatus.STARTED);
        assertThat(response.chiefComplaint()).isEqualTo("Fever and cough");

        verify(appointmentService).startConsultation("apt-1");
        verify(queueService).startConsultation("queue-1");
    }

    @Test
    @DisplayName("Should fail to start consultation when patient is not found")
    void startConsultation_throwsWhenPatientNotFound() {
        CreateConsultationRequest request = new CreateConsultationRequest("missing-pat", "doc-1", null, null, null);
        when(patientRepository.existsById("missing-pat")).thenReturn(false);

        assertThatThrownBy(() -> consultationService.startConsultation(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-pat");
    }

    @Test
    @DisplayName("Should fail to start consultation when staff member is not a DOCTOR")
    void startConsultation_throwsWhenStaffIsNotDoctor() {
        CreateConsultationRequest request = new CreateConsultationRequest("pat-1", "nur-1", null, null, null);
        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("nur-1")).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> consultationService.startConsultation(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only DOCTOR can conduct consultations");
    }

    @Test
    @DisplayName("Should update clinical findings and transition to IN_PROGRESS")
    void updateFindings_success_advancesToInProgress() {
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateConsultationRequest request = new UpdateConsultationRequest(
                "Persistent dry cough",
                "Symptoms started 4 days ago",
                "Lungs clear on auscultation",
                "Prescribe symptomatic relief and rest",
                LocalDate.now().plusDays(7),
                "Return if breathing difficulty develops"
        );

        ConsultationResponse response = consultationService.updateFindings("c-1", request);

        assertThat(response.status()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(response.chiefComplaint()).isEqualTo("Persistent dry cough");
        assertThat(response.treatmentPlan()).isEqualTo("Prescribe symptomatic relief and rest");
        assertThat(response.followUpDate()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    @DisplayName("Should record vitals, compute BMI, and advance to IN_PROGRESS")
    void recordVitals_success() {
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordVitalsRequest vitalsRequest = new RecordVitalsRequest(
                120, 80, 72, 16,
                new BigDecimal("36.8"),
                98,
                new BigDecimal("180.0"),
                new BigDecimal("75.00")
        );

        ConsultationResponse response = consultationService.recordVitals("c-1", vitalsRequest);

        assertThat(response.status()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(response.vitals()).isNotNull();
        assertThat(response.vitals().systolicBp()).isEqualTo(120);
        assertThat(response.vitals().diastolicBp()).isEqualTo(80);
        assertThat(response.vitals().bmi()).isNotNull();
        assertThat(response.vitals().bmi()).isEqualByComparingTo(new BigDecimal("23.1"));
    }

    @Test
    @DisplayName("Should add and remove diagnoses from consultation")
    void addAndRemoveDiagnosis_success() {
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddDiagnosisRequest diagRequest = new AddDiagnosisRequest(
                "J06.9", "Acute upper respiratory infection", DiagnosisType.PRIMARY, "MILD", "Viral etiology suspected"
        );

        ConsultationResponse response = consultationService.addDiagnosis("c-1", diagRequest);

        assertThat(response.diagnoses()).hasSize(1);
        assertThat(response.diagnoses().get(0).diagnosisCode()).isEqualTo("J06.9");
        assertThat(response.diagnoses().get(0).diagnosisType()).isEqualTo(DiagnosisType.PRIMARY);

        String diagnosisId = response.diagnoses().get(0).id();
        ConsultationResponse afterRemove = consultationService.removeDiagnosis("c-1", diagnosisId);
        assertThat(afterRemove.diagnoses()).isEmpty();
    }

    @Test
    @DisplayName("Should fail to complete consultation when no diagnosis and no treatment plan are entered")
    void completeConsultation_throwsWhenIncomplete() {
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));

        assertThatThrownBy(() -> consultationService.completeConsultation("c-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Consultation cannot be completed without at least one diagnosis or treatment plan");
    }

    @Test
    @DisplayName("Should successfully complete consultation and synchronize queue and appointment")
    void completeConsultation_success() {
        consultation.addDiagnosis(new ConsultationDiagnosis("diag-1", consultation, "I10", "Hypertension", DiagnosisType.PRIMARY, "MILD", "Well managed"));
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.completeConsultation("c-1");

        assertThat(response.status()).isEqualTo(ConsultationStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();

        verify(queueService).completeConsultation("queue-1");
        verify(appointmentService).completeConsultation("apt-1");
    }

    @Test
    @DisplayName("Should lock consultation after completion and reject further modifications")
    void lockedConsultation_rejectsModifications() {
        consultation.transitionTo(ConsultationStatus.COMPLETED);
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));

        AddDiagnosisRequest diagRequest = new AddDiagnosisRequest("E11", "Type 2 diabetes", DiagnosisType.SECONDARY, "MILD", null);

        assertThatThrownBy(() -> consultationService.addDiagnosis("c-1", diagRequest))
                .isInstanceOf(ConsultationLockedException.class)
                .hasMessageContaining("locked in status 'COMPLETED'");
    }

    @Test
    @DisplayName("Should cancel consultation and reject invalid state transitions")
    void cancelConsultation_success() {
        when(consultationRepository.findWithDiagnosesById("c-1")).thenReturn(Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.cancelConsultation("c-1", "Patient left before doctor arrival");
        assertThat(response.status()).isEqualTo(ConsultationStatus.CANCELLED);

        // Attempting to advance a cancelled consultation should fail
        assertThatThrownBy(() -> consultation.transitionTo(ConsultationStatus.IN_PROGRESS))
                .isInstanceOf(InvalidConsultationStatusTransitionException.class);
    }
}
