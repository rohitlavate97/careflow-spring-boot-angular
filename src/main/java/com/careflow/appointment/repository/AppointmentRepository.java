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

    interface DoctorAppointmentSlotProjection {
        LocalDateTime getAppointmentDateTime();
        Integer getDurationMinutes();
    }
}
