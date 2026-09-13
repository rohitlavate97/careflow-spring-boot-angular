package com.careflow.admission.service;

import com.careflow.admission.domain.Admission;
import com.careflow.admission.domain.AdmissionStatus;
import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.domain.BedTransferRecord;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.RoomType;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.domain.WardType;
import com.careflow.admission.dto.AdmissionResponse;
import com.careflow.admission.dto.AdmitPatientRequest;
import com.careflow.admission.dto.DischargePatientRequest;
import com.careflow.admission.dto.TransferBedRequest;
import com.careflow.admission.exception.BedNotAvailableException;
import com.careflow.admission.exception.InvalidAdmissionStatusTransitionException;
import com.careflow.admission.exception.PatientAlreadyAdmittedException;
import com.careflow.admission.mapper.AdmissionMapper;
import com.careflow.admission.repository.AdmissionRepository;
import com.careflow.admission.repository.BedRepository;
import com.careflow.admission.repository.BedTransferRecordRepository;
import com.careflow.admission.repository.RoomRepository;
import com.careflow.admission.repository.WardRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.department.repository.DepartmentRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private WardRepository wardRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private BedTransferRecordRepository bedTransferRecordRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @Spy
    private AdmissionMapper admissionMapper = new AdmissionMapper();

    @InjectMocks
    private AdmissionServiceImpl admissionService;

    private StaffMember doctor;
    private StaffMember nurse;
    private Ward ward;
    private Room room;
    private Bed bed1;
    private Bed bed2;

    @BeforeEach
    void setUp() {
        doctor = new StaffMember("doc-1", "DOC-001", "dept-1", "Gregory", "House", "house@careflow.local", "+1234567890", StaffType.DOCTOR, LocalDate.now());
        nurse = new StaffMember("nur-1", "NUR-001", "dept-1", "Florence", "Nightingale", "nurse@careflow.local", "+1234567891", StaffType.NURSE, LocalDate.now());

        ward = new Ward("ward-1", "WARD-ICU", "ICU", "dept-1", WardType.ICU, "Floor 2", 2, true);
        room = new Room("room-1", "ICU-101", ward, RoomType.PRIVATE, true);
        bed1 = new Bed("bed-1", "BED-101-A", room, BedStatus.AVAILABLE, BigDecimal.valueOf(500.00), true);
        bed2 = new Bed("bed-2", "BED-101-B", room, BedStatus.AVAILABLE, BigDecimal.valueOf(500.00), true);
    }

    @Test
    @DisplayName("Should successfully admit patient to an AVAILABLE bed")
    void admitPatient_success() {
        AdmitPatientRequest request = new AdmitPatientRequest(
                "pat-1", "doc-1", "bed-1", null, "Acute coronary syndrome", "STEMI"
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(admissionRepository.existsByPatientIdAndStatusIn(eq("pat-1"), anyCollection())).thenReturn(false);
        when(bedRepository.findByIdForUpdate("bed-1")).thenReturn(Optional.of(bed1));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArgument(0));

        AdmissionResponse response = admissionService.admitPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("pat-1");
        assertThat(response.status()).isEqualTo(AdmissionStatus.ADMITTED);
        assertThat(bed1.getStatus()).isEqualTo(BedStatus.OCCUPIED);
        verify(bedRepository).save(bed1);
        verify(admissionRepository).save(any(Admission.class));
        verify(bedTransferRecordRepository).save(any(BedTransferRecord.class));
    }

    @Test
    @DisplayName("Should reject admission when target bed is already OCCUPIED")
    void admitPatient_bedOccupied_throwsException() {
        bed1.setStatus(BedStatus.OCCUPIED);

        AdmitPatientRequest request = new AdmitPatientRequest(
                "pat-1", "doc-1", "bed-1", null, "Emergency admission", null
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(admissionRepository.existsByPatientIdAndStatusIn(eq("pat-1"), anyCollection())).thenReturn(false);
        when(bedRepository.findByIdForUpdate("bed-1")).thenReturn(Optional.of(bed1));

        assertThatThrownBy(() -> admissionService.admitPatient(request))
                .isInstanceOf(BedNotAvailableException.class)
                .hasMessageContaining("not available for allocation");
    }

    @Test
    @DisplayName("Should reject admission when patient already has an active admission")
    void admitPatient_patientAlreadyAdmitted_throwsException() {
        AdmitPatientRequest request = new AdmitPatientRequest(
                "pat-1", "doc-1", "bed-1", null, "Second admission", null
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(doctor));
        when(admissionRepository.existsByPatientIdAndStatusIn(eq("pat-1"), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> admissionService.admitPatient(request))
                .isInstanceOf(PatientAlreadyAdmittedException.class)
                .hasMessageContaining("already has an active admission");
    }

    @Test
    @DisplayName("Should reject admission when admitting doctor is not DOCTOR")
    void admitPatient_nonDoctor_throwsException() {
        AdmitPatientRequest request = new AdmitPatientRequest(
                "pat-1", "nur-1", "bed-1", null, "Admission by nurse", null
        );

        when(patientRepository.existsById("pat-1")).thenReturn(true);
        when(staffMemberRepository.findById("nur-1")).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> admissionService.admitPatient(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only DOCTOR can perform this action");
    }

    @Test
    @DisplayName("Should transfer inpatient to target bed and release previous bed")
    void transferBed_success() {
        Admission admission = new Admission(
                "adm-1", "ADM-001", "pat-1", "doc-1", bed1, null, "Condition", null, null
        );
        bed1.setStatus(BedStatus.OCCUPIED);

        when(admissionRepository.findById("adm-1")).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdForUpdate("bed-2")).thenReturn(Optional.of(bed2));
        when(bedRepository.findByIdForUpdate("bed-1")).thenReturn(Optional.of(bed1));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArgument(0));

        TransferBedRequest request = new TransferBedRequest("bed-2", "Step down to recovery", "doc-1");
        AdmissionResponse response = admissionService.transferBed("adm-1", request);

        assertThat(response.status()).isEqualTo(AdmissionStatus.TRANSFERRED);
        assertThat(bed1.getStatus()).isEqualTo(BedStatus.AVAILABLE);
        assertThat(bed2.getStatus()).isEqualTo(BedStatus.OCCUPIED);
    }

    @Test
    @DisplayName("Should discharge inpatient and release bed to AVAILABLE")
    void dischargePatient_success() {
        Admission admission = new Admission(
                "adm-1", "ADM-001", "pat-1", "doc-1", bed1, null, "Condition", null, null
        );
        bed1.setStatus(BedStatus.OCCUPIED);

        when(admissionRepository.findById("adm-1")).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdForUpdate("bed-1")).thenReturn(Optional.of(bed1));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArgument(0));

        DischargePatientRequest request = new DischargePatientRequest("Patient fully recovered and ambulatory");
        AdmissionResponse response = admissionService.dischargePatient("adm-1", request);

        assertThat(response.status()).isEqualTo(AdmissionStatus.DISCHARGED);
        assertThat(bed1.getStatus()).isEqualTo(BedStatus.AVAILABLE);
        assertThat(admission.getCurrentBed()).isNull();
    }

    @Test
    @DisplayName("Should reject discharge on already discharged patient")
    void dischargePatient_alreadyDischarged_throwsException() {
        Admission admission = new Admission(
                "adm-1", "ADM-001", "pat-1", "doc-1", null, null, "Condition", null, null
        );
        admission.setStatus(AdmissionStatus.DISCHARGED);

        when(admissionRepository.findById("adm-1")).thenReturn(Optional.of(admission));

        DischargePatientRequest request = new DischargePatientRequest("Discharge again");

        assertThatThrownBy(() -> admissionService.dischargePatient("adm-1", request))
                .isInstanceOf(InvalidAdmissionStatusTransitionException.class)
                .hasMessageContaining("Cannot discharge patient from admission in status: DISCHARGED");
    }
}
