package com.careflow.appointment.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Hospital appointment aggregate root managing scheduling, check-in, and clinical consultation lifecycle (§19, §20).
 */
@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_active_appointment_slot",
                        columnNames = {"doctor_id", "appointment_date_time", "active_slot_flag"}
                )
        },
        indexes = {
                @Index(name = "idx_appointments_doctor_date", columnList = "doctor_id, appointment_date_time"),
                @Index(name = "idx_appointments_patient_date", columnList = "patient_id, appointment_date_time"),
                @Index(name = "idx_appointments_status", columnList = "status")
        }
)
public class Appointment extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "department_id", length = 64, nullable = false)
    private String departmentId;

    @Column(name = "appointment_date_time", nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    /**
     * Set to 1 for active appointments holding a doctor slot reservation (§20).
     * Set to NULL when the appointment is CANCELLED or marked NO_SHOW,
     * releasing the slot for subsequent bookings in MySQL unique index.
     */
    @Column(name = "active_slot_flag")
    private Integer activeSlotFlag = 1;

    @Column(name = "reason_for_visit", length = 500)
    private String reasonForVisit;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    public Appointment() {
    }

    public Appointment(String id,
                       String patientId,
                       String doctorId,
                       String departmentId,
                       LocalDateTime appointmentDateTime,
                       Integer durationMinutes,
                       String reasonForVisit) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.departmentId = departmentId;
        this.appointmentDateTime = appointmentDateTime;
        this.durationMinutes = durationMinutes;
        this.reasonForVisit = reasonForVisit != null ? reasonForVisit.trim() : null;
        this.status = AppointmentStatus.REQUESTED;
        this.activeSlotFlag = 1;
    }

    // Business Methods (§19, §20, §69)

    public void transitionTo(AppointmentStatus targetStatus, String reason) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition appointment %s from status %s to %s",
                            this.id, this.status, targetStatus)
            );
        }

        this.status = targetStatus;
        if (targetStatus == AppointmentStatus.CANCELLED) {
            this.cancellationReason = reason != null ? reason.trim() : null;
            this.activeSlotFlag = null; // Release slot reservation (§20)
        } else if (targetStatus == AppointmentStatus.NO_SHOW) {
            this.activeSlotFlag = null; // Release slot reservation (§20)
        } else {
            this.activeSlotFlag = 1;
        }
    }

    public void reschedule(LocalDateTime newDateTime) {
        if (!this.status.isActive()) {
            throw new IllegalStateException(
                    String.format("Cannot reschedule appointment %s in inactive status %s",
                            this.id, this.status)
            );
        }
        this.appointmentDateTime = newDateTime;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Integer getActiveSlotFlag() {
        return activeSlotFlag;
    }

    public void setActiveSlotFlag(Integer activeSlotFlag) {
        this.activeSlotFlag = activeSlotFlag;
    }

    public String getReasonForVisit() {
        return reasonForVisit;
    }

    public void setReasonForVisit(String reasonForVisit) {
        this.reasonForVisit = reasonForVisit != null ? reasonForVisit.trim() : null;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason != null ? cancellationReason.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
