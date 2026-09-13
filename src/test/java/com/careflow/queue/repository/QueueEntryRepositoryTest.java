package com.careflow.queue.repository;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.domain.QueuePriority;
import com.careflow.queue.domain.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class QueueEntryRepositoryTest {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Department testDept;
    private Patient patient1;
    private Patient patient2;
    private Patient patient3;

    @BeforeEach
    void setUp() {
        testDept = new Department(UUID.randomUUID().toString(), "CARD-Q", "Cardiology Queue", "Desc", "Bldg A");
        departmentRepository.save(testDept);

        patient1 = new Patient(UUID.randomUUID().toString(), "CF-Q-01", "Bruce", "Wayne",
                LocalDate.of(1980, 2, 19), Gender.MALE, "+1-555-0701");
        patientRepository.save(patient1);

        patient2 = new Patient(UUID.randomUUID().toString(), "CF-Q-02", "Clark", "Kent",
                LocalDate.of(1982, 6, 18), Gender.MALE, "+1-555-0702");
        patientRepository.save(patient2);

        patient3 = new Patient(UUID.randomUUID().toString(), "CF-Q-03", "Diana", "Prince",
                LocalDate.of(1985, 3, 22), Gender.FEMALE, "+1-555-0703");
        patientRepository.save(patient3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("save should persist queue entry with audit metadata (§11, §21)")
    void save_shouldPersistQueueEntry() {
        QueueEntry entry = new QueueEntry(
                UUID.randomUUID().toString(),
                testDept.getId(),
                null,
                patient1.getId(),
                null,
                LocalDate.now(),
                1,
                "CARD-001",
                QueuePriority.NORMAL,
                Instant.now(),
                "Routine check"
        );

        QueueEntry saved = queueEntryRepository.save(entry);
        entityManager.flush();
        entityManager.clear();

        QueueEntry found = queueEntryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTokenDisplay()).isEqualTo("CARD-001");
        assertThat(found.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("uk_queue_dept_date_token constraint should prevent duplicate tokens on the same date (§21, §92)")
    void duplicateToken_shouldThrowConstraintViolation() {
        LocalDate date = LocalDate.now();

        QueueEntry entry1 = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient1.getId(), null,
                date, 1, "CARD-001", QueuePriority.NORMAL, Instant.now(), null
        );
        queueEntryRepository.save(entry1);
        entityManager.flush();

        QueueEntry entry2 = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient2.getId(), null,
                date, 1, "CARD-001", QueuePriority.URGENT, Instant.now(), null
        );

        assertThatThrownBy(() -> queueEntryRepository.saveAndFlush(entry2))
                .satisfies(ex -> assertThat(
                        ex instanceof DataIntegrityViolationException ||
                        ex instanceof org.hibernate.exception.ConstraintViolationException ||
                        ex instanceof jakarta.persistence.PersistenceException
                ).isTrue());
    }

    @Test
    @DisplayName("findNextWaitingCandidateIds should prioritize EMERGENCY over URGENT over NORMAL (§21)")
    void candidateIds_shouldOrderByPriorityAndEntryTime() {
        LocalDate date = LocalDate.now();
        Instant baseTime = Instant.now();

        // 1. Normal priority registered early
        QueueEntry normalEarly = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient1.getId(), null,
                date, 1, "CARD-001", QueuePriority.NORMAL, baseTime.minusSeconds(120), null
        );
        queueEntryRepository.save(normalEarly);

        // 2. Urgent priority registered later
        QueueEntry urgentLater = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient2.getId(), null,
                date, 2, "CARD-002", QueuePriority.URGENT, baseTime.minusSeconds(60), null
        );
        queueEntryRepository.save(urgentLater);

        // 3. Emergency priority registered last
        QueueEntry emergencyLast = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient3.getId(), null,
                date, 3, "CARD-003", QueuePriority.EMERGENCY, baseTime, null
        );
        queueEntryRepository.save(emergencyLast);

        entityManager.flush();
        entityManager.clear();

        List<String> orderedIds = queueEntryRepository.findNextWaitingCandidateIds(
                testDept.getId(), date, null, PageRequest.of(0, 10));

        assertThat(orderedIds).hasSize(3);
        // Order must be EMERGENCY, then URGENT, then NORMAL
        assertThat(orderedIds.get(0)).isEqualTo(emergencyLast.getId());
        assertThat(orderedIds.get(1)).isEqualTo(urgentLater.getId());
        assertThat(orderedIds.get(2)).isEqualTo(normalEarly.getId());
    }

    @Test
    @DisplayName("countPatientsAhead should correctly count higher priority or earlier patients (§21)")
    void countPatientsAhead_shouldCalculateAccurately() {
        LocalDate date = LocalDate.now();
        Instant baseTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        QueueEntry emergency = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient1.getId(), null,
                date, 1, "CARD-001", QueuePriority.EMERGENCY, baseTime, null
        );
        queueEntryRepository.save(emergency);

        QueueEntry urgent = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient2.getId(), null,
                date, 2, "CARD-002", QueuePriority.URGENT, baseTime.plusSeconds(30), null
        );
        queueEntryRepository.save(urgent);

        QueueEntry normal = new QueueEntry(
                UUID.randomUUID().toString(), testDept.getId(), null, patient3.getId(), null,
                date, 3, "CARD-003", QueuePriority.NORMAL, baseTime.plusSeconds(60), null
        );
        queueEntryRepository.save(normal);

        entityManager.flush();
        entityManager.clear();

        // For emergency patient, 0 patients ahead
        long aheadEmergency = queueEntryRepository.countPatientsAhead(
                testDept.getId(), date, QueuePriority.EMERGENCY.getRank(), emergency.getEntryTime());
        assertThat(aheadEmergency).isEqualTo(0L);

        // For urgent patient, 1 emergency patient is ahead
        long aheadUrgent = queueEntryRepository.countPatientsAhead(
                testDept.getId(), date, QueuePriority.URGENT.getRank(), urgent.getEntryTime());
        assertThat(aheadUrgent).isEqualTo(1L);

        // For normal patient, both emergency and urgent are ahead
        long aheadNormal = queueEntryRepository.countPatientsAhead(
                testDept.getId(), date, QueuePriority.NORMAL.getRank(), normal.getEntryTime());
        assertThat(aheadNormal).isEqualTo(2L);
    }
}
