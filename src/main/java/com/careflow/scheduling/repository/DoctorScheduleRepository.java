package com.careflow.scheduling.repository;

import com.careflow.scheduling.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DoctorSchedule (§11, §18).
 */
@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, String> {

    List<DoctorSchedule> findByDoctorId(String doctorId);

    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(String doctorId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(String doctorId, DayOfWeek dayOfWeek);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeek(String doctorId, DayOfWeek dayOfWeek);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.doctorId = :doctorId AND s.dayOfWeek = :dayOfWeek AND s.isActive = true")
    List<DoctorSchedule> findActiveSchedulesByDoctorAndDay(
            @Param("doctorId") String doctorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );
}
