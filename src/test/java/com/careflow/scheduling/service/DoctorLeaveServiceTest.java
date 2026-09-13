package com.careflow.scheduling.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.LeaveStatus;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.DoctorLeaveResponse;
import com.careflow.scheduling.dto.UpdateDoctorLeaveStatusRequest;
import com.careflow.scheduling.exception.DoctorLeaveNotFoundException;
import com.careflow.scheduling.exception.InvalidLeaveStatusTransitionException;
import com.careflow.scheduling.exception.LeaveOverlapException;
import com.careflow.scheduling.mapper.SchedulingMapper;
import com.careflow.scheduling.repository.DoctorLeaveRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorLeaveServiceTest {

    @Mock
    private DoctorLeaveRepository doctorLeaveRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    private SchedulingMapper schedulingMapper;
    private DoctorLeaveServiceImpl doctorLeaveService;

    private StaffMember mockDoctor;

    @BeforeEach
    void setUp() {
        schedulingMapper = new SchedulingMapper();
        doctorLeaveService = new DoctorLeaveServiceImpl(
                doctorLeaveRepository,
                staffMemberRepository,
                schedulingMapper
        );

        mockDoctor = new StaffMember(
                "doc-1",
                "DOC-001",
                "dept-card-001",
                "Alexander",
                "Fleming",
                "fleming@careflow.local",
                "+1-555-0102",
                StaffType.DOCTOR,
                LocalDate.of(2023, 1, 1)
        );
    }

    @Test
    @DisplayName("createLeave should succeed when dates are valid and no overlap (§18)")
    void createLeave_shouldSucceed_whenValid() {
        CreateDoctorLeaveRequest request = new CreateDoctorLeaveRequest(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "Annual vacation"
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorLeaveRepository.findOverlappingActiveLeaves("doc-1", request.startDate(), request.endDate()))
                .thenReturn(Collections.emptyList());
        when(doctorLeaveRepository.save(any(DoctorLeave.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorLeaveResponse response = doctorLeaveService.createLeave("doc-1", request);

        assertThat(response).isNotNull();
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        verify(doctorLeaveRepository).save(any(DoctorLeave.class));
    }

    @Test
    @DisplayName("createLeave should throw BusinessRuleException when startDate > endDate (§18)")
    void createLeave_shouldThrow_whenStartDateAfterEndDate() {
        CreateDoctorLeaveRequest request = new CreateDoctorLeaveRequest(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 5),
                "Invalid dates"
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));

        assertThatThrownBy(() -> doctorLeaveService.createLeave("doc-1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be after end date");
    }

    @Test
    @DisplayName("createLeave should throw LeaveOverlapException when overlapping active leave exists (§18, §92)")
    void createLeave_shouldThrow_whenOverlappingLeaveExists() {
        CreateDoctorLeaveRequest request = new CreateDoctorLeaveRequest(
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 7),
                "Conflicting request"
        );

        DoctorLeave existingLeave = new DoctorLeave(
                "leave-existing",
                "doc-1",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "Existing approved leave"
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorLeaveRepository.findOverlappingActiveLeaves("doc-1", request.startDate(), request.endDate()))
                .thenReturn(List.of(existingLeave));

        assertThatThrownBy(() -> doctorLeaveService.createLeave("doc-1", request))
                .isInstanceOf(LeaveOverlapException.class)
                .hasMessageContaining("overlapping the requested period");
    }

    @Test
    @DisplayName("updateLeaveStatus should transition status from PENDING to APPROVED (§18, §69)")
    void updateLeaveStatus_shouldTransitionFromPendingToApproved() {
        DoctorLeave existing = new DoctorLeave(
                "leave-1", "doc-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "Vacation"
        );
        existing.setStatus(LeaveStatus.PENDING);

        when(doctorLeaveRepository.findById("leave-1")).thenReturn(Optional.of(existing));
        when(doctorLeaveRepository.save(any(DoctorLeave.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorLeaveResponse response = doctorLeaveService.updateLeaveStatus(
                "leave-1", new UpdateDoctorLeaveStatusRequest(LeaveStatus.APPROVED)
        );

        assertThat(response.status()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("updateLeaveStatus should throw InvalidLeaveStatusTransitionException when transition illegal (§18, §69)")
    void updateLeaveStatus_shouldThrow_whenInvalidTransition() {
        DoctorLeave existing = new DoctorLeave(
                "leave-1", "doc-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "Vacation"
        );
        existing.setStatus(LeaveStatus.REJECTED);

        when(doctorLeaveRepository.findById("leave-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> doctorLeaveService.updateLeaveStatus(
                "leave-1", new UpdateDoctorLeaveStatusRequest(LeaveStatus.APPROVED)))
                .isInstanceOf(InvalidLeaveStatusTransitionException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    @DisplayName("getLeaveById should throw DoctorLeaveNotFoundException when not found (§18)")
    void getLeaveById_shouldThrow_whenNotFound() {
        when(doctorLeaveRepository.findById("missing-leave")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorLeaveService.getLeaveById("missing-leave"))
                .isInstanceOf(DoctorLeaveNotFoundException.class);
    }
}
