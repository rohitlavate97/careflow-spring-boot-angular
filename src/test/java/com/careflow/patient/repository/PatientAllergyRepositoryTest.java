package com.careflow.patient.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.patient.domain.AllergenCategory;
import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientAllergy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class PatientAllergyRepositoryTest {

    @Autowired
    private PatientAllergyRepository allergyRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-2026-99001",
                "Louis",
                "Pasteur",
                LocalDate.of(1822, 12, 27),
                Gender.MALE,
                "+1-555-0987"
        );
        patientRepository.save(patient);
        entityManager.flush();
    }

    @Test
    @DisplayName("save should persist allergy record with audit timestamps and enum mappings (§11, §16)")
    void save_shouldPersistAllergy() {
        PatientAllergy allergy = new PatientAllergy(
                UUID.randomUUID().toString(),
                patient.getId(),
                "Amoxicillin",
                AllergenCategory.DRUG,
                AllergySeverity.SEVERE,
                "Facial edema and urticaria",
                AllergyStatus.ACTIVE,
                "Observed during childhood treatment",
                LocalDate.of(2015, 3, 20)
        );

        PatientAllergy saved = allergyRepository.save(allergy);
        entityManager.flush();
        entityManager.clear();

        Optional<PatientAllergy> found = allergyRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAllergen()).isEqualTo("Amoxicillin");
        assertThat(found.get().getCategory()).isEqualTo(AllergenCategory.DRUG);
        assertThat(found.get().getSeverity()).isEqualTo(AllergySeverity.SEVERE);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().isHighRisk()).isTrue();
    }

    @Test
    @DisplayName("existsByPatientIdAndAllergenIgnoreCaseAndStatus should detect case-insensitive active allergen match (§16, §92)")
    void existsByPatientIdAndAllergenIgnoreCaseAndStatus_shouldDetectMatch() {
        PatientAllergy allergy = new PatientAllergy(
                UUID.randomUUID().toString(),
                patient.getId(),
                "Peanuts",
                AllergenCategory.FOOD,
                AllergySeverity.LIFE_THREATENING,
                "Anaphylactic shock",
                AllergyStatus.ACTIVE,
                null,
                null
        );
        allergyRepository.save(allergy);
        entityManager.flush();
        entityManager.clear();

        boolean existsMatch = allergyRepository.existsByPatientIdAndAllergenIgnoreCaseAndStatus(
                patient.getId(), "peanuts", AllergyStatus.ACTIVE
        );
        boolean existsInactive = allergyRepository.existsByPatientIdAndAllergenIgnoreCaseAndStatus(
                patient.getId(), "peanuts", AllergyStatus.INACTIVE
        );

        assertThat(existsMatch).isTrue();
        assertThat(existsInactive).isFalse();
    }

    @Test
    @DisplayName("countHighRiskAllergies should accurately return count of SEVERE and LIFE_THREATENING active allergies (§16)")
    void countHighRiskAllergies_shouldReturnAccurateCount() {
        PatientAllergy highRisk = new PatientAllergy(
                UUID.randomUUID().toString(),
                patient.getId(),
                "Penicillin",
                AllergenCategory.DRUG,
                AllergySeverity.LIFE_THREATENING,
                "Airway obstruction",
                AllergyStatus.ACTIVE,
                null,
                null
        );
        PatientAllergy mild = new PatientAllergy(
                UUID.randomUUID().toString(),
                patient.getId(),
                "Dust Mites",
                AllergenCategory.ENVIRONMENTAL,
                AllergySeverity.MILD,
                "Sneezing",
                AllergyStatus.ACTIVE,
                null,
                null
        );
        allergyRepository.saveAll(List.of(highRisk, mild));
        entityManager.flush();
        entityManager.clear();

        long count = allergyRepository.countHighRiskAllergies(
                patient.getId(),
                AllergyStatus.ACTIVE,
                List.of(AllergySeverity.SEVERE, AllergySeverity.LIFE_THREATENING)
        );

        assertThat(count).isEqualTo(1L);
    }
}
