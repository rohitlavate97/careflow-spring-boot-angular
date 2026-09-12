package com.careflow.patient.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.patient.domain.AllergenCategory;
import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientAllergy;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.PatientAllergyResponse;
import com.careflow.patient.dto.UpdateAllergyStatusRequest;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;
import com.careflow.patient.exception.AllergyNotFoundException;
import com.careflow.patient.exception.DuplicateAllergyException;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.mapper.PatientAllergyMapper;
import com.careflow.patient.repository.PatientAllergyRepository;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAllergyServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientAllergyRepository patientAllergyRepository;

    private PatientAllergyMapper allergyMapper;
    private PatientAllergyServiceImpl allergyService;

    private Patient activePatient;
    private Patient deceasedPatient;

    @BeforeEach
    void setUp() {
        allergyMapper = new PatientAllergyMapper();
        allergyService = new PatientAllergyServiceImpl(patientRepository, patientAllergyRepository, allergyMapper);

        activePatient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-202609-00001",
                "Alexander",
                "Fleming",
                LocalDate.of(1881, 8, 6),
                Gender.MALE,
                "+1-555-0199"
        );

        deceasedPatient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-202609-00002",
                "Arthur",
                "Conan",
                LocalDate.of(1859, 5, 22),
                Gender.MALE,
                "+1-555-0198"
        );
        deceasedPatient.markDeceased();
    }

    @Test
    @DisplayName("recordAllergy should save and return allergy response when input is valid (§16)")
    void recordAllergy_shouldSucceed_whenValid() {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Penicillin",
                AllergenCategory.DRUG,
                AllergySeverity.LIFE_THREATENING,
                "Anaphylaxis, airway swelling",
                "Severe reaction reported in childhood",
                LocalDate.of(2010, 5, 15)
        );

        when(patientRepository.findById(activePatient.getId())).thenReturn(Optional.of(activePatient));
        when(patientAllergyRepository.existsByPatientIdAndAllergenIgnoreCaseAndStatus(activePatient.getId(), "Penicillin", AllergyStatus.ACTIVE))
                .thenReturn(false);
        when(patientAllergyRepository.save(any(PatientAllergy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAllergyResponse response = allergyService.recordAllergy(activePatient.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.allergen()).isEqualTo("Penicillin");
        assertThat(response.category()).isEqualTo(AllergenCategory.DRUG);
        assertThat(response.severity()).isEqualTo(AllergySeverity.LIFE_THREATENING);
        assertThat(response.status()).isEqualTo(AllergyStatus.ACTIVE);
        assertThat(response.isHighRisk()).isTrue();
        verify(patientAllergyRepository).save(any(PatientAllergy.class));
    }

    @Test
    @DisplayName("recordAllergy should throw DuplicateAllergyException when patient already has active allergy for same allergen (§16, §92)")
    void recordAllergy_shouldThrowDuplicateAllergyException_whenAllergenAlreadyActive() {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Penicillin",
                AllergenCategory.DRUG,
                AllergySeverity.MODERATE,
                "Hives",
                null,
                null
        );

        when(patientRepository.findById(activePatient.getId())).thenReturn(Optional.of(activePatient));
        when(patientAllergyRepository.existsByPatientIdAndAllergenIgnoreCaseAndStatus(activePatient.getId(), "Penicillin", AllergyStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> allergyService.recordAllergy(activePatient.getId(), request))
                .isInstanceOf(DuplicateAllergyException.class)
                .hasMessageContaining("Penicillin");
    }

    @Test
    @DisplayName("recordAllergy should throw BusinessRuleException when patient is DECEASED (§16, §88)")
    void recordAllergy_shouldThrowBusinessRuleException_whenPatientIsDeceased() {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Sulfa",
                AllergenCategory.DRUG,
                AllergySeverity.MILD,
                "Rash",
                null,
                null
        );

        when(patientRepository.findById(deceasedPatient.getId())).thenReturn(Optional.of(deceasedPatient));

        assertThatThrownBy(() -> allergyService.recordAllergy(deceasedPatient.getId(), request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deceased patient");
    }

    @Test
    @DisplayName("recordAllergy should throw PatientNotFoundException when patient does not exist (§16)")
    void recordAllergy_shouldThrowPatientNotFoundException_whenPatientNotFound() {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Aspirin",
                AllergenCategory.DRUG,
                AllergySeverity.MODERATE,
                null,
                null,
                null
        );

        when(patientRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allergyService.recordAllergy("unknown-id", request))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    @DisplayName("getPatientAllergies should return all allergies when status filter is null (§16)")
    void getPatientAllergies_shouldReturnAllAllergies_whenStatusFilterIsNull() {
        PatientAllergy allergy1 = new PatientAllergy("a-1", activePatient.getId(), "Penicillin",
                AllergenCategory.DRUG, AllergySeverity.SEVERE, "Hives", AllergyStatus.ACTIVE, null, null);
        PatientAllergy allergy2 = new PatientAllergy("a-2", activePatient.getId(), "Peanuts",
                AllergenCategory.FOOD, AllergySeverity.LIFE_THREATENING, "Anaphylaxis", AllergyStatus.ACTIVE, null, null);

        when(patientRepository.existsById(activePatient.getId())).thenReturn(true);
        when(patientAllergyRepository.findByPatientIdOrderByCreatedAtDesc(activePatient.getId()))
                .thenReturn(List.of(allergy1, allergy2));

        List<PatientAllergyResponse> result = allergyService.getPatientAllergies(activePatient.getId(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).allergen()).isEqualTo("Penicillin");
        assertThat(result.get(1).allergen()).isEqualTo("Peanuts");
    }

    @Test
    @DisplayName("updateAllergy should modify severity, reaction, and notes (§16)")
    void updateAllergy_shouldModifyDetails() {
        PatientAllergy existing = new PatientAllergy("a-1", activePatient.getId(), "Latex",
                AllergenCategory.OTHER, AllergySeverity.MILD, "Rash", AllergyStatus.ACTIVE, "Initial note", null);

        UpdatePatientAllergyRequest updateReq = new UpdatePatientAllergyRequest(
                AllergySeverity.SEVERE,
                "Severe swelling and contact dermatitis",
                "Escalated severity after occupational exposure",
                LocalDate.of(2023, 1, 10)
        );

        when(patientRepository.findById(activePatient.getId())).thenReturn(Optional.of(activePatient));
        when(patientAllergyRepository.findByIdAndPatientId("a-1", activePatient.getId())).thenReturn(Optional.of(existing));
        when(patientAllergyRepository.save(any(PatientAllergy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAllergyResponse response = allergyService.updateAllergy(activePatient.getId(), "a-1", updateReq);

        assertThat(response.severity()).isEqualTo(AllergySeverity.SEVERE);
        assertThat(response.reaction()).isEqualTo("Severe swelling and contact dermatitis");
        assertThat(response.notes()).isEqualTo("Escalated severity after occupational exposure");
        assertThat(response.isHighRisk()).isTrue();
    }

    @Test
    @DisplayName("updateAllergyStatus should transition status and update notes (§16, §69)")
    void updateAllergyStatus_shouldTransitionStatus() {
        PatientAllergy existing = new PatientAllergy("a-1", activePatient.getId(), "Pollen",
                AllergenCategory.ENVIRONMENTAL, AllergySeverity.MILD, "Sneezing", AllergyStatus.ACTIVE, null, null);

        UpdateAllergyStatusRequest statusReq = new UpdateAllergyStatusRequest(
                AllergyStatus.RESOLVED,
                "Desensitization therapy completed successfully"
        );

        when(patientRepository.findById(activePatient.getId())).thenReturn(Optional.of(activePatient));
        when(patientAllergyRepository.findByIdAndPatientId("a-1", activePatient.getId())).thenReturn(Optional.of(existing));
        when(patientAllergyRepository.save(any(PatientAllergy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAllergyResponse response = allergyService.updateAllergyStatus(activePatient.getId(), "a-1", statusReq);

        assertThat(response.status()).isEqualTo(AllergyStatus.RESOLVED);
        assertThat(response.notes()).isEqualTo("Desensitization therapy completed successfully");
    }

    @Test
    @DisplayName("removeAllergy should soft-deactivate allergy to preserve audit trail (§16, §74)")
    void removeAllergy_shouldSoftDeactivate() {
        PatientAllergy existing = new PatientAllergy("a-1", activePatient.getId(), "Dust",
                AllergenCategory.ENVIRONMENTAL, AllergySeverity.MILD, "Cough", AllergyStatus.ACTIVE, null, null);

        when(patientRepository.findById(activePatient.getId())).thenReturn(Optional.of(activePatient));
        when(patientAllergyRepository.findByIdAndPatientId("a-1", activePatient.getId())).thenReturn(Optional.of(existing));

        allergyService.removeAllergy(activePatient.getId(), "a-1");

        assertThat(existing.getStatus()).isEqualTo(AllergyStatus.INACTIVE);
        verify(patientAllergyRepository).save(existing);
    }

    @Test
    @DisplayName("hasHighRiskAllergies should return true when severe or life-threatening active allergy exists (§16)")
    void hasHighRiskAllergies_shouldReturnTrue_whenSevereOrLifeThreateningPresent() {
        when(patientAllergyRepository.countHighRiskAllergies(eq(activePatient.getId()), eq(AllergyStatus.ACTIVE), any()))
                .thenReturn(1L);

        boolean hasHighRisk = allergyService.hasHighRiskAllergies(activePatient.getId());

        assertThat(hasHighRisk).isTrue();
    }
}
