package com.careflow.appointment.repository;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Appointment entity (§11, §19, §96).
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByDoctorId(String doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(
            String doctorId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<Appointment> findByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag(
            String doctorId,
            LocalDateTime appointmentDateTime,
            Integer activeSlotFlag
    );

    boolean existsByDoctorIdAndAppointmentDateTimeAndActiveSlotFlag(
            String doctorId,
            LocalDateTime appointmentDateTime,
            Integer activeSlotFlag
    );

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.activeSlotFlag = 1 " +
           "AND a.appointmentDateTime >= :start AND a.appointmentDateTime < :end")
    List<DoctorAppointmentSlotProjection> findActiveBookingsForDoctorInRange(
            @Param("doctorId") String doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end")
    long countAppointmentsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end AND a.status = :status")
    long countAppointmentsInRangeByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("status") AppointmentStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end " +
           "AND (:departmentId IS NULL OR a.departmentId = :departmentId) " +
           "AND (:doctorId IS NULL OR a.doctorId = :doctorId) " +
           "GROUP BY a.status")
    List<Object[]> countAppointmentsByStatusInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("departmentId") String departmentId,
            @Param("doctorId") String doctorId
    );

    @Query("SELECT a.departmentId, COUNT(a) FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end " +
           "GROUP BY a.departmentId ORDER BY COUNT(a) DESC")
    List<Object[]> countAppointmentsByDepartmentInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT a.doctorId, COUNT(a), " +
           "SUM(CASE WHEN a.status = com.careflow.appointment.domain.AppointmentStatus.COMPLETED THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.status = com.careflow.appointment.domain.AppointmentStatus.CANCELLED THEN 1 ELSE 0 END) " +
           "FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end " +
           "GROUP BY a.doctorId")
    List<Object[]> countDoctorUtilizationInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT a.appointmentDateTime, a.status FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end " +
           "AND (:departmentId IS NULL OR a.departmentId = :departmentId) " +
           "AND (:doctorId IS NULL OR a.doctorId = :doctorId) " +
           "ORDER BY a.appointmentDateTime ASC")
    List<Object[]> findAppointmentsTimelineInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("departmentId") String departmentId,
            @Param("doctorId") String doctorId
    );

    interface DoctorAppointmentSlotProjection {
        LocalDateTime getAppointmentDateTime();
        Integer getDurationMinutes();
    }
}
