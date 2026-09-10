package com.careflow.patient.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.patient.domain.Address;
import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.EmergencyContact;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class PatientRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("save should persist patient with embedded value objects and audit metadata (§11, §16)")
    void save_shouldPersistPatientWithEmbeddedAddressAndEmergencyContact() {
        Patient patient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-2026-00001",
                "Eleanor",
                "Rigby",
                LocalDate.of(1985, 4, 12),
                Gender.FEMALE,
                "+1-555-0144"
        );
        patient.setEmail("eleanor.rigby@example.com");
        patient.setBloodGroup(BloodGroup.O_POSITIVE);
        patient.setAddress(new Address("100 Penny Lane", "Apt 4B", "Liverpool", "Merseyside", "L18 1DG", "UK"));
        patient.setEmergencyContact(new EmergencyContact("John Lennon", "Friend", "+1-555-0199"));

        Patient saved = patientRepository.save(patient);
        entityManager.flush();
        entityManager.clear();

        Optional<Patient> found = patientRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getMrn()).isEqualTo("PAT-2026-00001");
        assertThat(found.get().getFullName()).isEqualTo("Eleanor Rigby");
        assertThat(found.get().getAddress()).isNotNull();
        assertThat(found.get().getAddress().getCity()).isEqualTo("Liverpool");
        assertThat(found.get().getEmergencyContact()).isNotNull();
        assertThat(found.get().getEmergencyContact().getName()).isEqualTo("John Lennon");
        assertThat(found.get().getStatus()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(found.get().getVersion()).isEqualTo(0L);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByMrn should return patient matching unique medical record number (§16)")
    void findByMrn_shouldReturnPatient() {
        Patient patient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-2026-00002",
                "James",
                "Watson",
                LocalDate.of(1960, 8, 20),
                Gender.MALE,
                "+1-555-0145"
        );
        patientRepository.save(patient);
        entityManager.flush();

        Optional<Patient> found = patientRepository.findByMrn("PAT-2026-00002");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("James");
        assertThat(found.get().getLastName()).isEqualTo("Watson");
    }

    @Test
    @DisplayName("findByPhone should return matching patients using indexed query (§96)")
    void findByPhone_shouldReturnMatchingPatients() {
        Patient p1 = new Patient(UUID.randomUUID().toString(), "PAT-2026-00003", "Alice", "Cooper", LocalDate.of(1975, 2, 1), Gender.FEMALE, "+1-555-0777");
        Patient p2 = new Patient(UUID.randomUUID().toString(), "PAT-2026-00004", "Bob", "Cooper", LocalDate.of(2005, 5, 15), Gender.MALE, "+1-555-0777"); // Family shared phone
        patientRepository.save(p1);
        patientRepository.save(p2);
        entityManager.flush();

        List<Patient> list = patientRepository.findByPhone("+1-555-0777");
        assertThat(list).hasSize(2);
    }

    @Test
    @DisplayName("save should fail when duplicate MRN is persisted (§10, §92)")
    void save_shouldFail_whenDuplicateMrnPersisted() {
        Patient p1 = new Patient(UUID.randomUUID().toString(), "PAT-DUPLICATE", "User1", "Test", LocalDate.of(1990, 1, 1), Gender.MALE, "+1-555-0101");
        patientRepository.save(p1);
        entityManager.flush();

        Patient p2 = new Patient(UUID.randomUUID().toString(), "PAT-DUPLICATE", "User2", "Test", LocalDate.of(1992, 2, 2), Gender.FEMALE, "+1-555-0102");

        assertThatThrownBy(() -> {
            patientRepository.save(p2);
            entityManager.flush();
        }).satisfies(throwable -> {
            assertThat(throwable).isInstanceOfAny(
                    DataIntegrityViolationException.class,
                    org.hibernate.exception.ConstraintViolationException.class,
                    jakarta.persistence.PersistenceException.class
            );
        });
    }

    @Test
    @DisplayName("optimisticLocking should increment version on patient update (§11)")
    void optimisticLocking_shouldIncrementVersionOnPatientUpdate() {
        Patient patient = new Patient(UUID.randomUUID().toString(), "PAT-2026-00005", "Grace", "Hopper", LocalDate.of(1906, 12, 9), Gender.FEMALE, "+1-555-0103");
        Patient saved = patientRepository.save(patient);
        entityManager.flush();
        assertThat(saved.getVersion()).isEqualTo(0L);

        saved.setPhone("+1-555-9999");
        Patient updated = patientRepository.save(saved);
        entityManager.flush();

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchPatients should support pagination and search filtering (§71, §72)")
    void searchPatients_shouldSupportPaginationAndSearch() {
        for (int i = 1; i <= 5; i++) {
            patientRepository.save(new Patient(
                    UUID.randomUUID().toString(),
                    "PAT-PAGE-" + i,
                    "SearchName" + i,
                    "TargetFamily",
                    LocalDate.of(1980 + i, 1, 1),
                    Gender.OTHER,
                    "+1-555-000" + i
            ));
        }
        entityManager.flush();

        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("lastName").ascending());
        Page<Patient> result = patientRepository.searchPatients("TargetFamily", pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
