package com.careflow.scheduling.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.DoctorSchedule;
import com.careflow.scheduling.domain.LeaveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class DoctorScheduleRepositoryTest {

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorLeaveRepository doctorLeaveRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("save should persist DoctorSchedule with audit fields and version (§11, §18)")
    void save_shouldPersistDoctorSchedule() {
        String id = UUID.randomUUID().toString();
        DoctorSchedule schedule = new DoctorSchedule(
                id,
                "staff-doc-001",
                DayOfWeek.THURSDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalTime.of(12, 0),
                LocalTime.of(12, 45),
                15
        );

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        entityManager.flush();
        entityManager.clear();

        List<DoctorSchedule> found = doctorScheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(
                "staff-doc-001", DayOfWeek.THURSDAY
        );

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(saved.getId());
        assertThat(found.get(0).getSlotDurationMinutes()).isEqualTo(15);
        assertThat(found.get(0).getCreatedAt()).isNotNull();
        assertThat(found.get(0).getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findOverlappingActiveLeaves should detect overlapping leave periods (§18, §92)")
    void findOverlappingActiveLeaves_shouldDetectOverlap() {
        String leaveId = UUID.randomUUID().toString();
        DoctorLeave leave = new DoctorLeave(
                leaveId,
                "staff-doc-001",
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 20),
                "Conference leave"
        );
        leave.setStatus(LeaveStatus.APPROVED);

        doctorLeaveRepository.save(leave);
        entityManager.flush();
        entityManager.clear();

        // 1. Partial overlap: 2026-11-15 to 2026-11-25
        List<DoctorLeave> overlap1 = doctorLeaveRepository.findOverlappingActiveLeaves(
                "staff-doc-001", LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 25)
        );
        assertThat(overlap1).isNotEmpty();

        // 2. Completely distinct date: 2026-11-21 to 2026-11-25
        List<DoctorLeave> distinct = doctorLeaveRepository.findOverlappingActiveLeaves(
                "staff-doc-001", LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 25)
        );
        assertThat(distinct).isEmpty();
    }
}
