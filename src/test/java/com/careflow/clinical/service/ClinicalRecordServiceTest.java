package com.careflow.clinical.service;

import com.careflow.clinical.domain.ClinicalNote;
import com.careflow.clinical.domain.NoteType;
import com.careflow.clinical.dto.ClinicalNoteResponse;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import com.careflow.clinical.mapper.ClinicalNoteMapper;
import com.careflow.clinical.repository.ClinicalNoteRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {

    @Mock
    private ClinicalNoteRepository clinicalNoteRepository;

    @Spy
    private ClinicalNoteMapper clinicalNoteMapper = new ClinicalNoteMapper();

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @InjectMocks
    private ClinicalRecordServiceImpl clinicalRecordService;

    private StaffMember doctor;
    private StaffMember receptionist;

    @BeforeEach
    void setUp() {
        doctor = new StaffMember("doc-1", "DOC-001", "dept-1", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());
        receptionist = new StaffMember("rec-1", "REC-001", "dept-1", "Rory", "Williams", "rory@careflow.local", "+1234567892", StaffType.RECEPTIONIST, LocalDate.now());
    }

    @Test
    @DisplayName("Should successfully create clinical note when authored by a DOCTOR")
    void createClinicalNote_success() {
        CreateClinicalNoteRequest request = new CreateClinicalNoteRequest(
                "pat-1", "c-1", "doc-1", NoteType.SOAP_ASSESSMENT, "Assessment Note", "Patient responding well to treatment"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(consultationRepository.existsById("c-1")).thenReturn(true);
        when(clinicalNoteRepository.save(any(ClinicalNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalNoteResponse response = clinicalRecordService.createClinicalNote(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.authorId()).isEqualTo("doc-1");
        assertThat(response.noteType()).isEqualTo(NoteType.SOAP_ASSESSMENT);
        assertThat(response.title()).isEqualTo("Assessment Note");
        assertThat(response.content()).isEqualTo("Patient responding well to treatment");

        verify(clinicalNoteRepository).save(any(ClinicalNote.class));
    }

    @Test
    @DisplayName("Should reject clinical note creation when author is non-clinical staff (e.g. RECEPTIONIST)")
    void createClinicalNote_rejectsNonClinicalAuthor() {
        CreateClinicalNoteRequest request = new CreateClinicalNoteRequest(
                "pat-1", null, "rec-1", NoteType.GENERAL, "General Note", "Non-clinical observation"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("rec-1")).thenReturn(Optional.of(receptionist));

        assertThatThrownBy(() -> clinicalRecordService.createClinicalNote(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not authorized to author clinical notes");
    }

    @Test
    @DisplayName("Should fail when patient does not exist")
    void createClinicalNote_patientNotFound() {
        CreateClinicalNoteRequest request = new CreateClinicalNoteRequest(
                "missing-pat", null, "doc-1", NoteType.GENERAL, "Note", "Content"
        );

        when(patientRepository.existsById("missing-pat")).thenReturn(false);

        assertThatThrownBy(() -> clinicalRecordService.createClinicalNote(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-pat");
    }

    @Test
    @DisplayName("Should retrieve paginated clinical notes for a patient")
    void getNotesByPatient_success() {
        when(patientRepository.existsById("pat-1")).thenReturn(true);

        ClinicalNote note = new ClinicalNote("n-1", "pat-1", "c-1", "doc-1", NoteType.PROGRESS_NOTE, "Progress", "Stable");
        when(clinicalNoteRepository.findByPatientIdOrderByCreatedAtDesc("pat-1", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(note)));

        Page<ClinicalNoteResponse> result = clinicalRecordService.getNotesByPatient("pat-1", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Progress");
    }
}
