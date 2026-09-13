package com.careflow.staff.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.service.DepartmentService;
import com.careflow.identity.repository.UserRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorProfileDto;
import com.careflow.staff.dto.DoctorSummaryResponse;
import com.careflow.staff.dto.StaffResponse;
import com.careflow.staff.dto.UpdateStaffStatusRequest;
import com.careflow.staff.exception.DuplicateDoctorLicenseException;
import com.careflow.staff.exception.DuplicateStaffException;
import com.careflow.staff.exception.InvalidStaffStatusTransitionException;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.mapper.StaffMapper;
import com.careflow.staff.repository.DoctorProfileRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private UserRepository userRepository;

    private StaffMapper staffMapper;
    private StaffServiceImpl staffService;

    @BeforeEach
    void setUp() {
        staffMapper = new StaffMapper();
        staffService = new StaffServiceImpl(
                staffMemberRepository,
                doctorProfileRepository,
                departmentService,
                userRepository,
                staffMapper
        );
    }

    @Test
    @DisplayName("createStaff should succeed for doctor with valid doctor profile (§17)")
    void createStaff_shouldSucceed_forDoctor() {
        DoctorProfileDto docDto = new DoctorProfileDto(
                "Cardiology",
                "MD, FACC",
                "MED-12345",
                BigDecimal.valueOf(200.00),
                "Room 101",
                "Expert cardiologist"
        );
        CreateStaffRequest request = new CreateStaffRequest(
                "DOC-001",
                "user-doc-001",
                "dept-card-001",
                "Gregory",
                "House",
                "house@careflow.local",
                "+1-555-0199",
                StaffType.DOCTOR,
                LocalDate.of(2023, 1, 1),
                docDto
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("DOC-001")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("house@careflow.local")).thenReturn(false);
        when(staffMemberRepository.existsByUserId("user-doc-001")).thenReturn(false);
        when(userRepository.existsById("user-doc-001")).thenReturn(true);
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );
        when(doctorProfileRepository.existsByMedicalLicenseNumberIgnoreCase("MED-12345")).thenReturn(false);
        when(staffMemberRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffResponse response = staffService.createStaff(request);

        assertThat(response).isNotNull();
        assertThat(response.staffCode()).isEqualTo("DOC-001");
        assertThat(response.staffType()).isEqualTo(StaffType.DOCTOR);
        assertThat(response.doctorProfile()).isNotNull();
        assertThat(response.doctorProfile().specialization()).isEqualTo("Cardiology");
        assertThat(response.doctorProfile().medicalLicenseNumber()).isEqualTo("MED-12345");
        verify(staffMemberRepository).save(any(StaffMember.class));
    }

    @Test
    @DisplayName("createStaff should succeed for nurse without doctor profile (§17)")
    void createStaff_shouldSucceed_forNurse() {
        CreateStaffRequest request = new CreateStaffRequest(
                "NUR-001",
                null,
                "dept-emer-001",
                "Clara",
                "Oswald",
                "clara@careflow.local",
                "+1-555-0198",
                StaffType.NURSE,
                LocalDate.of(2023, 2, 1),
                null
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("NUR-001")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("clara@careflow.local")).thenReturn(false);
        when(departmentService.getDepartmentById("dept-emer-001")).thenReturn(
                new DepartmentResponse("dept-emer-001", "EMER", "Emergency", null, null, null, null, null, null, null, null, 0L)
        );
        when(staffMemberRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffResponse response = staffService.createStaff(request);

        assertThat(response).isNotNull();
        assertThat(response.staffCode()).isEqualTo("NUR-001");
        assertThat(response.staffType()).isEqualTo(StaffType.NURSE);
        assertThat(response.doctorProfile()).isNull();
    }

    @Test
    @DisplayName("createStaff should throw BusinessRuleException when doctor created without doctor profile (§17)")
    void createStaff_shouldThrow_whenDoctorWithoutProfile() {
        CreateStaffRequest request = new CreateStaffRequest(
                "DOC-002",
                null,
                "dept-card-001",
                "James",
                "Wilson",
                "wilson@careflow.local",
                "+1-555-0197",
                StaffType.DOCTOR,
                LocalDate.of(2023, 3, 1),
                null
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("DOC-002")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("wilson@careflow.local")).thenReturn(false);
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Doctor profile credentials are required");
    }

    @Test
    @DisplayName("createStaff should throw DuplicateStaffException when staff code exists (§17, §92)")
    void createStaff_shouldThrow_whenDuplicateStaffCode() {
        CreateStaffRequest request = new CreateStaffRequest(
                "DOC-001",
                null,
                "dept-card-001",
                "Duplicate",
                "Doc",
                "dup@careflow.local",
                "+1-555-0196",
                StaffType.DOCTOR,
                LocalDate.of(2023, 1, 1),
                null
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("DOC-001")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(DuplicateStaffException.class)
                .hasMessageContaining("DOC-001");
    }

    @Test
    @DisplayName("createStaff should throw DuplicateStaffException when email exists (§17, §92)")
    void createStaff_shouldThrow_whenDuplicateEmail() {
        CreateStaffRequest request = new CreateStaffRequest(
                "NUR-002",
                null,
                "dept-emer-001",
                "Amy",
                "Pond",
                "existing@careflow.local",
                "+1-555-0195",
                StaffType.NURSE,
                LocalDate.of(2023, 2, 1),
                null
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("NUR-002")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("existing@careflow.local")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(DuplicateStaffException.class)
                .hasMessageContaining("existing@careflow.local");
    }

    @Test
    @DisplayName("createStaff should throw DuplicateDoctorLicenseException when license exists (§17, §92)")
    void createStaff_shouldThrow_whenDuplicateLicenseNumber() {
        DoctorProfileDto docDto = new DoctorProfileDto(
                "Cardiology", "MD", "MED-DUP", BigDecimal.valueOf(100), null, null
        );
        CreateStaffRequest request = new CreateStaffRequest(
                "DOC-003", null, "dept-card-001", "Doc", "Three",
                "doc3@careflow.local", "+1-555-0194", StaffType.DOCTOR,
                LocalDate.of(2023, 1, 1), docDto
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("DOC-003")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("doc3@careflow.local")).thenReturn(false);
        when(departmentService.getDepartmentById("dept-card-001")).thenReturn(
                new DepartmentResponse("dept-card-001", "CARD", "Cardiology", null, null, null, null, null, null, null, null, 0L)
        );
        when(doctorProfileRepository.existsByMedicalLicenseNumberIgnoreCase("MED-DUP")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(DuplicateDoctorLicenseException.class)
                .hasMessageContaining("MED-DUP");
    }

    @Test
    @DisplayName("createStaff should throw ResourceNotFoundException when userId does not exist (§17)")
    void createStaff_shouldThrow_whenUserNotFound() {
        CreateStaffRequest request = new CreateStaffRequest(
                "REC-001", "missing-user", "dept-genm-001", "Rory", "Williams",
                "rory@careflow.local", "+1-555-0193", StaffType.RECEPTIONIST,
                LocalDate.of(2023, 3, 1), null
        );

        when(staffMemberRepository.existsByStaffCodeIgnoreCase("REC-001")).thenReturn(false);
        when(staffMemberRepository.existsByEmailIgnoreCase("rory@careflow.local")).thenReturn(false);
        when(staffMemberRepository.existsByUserId("missing-user")).thenReturn(false);
        when(userRepository.existsById("missing-user")).thenReturn(false);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-user");
    }

    @Test
    @DisplayName("getStaffById should return staff response when found (§17)")
    void getStaffById_shouldReturnStaff_whenFound() {
        StaffMember staff = new StaffMember("s-1", "DOC-001", "dept-card-001", "Gregory", "House",
                "house@careflow.local", "+1-555-0199", StaffType.DOCTOR, LocalDate.of(2023, 1, 1));
        when(staffMemberRepository.findByIdWithDoctorProfile("s-1")).thenReturn(Optional.of(staff));

        StaffResponse response = staffService.getStaffById("s-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("s-1");
        assertThat(response.staffCode()).isEqualTo("DOC-001");
    }

    @Test
    @DisplayName("getStaffById should throw StaffNotFoundException when not found (§17)")
    void getStaffById_shouldThrowNotFound_whenNotFound() {
        when(staffMemberRepository.findByIdWithDoctorProfile("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.getStaffById("non-existent"))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("updateStaffStatus should transition status from ACTIVE to ON_LEAVE (§17, §69)")
    void updateStaffStatus_shouldTransitionStatus_whenValid() {
        StaffMember staff = new StaffMember("s-1", "NUR-001", "dept-emer-001", "Clara", "Oswald",
                "clara@careflow.local", "+1-555-0198", StaffType.NURSE, LocalDate.of(2023, 2, 1));
        staff.setStatus(StaffStatus.ACTIVE);

        when(staffMemberRepository.findByIdWithDoctorProfile("s-1")).thenReturn(Optional.of(staff));
        when(staffMemberRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffResponse response = staffService.updateStaffStatus("s-1", new UpdateStaffStatusRequest(StaffStatus.ON_LEAVE));

        assertThat(response.status()).isEqualTo(StaffStatus.ON_LEAVE);
    }

    @Test
    @DisplayName("updateStaffStatus should throw InvalidStaffStatusTransitionException when terminal (§17, §69)")
    void updateStaffStatus_shouldThrow_whenTransitionFromTerminalState() {
        StaffMember staff = new StaffMember("s-1", "NUR-001", "dept-emer-001", "Clara", "Oswald",
                "clara@careflow.local", "+1-555-0198", StaffType.NURSE, LocalDate.of(2023, 2, 1));
        staff.setStatus(StaffStatus.TERMINATED);

        when(staffMemberRepository.findByIdWithDoctorProfile("s-1")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> staffService.updateStaffStatus("s-1", new UpdateStaffStatusRequest(StaffStatus.ACTIVE)))
                .isInstanceOf(InvalidStaffStatusTransitionException.class)
                .hasMessageContaining("TERMINATED");
    }

    @Test
    @DisplayName("searchDoctors should return paged doctor summaries (§17)")
    void searchDoctors_shouldReturnPagedResults() {
        StaffMember staff = new StaffMember("s-1", "DOC-001", "dept-card-001", "Alexander", "Fleming",
                "fleming@careflow.local", "+1-555-0102", StaffType.DOCTOR, LocalDate.of(2023, 1, 1));
        DoctorProfile doc = new DoctorProfile("d-1", "Cardiology", "MD", "MED-1", BigDecimal.valueOf(150), "Room 1", null);
        staff.setDoctorProfile(doc);

        Page<DoctorProfile> page = new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1);
        when(doctorProfileRepository.findActiveDoctorsPaged(eq("Cardiology"), eq("dept-card-001"), any())).thenReturn(page);

        PageResponse<DoctorSummaryResponse> response = staffService.searchDoctors("Cardiology", "dept-card-001", PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).specialization()).isEqualTo("Cardiology");
        assertThat(response.content().get(0).doctorName()).isEqualTo("Alexander Fleming");
    }
}
