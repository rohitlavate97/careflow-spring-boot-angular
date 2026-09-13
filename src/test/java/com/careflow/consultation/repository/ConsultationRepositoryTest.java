package com.careflow.consultation.repository;

import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationDiagnosis;
import com.careflow.consultation.domain.ConsultationStatus;
import com.careflow.consultation.domain.ConsultationVitals;
import com.careflow.consultation.domain.DiagnosisType;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ConsultationRepositoryTest {

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Patient patient;
    private StaffMember doctor;

    @BeforeEach
    void setUp() {
        Department dept = new Department(
                UUID.randomUUID().toString(), "CARD-REPO", "Cardiology Repo", "Desc", "Building A"
        );
        departmentRepository.save(dept);

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-REPO-01", dept.getId(),
                "Stephen", "Strange", "strange.repo@careflow.local", "+1-555-0991",
                StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "Cardiology", "MD", "LIC-REPO-01",
                BigDecimal.valueOf(250), "Room 101", null
        );
        doctor.setDoctorProfile(docProfile);
        staffMemberRepository.save(doctor);

        patient = new Patient(
                UUID.randomUUID().toString(), "CF-2026-REPO", "Bruce", "Banner",
                LocalDate.of(1980, 5, 20), Gender.MALE, "+1-555-0992"
        );
        patientRepository.save(patient);
    }

    @Test
    @DisplayName("Should persist consultation with vitals and retrieve using EntityGraph")
    void persistConsultation_withVitalsAndDiagnoses() {
        Consultation consultation = new Consultation(
                UUID.randomUUID().toString(),
                patient.getId(),
                doctor.getId(),
                null,
                null,
                Instant.now()
        );

        ConsultationVitals vitals = new ConsultationVitals(
                120, 80, 75, 16,
                new BigDecimal("37.0"),
                99,
                new BigDecimal("175.0"),
                new BigDecimal("70.00")
        );
        consultation.updateVitals(vitals);

        ConsultationDiagnosis diagnosis = new ConsultationDiagnosis(
                UUID.randomUUID().toString(),
                consultation,
                "I10",
                "Essential (primary) hypertension",
                DiagnosisType.PRIMARY,
                "MILD",
                "Start lifestyle modification"
        );
        consultation.addDiagnosis(diagnosis);

        consultationRepository.saveAndFlush(consultation);

        Optional<Consultation> loaded = consultationRepository.findWithDiagnosesById(consultation.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getStatus()).isEqualTo(ConsultationStatus.STARTED);
        assertThat(loaded.get().getVitals()).isNotNull();
        assertThat(loaded.get().getVitals().getSystolicBp()).isEqualTo(120);
        assertThat(loaded.get().getVitals().getBmi()).isNotNull();
        assertThat(loaded.get().getDiagnoses()).hasSize(1);
        assertThat(loaded.get().getDiagnoses().iterator().next().getDiagnosisCode()).isEqualTo("I10");
    }

    @Test
    @DisplayName("Should query consultations by patient and doctor with pagination")
    void queryConsultationsByPatientAndDoctor() {
        Consultation c1 = new Consultation(UUID.randomUUID().toString(), patient.getId(), doctor.getId(), null, null, Instant.now());
        consultationRepository.saveAndFlush(c1);

        Page<Consultation> patientConsultations = consultationRepository
                .findByPatientIdOrderByStartedAtDesc(patient.getId(), PageRequest.of(0, 10));

        assertThat(patientConsultations.getContent()).hasSize(1);
        assertThat(patientConsultations.getContent().get(0).getId()).isEqualTo(c1.getId());

        Page<Consultation> doctorConsultations = consultationRepository
                .findByDoctorIdOrderByStartedAtDesc(doctor.getId(), PageRequest.of(0, 10));

        assertThat(doctorConsultations.getContent()).hasSize(1);
        assertThat(doctorConsultations.getContent().get(0).getId()).isEqualTo(c1.getId());
    }
}
