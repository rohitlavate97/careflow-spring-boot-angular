package com.careflow.patient.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.patient.domain.Address;
import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.EmergencyContact;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.AddressDto;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.EmergencyContactDto;
import com.careflow.patient.dto.PatientResponse;
import com.careflow.patient.dto.PatientSummaryResponse;
import com.careflow.patient.dto.UpdatePatientRequest;
import com.careflow.patient.dto.UpdatePatientStatusRequest;
import com.careflow.patient.exception.InvalidPatientStatusTransitionException;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.mapper.PatientMapper;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    private MrnGenerator mrnGenerator;
    private PatientMapper patientMapper;
    private PatientServiceImpl patientService;

    @BeforeEach
    void setUp() {
        mrnGenerator = () -> "PAT-202609-00001";
        patientMapper = new PatientMapper();
        patientService = new PatientServiceImpl(patientRepository, patientMapper, mrnGenerator);
    }

    @Test
    @DisplayName("registerPatient should generate unique MRN and persist patient (§16)")
    void registerPatient_shouldGenerateMrnAndSave() {
        CreatePatientRequest request = new CreatePatientRequest(
                "John",
                "F.",
                "Kennedy",
                LocalDate.of(1960, 5, 29),
                Gender.MALE,
                BloodGroup.O_POSITIVE,
                "jfk@example.com",
                "+1-555-1960",
                new AddressDto("1600 Pennsylvania Ave", null, "Washington", "DC", "20500", "USA"),
                new EmergencyContactDto("Jackie Kennedy", "Spouse", "+1-555-1961")
        );

        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.registerPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.mrn()).isEqualTo("PAT-202609-00001");
        assertThat(response.fullName()).isEqualTo("John F. Kennedy");
        assertThat(response.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(response.address()).isNotNull();
        assertThat(response.address().city()).isEqualTo("Washington");
        assertThat(response.emergencyContact()).isNotNull();
        assertThat(response.emergencyContact().name()).isEqualTo("Jackie Kennedy");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("getPatientById should return patient response when patient exists")
    void getPatientById_shouldReturnResponse() {
        String patientId = UUID.randomUUID().toString();
        Patient patient = new Patient(patientId, "PAT-202609-00002", "Ada", "Lovelace", LocalDate.of(1815, 12, 10), Gender.FEMALE, "+44-20-7946-0912");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        PatientResponse response = patientService.getPatientById(patientId);

        assertThat(response.id()).isEqualTo(patientId);
        assertThat(response.fullName()).isEqualTo("Ada Lovelace");
        assertThat(response.mrn()).isEqualTo("PAT-202609-00002");
    }

    @Test
    @DisplayName("getPatientById should throw PatientNotFoundException when id not found")
    void getPatientById_shouldThrowWhenNotFound() {
        String patientId = "non-existent-id";
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(patientId))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining("Patient with identifier 'non-existent-id' was not found");
    }

    @Test
    @DisplayName("getPatientByMrn should return patient response when MRN exists")
    void getPatientByMrn_shouldReturnResponse() {
        Patient patient = new Patient(UUID.randomUUID().toString(), "PAT-202609-00003", "Alan", "Turing", LocalDate.of(1912, 6, 23), Gender.MALE, "+44-1625-123456");

        when(patientRepository.findByMrn("PAT-202609-00003")).thenReturn(Optional.of(patient));

        PatientResponse response = patientService.getPatientByMrn("PAT-202609-00003");

        assertThat(response.fullName()).isEqualTo("Alan Turing");
        assertThat(response.mrn()).isEqualTo("PAT-202609-00003");
    }

    @Test
    @DisplayName("updatePatient should modify demographic details when patient is active")
    void updatePatient_shouldUpdateDetails() {
        String patientId = UUID.randomUUID().toString();
        Patient existing = new Patient(patientId, "PAT-202609-00004", "Marie", "Curie", LocalDate.of(1867, 11, 7), Gender.FEMALE, "+33-1-4000-0000");

        UpdatePatientRequest request = new UpdatePatientRequest(
                "Marie",
                "Sklodowska",
                "Curie",
                LocalDate.of(1867, 11, 7),
                Gender.FEMALE,
                BloodGroup.A_POSITIVE,
                "marie.curie@radium.org",
                "+33-1-4000-9999",
                new AddressDto("11 Rue Pierre et Marie Curie", null, "Paris", "Ile-de-France", "75005", "France"),
                new EmergencyContactDto("Pierre Curie", "Husband", "+33-1-4000-8888")
        );

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.updatePatient(patientId, request);

        assertThat(response.fullName()).isEqualTo("Marie Sklodowska Curie");
        assertThat(response.phone()).isEqualTo("+33-1-4000-9999");
        assertThat(response.bloodGroup()).isEqualTo(BloodGroup.A_POSITIVE);
        assertThat(response.address().city()).isEqualTo("Paris");
    }

    @Test
    @DisplayName("updatePatient should reject updates when patient is DECEASED (§69)")
    void updatePatient_shouldRejectUpdatesForDeceasedPatient() {
        String patientId = UUID.randomUUID().toString();
        Patient deceased = new Patient(patientId, "PAT-202609-00005", "Old", "Patient", LocalDate.of(1920, 1, 1), Gender.OTHER, "+1-555-0000");
        deceased.markDeceased();

        UpdatePatientRequest request = new UpdatePatientRequest(
                "New", null, "Name", LocalDate.of(1920, 1, 1), Gender.OTHER, null, null, "+1-555-0000", null, null
        );

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(deceased));

        assertThatThrownBy(() -> patientService.updatePatient(patientId, request))
                .isInstanceOf(InvalidPatientStatusTransitionException.class)
                .hasMessageContaining("Demographic updates are not permitted for deceased patients");
    }

    @Test
    @DisplayName("updatePatientStatus should correctly transition between ACTIVE, INACTIVE, and DECEASED (§16, §69)")
    void updatePatientStatus_shouldTransitionStatuses() {
        String patientId = UUID.randomUUID().toString();
        Patient patient = new Patient(patientId, "PAT-202609-00006", "Test", "Subject", LocalDate.of(1990, 1, 1), Gender.MALE, "+1-555-1111");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACTIVE -> INACTIVE
        PatientResponse resp1 = patientService.updatePatientStatus(patientId, new UpdatePatientStatusRequest(PatientStatus.INACTIVE, "Relocated"));
        assertThat(resp1.status()).isEqualTo(PatientStatus.INACTIVE);

        // INACTIVE -> ACTIVE
        PatientResponse resp2 = patientService.updatePatientStatus(patientId, new UpdatePatientStatusRequest(PatientStatus.ACTIVE, "Returned"));
        assertThat(resp2.status()).isEqualTo(PatientStatus.ACTIVE);

        // ACTIVE -> DECEASED
        PatientResponse resp3 = patientService.updatePatientStatus(patientId, new UpdatePatientStatusRequest(PatientStatus.DECEASED, "Death certificate filed"));
        assertThat(resp3.status()).isEqualTo(PatientStatus.DECEASED);

        // DECEASED -> ACTIVE (Should fail!)
        assertThatThrownBy(() -> patientService.updatePatientStatus(patientId, new UpdatePatientStatusRequest(PatientStatus.ACTIVE, "Revival attempt")))
                .isInstanceOf(InvalidPatientStatusTransitionException.class)
                .hasMessageContaining("Cannot transition patient status from 'DECEASED' to 'ACTIVE'");
    }

    @Test
    @DisplayName("searchPatients should query repository with dynamic specification and return PageResponse (§71, §72)")
    @SuppressWarnings("unchecked")
    void searchPatients_shouldQueryWithSpecification() {
        Patient p = new Patient(UUID.randomUUID().toString(), "PAT-202609-00007", "Grace", "Hopper", LocalDate.of(1906, 12, 9), Gender.FEMALE, "+1-555-2222");
        Page<Patient> mockPage = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);

        when(patientRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<PatientResponse> response = patientService.searchPatients(
                "Grace", Gender.FEMALE, PatientStatus.ACTIVE, null, PageRequest.of(0, 10)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().fullName()).isEqualTo("Grace Hopper");
    }

    @Test
    @DisplayName("quickSearch should return lightweight PatientSummaryResponse items (§111)")
    void quickSearch_shouldReturnSummaries() {
        Patient p = new Patient(UUID.randomUUID().toString(), "PAT-202609-00008", "Katherine", "Johnson", LocalDate.of(1918, 8, 26), Gender.FEMALE, "+1-555-3333");
        Page<Patient> mockPage = new PageImpl<>(List.of(p));

        when(patientRepository.searchPatients(eq("Katherine"), any(PageRequest.class))).thenReturn(mockPage);

        List<PatientSummaryResponse> summaries = patientService.quickSearch("Katherine", 5);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().fullName()).isEqualTo("Katherine Johnson");
        assertThat(summaries.getFirst().mrn()).isEqualTo("PAT-202609-00008");
    }
}
