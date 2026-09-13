package com.careflow.queue.service;

import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.department.service.DepartmentService;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;
import com.careflow.queue.dto.DepartmentQueueLiveStatusResponse;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;
import com.careflow.queue.dto.QueueSummaryResponse;
import com.careflow.queue.exception.InvalidQueueStatusTransitionException;
import com.careflow.queue.exception.QueueEmptyException;
import com.careflow.queue.exception.QueueEntryNotFoundException;
import com.careflow.queue.mapper.QueueEntryMapper;
import com.careflow.queue.repository.QueueEntryRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Spy
    private QueueEntryMapper queueEntryMapper = new QueueEntryMapper();

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private QueueServiceImpl queueService;

    private DepartmentResponse sampleDepartment;
    private Department sampleDepartmentEntity;

    @BeforeEach
    void setUp() {
        sampleDepartment = new DepartmentResponse(
                "dept-1", "CARD", "Cardiology", "Desc", "555-0100", "card@careflow.local",
                "Building A", null, DepartmentStatus.ACTIVE, Instant.now(), Instant.now(), 0L
        );
        sampleDepartmentEntity = new Department("dept-1", "CARD", "Cardiology", "Desc", "Building A");
    }

    @Test
    @DisplayName("enqueuePatient should assign sequential token and calculate patients ahead (§21)")
    void enqueuePatient_shouldSucceed() {
        EnqueuePatientRequest request = new EnqueuePatientRequest(
                "pat-1", "dept-1", "doc-1", null, QueuePriority.URGENT, "Chest discomfort"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(departmentRepository.findByIdForUpdate("dept-1")).thenReturn(Optional.of(sampleDepartmentEntity));
        when(staffMemberRepository.existsById("doc-1")).thenReturn(true);
        when(queueEntryRepository.findFirstByPatientIdAndStatusIn(eq("pat-1"), anyList())).thenReturn(Optional.empty());
        when(queueEntryRepository.findMaxTokenNumberByDepartmentIdAndQueueDate(eq("dept-1"), any(LocalDate.class))).thenReturn(4);

        when(queueEntryRepository.saveAndFlush(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(queueEntryRepository.countPatientsAhead(eq("dept-1"), any(LocalDate.class), anyInt(), any(Instant.class))).thenReturn(2L);

        QueueEntryResponse response = queueService.enqueuePatient(request);

        assertThat(response).isNotNull();
        assertThat(response.tokenNumber()).isEqualTo(5);
        assertThat(response.tokenDisplay()).isEqualTo("CARD-005");
        assertThat(response.priority()).isEqualTo(QueuePriority.URGENT);
        assertThat(response.status()).isEqualTo(QueueStatus.WAITING);
        assertThat(response.patientsAhead()).isEqualTo(2L);
    }

    @Test
    @DisplayName("enqueuePatient should reject when patient is already actively queued (§21)")
    void enqueuePatient_shouldRejectDuplicateActiveQueue() {
        EnqueuePatientRequest request = new EnqueuePatientRequest(
                "pat-1", "dept-1", null, null, QueuePriority.NORMAL, null
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(departmentRepository.findByIdForUpdate("dept-1")).thenReturn(Optional.of(sampleDepartmentEntity));

        QueueEntry existing = new QueueEntry("q-1", "dept-1", null, "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), null);
        when(queueEntryRepository.findFirstByPatientIdAndStatusIn(eq("pat-1"), anyList()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> queueService.enqueuePatient(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in active queue");
    }

    @Test
    @DisplayName("callNextPatient should lock and transition top waiting patient to CALLED (§21, §92)")
    void callNextPatient_shouldSucceed() {
        when(departmentService.getDepartmentById("dept-1")).thenReturn(sampleDepartment);

        QueueEntry waitingPatient = new QueueEntry("q-1", "dept-1", null, "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.EMERGENCY, Instant.now(), "Triage emergency");

        when(queueEntryRepository.findNextWaitingCandidateIds(eq("dept-1"), any(LocalDate.class), eq("doc-1"), any(Pageable.class)))
                .thenReturn(List.of("q-1"));
        when(queueEntryRepository.findByIdForUpdate("q-1")).thenReturn(Optional.of(waitingPatient));
        when(queueEntryRepository.saveAndFlush(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueEntryResponse response = queueService.callNextPatient("dept-1", "doc-1");

        assertThat(response.id()).isEqualTo("q-1");
        assertThat(response.status()).isEqualTo(QueueStatus.CALLED);
        assertThat(response.doctorId()).isEqualTo("doc-1");
        assertThat(response.calledTime()).isNotNull();
    }

    @Test
    @DisplayName("callNextPatient should throw QueueEmptyException when no waiting patients exist (§21)")
    void callNextPatient_shouldThrowWhenQueueEmpty() {
        when(departmentService.getDepartmentById("dept-1")).thenReturn(sampleDepartment);
        when(queueEntryRepository.findNextWaitingCandidateIds(eq("dept-1"), any(LocalDate.class), any(), any(Pageable.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> queueService.callNextPatient("dept-1", null))
                .isInstanceOf(QueueEmptyException.class);
    }

    @Test
    @DisplayName("startConsultation and completeConsultation lifecycle transitions should succeed (§21)")
    void consultationLifecycle_shouldSucceed() {
        QueueEntry entry = new QueueEntry("q-1", "dept-1", "doc-1", "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), null);
        entry.setStatus(QueueStatus.CALLED);

        when(queueEntryRepository.findById("q-1")).thenReturn(Optional.of(entry));
        when(queueEntryRepository.saveAndFlush(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // Start consultation: CALLED -> IN_CONSULTATION
        QueueEntryResponse started = queueService.startConsultation("q-1");
        assertThat(started.status()).isEqualTo(QueueStatus.IN_CONSULTATION);
        assertThat(started.consultationStartTime()).isNotNull();

        // Complete consultation: IN_CONSULTATION -> COMPLETED
        QueueEntryResponse completed = queueService.completeConsultation("q-1");
        assertThat(completed.status()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(completed.consultationEndTime()).isNotNull();
    }

    @Test
    @DisplayName("skipPatient and requeuePatient transitions should succeed (§21)")
    void skipAndRequeue_shouldSucceed() {
        QueueEntry entry = new QueueEntry("q-1", "dept-1", "doc-1", "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), null);
        entry.setStatus(QueueStatus.CALLED);

        when(queueEntryRepository.findById("q-1")).thenReturn(Optional.of(entry));
        when(queueEntryRepository.saveAndFlush(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // Skip: CALLED -> SKIPPED
        QueueEntryResponse skipped = queueService.skipPatient("q-1");
        assertThat(skipped.status()).isEqualTo(QueueStatus.SKIPPED);

        // Requeue: SKIPPED -> WAITING
        when(queueEntryRepository.countPatientsAhead(any(), any(), anyInt(), any())).thenReturn(1L);
        QueueEntryResponse requeued = queueService.requeuePatient("q-1");
        assertThat(requeued.status()).isEqualTo(QueueStatus.WAITING);
    }

    @Test
    @DisplayName("cancelQueueEntry should set status to CANCELLED and append cancellation note (§21)")
    void cancelQueueEntry_shouldSucceed() {
        QueueEntry entry = new QueueEntry("q-1", "dept-1", null, "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), "Walk-in");

        when(queueEntryRepository.findById("q-1")).thenReturn(Optional.of(entry));
        when(queueEntryRepository.saveAndFlush(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueEntryResponse cancelled = queueService.cancelQueueEntry("q-1", "Patient had to leave");
        assertThat(cancelled.status()).isEqualTo(QueueStatus.CANCELLED);
        assertThat(cancelled.notes()).contains("Cancelled: Patient had to leave");
    }

    @Test
    @DisplayName("invalid status transition should throw InvalidQueueStatusTransitionException (§21, §69)")
    void invalidTransition_shouldThrow() {
        QueueEntry entry = new QueueEntry("q-1", "dept-1", null, "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), null);
        // Default is WAITING. Attempting to complete directly from WAITING must fail.
        when(queueEntryRepository.findById("q-1")).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> queueService.completeConsultation("q-1"))
                .isInstanceOf(InvalidQueueStatusTransitionException.class);
    }

    @Test
    @DisplayName("getDepartmentLiveStatus should return aggregate counts and live ticket lists (§21)")
    void getDepartmentLiveStatus_shouldReturnDashboard() {
        when(departmentService.getDepartmentById("dept-1")).thenReturn(sampleDepartment);
        when(queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(eq("dept-1"), any(), eq(QueueStatus.WAITING))).thenReturn(5L);
        when(queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(eq("dept-1"), any(), eq(QueueStatus.CALLED))).thenReturn(1L);
        when(queueEntryRepository.countByDepartmentIdAndQueueDateAndStatus(eq("dept-1"), any(), eq(QueueStatus.IN_CONSULTATION))).thenReturn(2L);

        QueueEntry called = new QueueEntry("q-called", "dept-1", "doc-1", "pat-1", null,
                LocalDate.now(), 1, "CARD-001", QueuePriority.URGENT, Instant.now(), null);
        called.setStatus(QueueStatus.CALLED);

        QueueEntry waiting = new QueueEntry("q-waiting", "dept-1", null, "pat-2", null,
                LocalDate.now(), 2, "CARD-002", QueuePriority.NORMAL, Instant.now(), null);

        when(queueEntryRepository.findByDepartmentIdAndQueueDateAndStatusInOrderByPriorityAscEntryTimeAsc(
                eq("dept-1"), any(), eq(List.of(QueueStatus.CALLED, QueueStatus.IN_CONSULTATION))))
                .thenReturn(List.of(called));

        when(queueEntryRepository.findByDepartmentIdAndQueueDateAndStatusInOrderByPriorityAscEntryTimeAsc(
                eq("dept-1"), any(), eq(List.of(QueueStatus.WAITING))))
                .thenReturn(List.of(waiting));

        DepartmentQueueLiveStatusResponse response = queueService.getDepartmentLiveStatus("dept-1");

        assertThat(response).isNotNull();
        assertThat(response.totalWaiting()).isEqualTo(5L);
        assertThat(response.totalCalled()).isEqualTo(1L);
        assertThat(response.totalInConsultation()).isEqualTo(2L);
        assertThat(response.currentlyCalled()).hasSize(1);
        assertThat(response.waitingList()).hasSize(1);
    }
}
