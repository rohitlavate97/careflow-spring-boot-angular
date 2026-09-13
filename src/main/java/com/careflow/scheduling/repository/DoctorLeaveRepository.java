package com.careflow.scheduling.repository;

import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for DoctorLeave (§11, §18).
 */
@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, String> {

    List<DoctorLeave> findByDoctorId(String doctorId);

    Page<DoctorLeave> findByDoctorId(String doctorId, Pageable pageable);

    List<DoctorLeave> findByDoctorIdAndStatus(String doctorId, LeaveStatus status);

    @Query("SELECT l FROM DoctorLeave l WHERE l.doctorId = :doctorId AND l.status = 'APPROVED' " +
           "AND l.startDate <= :date AND l.endDate >= :date")
    List<DoctorLeave> findApprovedLeavesCoveringDate(
            @Param("doctorId") String doctorId,
            @Param("date") LocalDate date
    );

    @Query("SELECT l FROM DoctorLeave l WHERE l.doctorId = :doctorId " +
           "AND l.status IN ('PENDING', 'APPROVED') " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<DoctorLeave> findOverlappingActiveLeaves(
            @Param("doctorId") String doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
