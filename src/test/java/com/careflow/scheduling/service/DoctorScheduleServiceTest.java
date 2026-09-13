package com.careflow.scheduling.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.DoctorSchedule;
import com.careflow.scheduling.dto.AvailableSlotResponse;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.DoctorScheduleResponse;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;
import com.careflow.scheduling.exception.DoctorScheduleNotFoundException;
import com.careflow.scheduling.exception.InvalidScheduleTimeException;
import com.careflow.scheduling.exception.ScheduleOverlapException;
import com.careflow.scheduling.mapper.SchedulingMapper;
import com.careflow.scheduling.repository.DoctorLeaveRepository;
import com.careflow.scheduling.repository.DoctorScheduleRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceTest {

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private DoctorLeaveRepository doctorLeaveRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    private SchedulingMapper schedulingMapper;
    private DoctorScheduleServiceImpl doctorScheduleService;

    private StaffMember mockDoctor;

    @BeforeEach
    void setUp() {
        schedulingMapper = new SchedulingMapper();
        doctorScheduleService = new DoctorScheduleServiceImpl(
                doctorScheduleRepository,
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
    @DisplayName("createSchedule should succeed when valid parameters provided (§18)")
    void createSchedule_shouldSucceed_whenValid() {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                30
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek("doc-1", DayOfWeek.MONDAY))
                .thenReturn(Collections.emptyList());
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorScheduleResponse response = doctorScheduleService.createSchedule("doc-1", request);

        assertThat(response).isNotNull();
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(response.breakStartTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.breakEndTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(response.slotDurationMinutes()).isEqualTo(30);
        assertThat(response.isActive()).isTrue();
        verify(doctorScheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    @DisplayName("createSchedule should throw InvalidScheduleTimeException when start time >= end time (§18)")
    void createSchedule_shouldThrow_whenStartTimeNotBeforeEndTime() {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(14, 0),
                LocalTime.of(12, 0),
                null,
                null,
                30
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));

        assertThatThrownBy(() -> doctorScheduleService.createSchedule("doc-1", request))
                .isInstanceOf(InvalidScheduleTimeException.class)
                .hasMessageContaining("must be before end time");
    }

    @Test
    @DisplayName("createSchedule should throw InvalidScheduleTimeException when break outside working hours (§18)")
    void createSchedule_shouldThrow_whenBreakOutsideWorkingHours() {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalTime.of(13, 0),
                LocalTime.of(13, 30),
                30
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));

        assertThatThrownBy(() -> doctorScheduleService.createSchedule("doc-1", request))
                .isInstanceOf(InvalidScheduleTimeException.class)
                .hasMessageContaining("must be strictly within working hours");
    }

    @Test
    @DisplayName("createSchedule should throw ScheduleOverlapException when overlapping schedule exists on same day (§18, §92)")
    void createSchedule_shouldThrow_whenScheduleOverlaps() {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                null,
                null,
                30
        );

        DoctorSchedule existing = new DoctorSchedule(
                "sched-existing",
                "doc-1",
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                null,
                null,
                30
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorScheduleRepository.findByDoctorIdAndDayOfWeek("doc-1", DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> doctorScheduleService.createSchedule("doc-1", request))
                .isInstanceOf(ScheduleOverlapException.class)
                .hasMessageContaining("overlapping with requested");
    }

    @Test
    @DisplayName("createSchedule should throw StaffNotFoundException when doctor does not exist (§18)")
    void createSchedule_shouldThrow_whenDoctorNotFound() {
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                null,
                null,
                30
        );

        when(staffMemberRepository.findById("missing-doc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorScheduleService.createSchedule("missing-doc", request))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("createSchedule should throw BusinessRuleException when staff is not a DOCTOR (§18)")
    void createSchedule_shouldThrow_whenStaffNotDoctor() {
        StaffMember nurse = new StaffMember(
                "nurse-1", "NUR-001", "dept-emer-001", "Florence", "Nightingale",
                "nurse@careflow.local", "+1-555-0103", StaffType.NURSE, LocalDate.of(2023, 1, 1)
        );
        CreateDoctorScheduleRequest request = new CreateDoctorScheduleRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                null,
                null,
                30
        );

        when(staffMemberRepository.findById("nurse-1")).thenReturn(Optional.of(nurse));

        assertThatThrownBy(() -> doctorScheduleService.createSchedule("nurse-1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not a DOCTOR");
    }

    @Test
    @DisplayName("getAvailableSlots should correctly generate discrete slots and skip break period (§18)")
    void getAvailableSlots_shouldGenerateCorrectSlots_withBreak() {
        // Date: 2026-09-14 is a MONDAY
        LocalDate monday = LocalDate.of(2026, 9, 14);
        assertThat(monday.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        // Schedule: 09:00 to 13:00, break 11:00 to 11:30, slot duration 30m
        DoctorSchedule schedule = new DoctorSchedule(
                "sched-1",
                "doc-1",
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                30
        );

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorLeaveRepository.findApprovedLeavesCoveringDate("doc-1", monday))
                .thenReturn(Collections.emptyList());
        when(doctorScheduleRepository.findActiveSchedulesByDoctorAndDay("doc-1", DayOfWeek.MONDAY))
                .thenReturn(List.of(schedule));

        List<AvailableSlotResponse> slots = doctorScheduleService.getAvailableSlots("doc-1", monday);

        // Expected slots:
        // 09:00 - 09:30
        // 09:30 - 10:00
        // 10:00 - 10:30
        // 10:30 - 11:00
        // [11:00 - 11:30 skipped as break]
        // 11:30 - 12:00
        // 12:00 - 12:30
        // 12:30 - 13:00
        // Total = 7 slots
        assertThat(slots).hasSize(7);
        assertThat(slots.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(0).endTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots.get(3).startTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(slots.get(3).endTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(slots.get(4).startTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(slots.get(4).endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(slots.get(6).startTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(slots.get(6).endTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("getAvailableSlots should return empty list when doctor is on approved leave (§18)")
    void getAvailableSlots_shouldReturnEmpty_whenDoctorOnApprovedLeave() {
        LocalDate monday = LocalDate.of(2026, 9, 14);
        DoctorLeave approvedLeave = new DoctorLeave(
                "leave-1",
                "doc-1",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 20),
                "Annual Medical Conference"
        );
        approvedLeave.approve();

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorLeaveRepository.findApprovedLeavesCoveringDate("doc-1", monday))
                .thenReturn(List.of(approvedLeave));

        List<AvailableSlotResponse> slots = doctorScheduleService.getAvailableSlots("doc-1", monday);

        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("getAvailableSlots should return empty list when no schedule exists for that day of week (§18)")
    void getAvailableSlots_shouldReturnEmpty_whenNoScheduleForDay() {
        // Date: 2026-09-15 is a TUESDAY
        LocalDate tuesday = LocalDate.of(2026, 9, 15);

        when(staffMemberRepository.findById("doc-1")).thenReturn(Optional.of(mockDoctor));
        when(doctorLeaveRepository.findApprovedLeavesCoveringDate("doc-1", tuesday))
                .thenReturn(Collections.emptyList());
        when(doctorScheduleRepository.findActiveSchedulesByDoctorAndDay("doc-1", DayOfWeek.TUESDAY))
                .thenReturn(Collections.emptyList());

        List<AvailableSlotResponse> slots = doctorScheduleService.getAvailableSlots("doc-1", tuesday);

        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("deleteSchedule should delete existing schedule (§18)")
    void deleteSchedule_shouldSucceed_whenFound() {
        DoctorSchedule schedule = new DoctorSchedule(
                "sched-1", "doc-1", DayOfWeek.FRIDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), null, null, 30
        );

        when(doctorScheduleRepository.findById("sched-1")).thenReturn(Optional.of(schedule));

        doctorScheduleService.deleteSchedule("sched-1");

        verify(doctorScheduleRepository).delete(schedule);
    }
}
