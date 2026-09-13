package com.careflow.queue.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.queue.exception.InvalidQueueStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Patient queue ticket aggregate root managing token assignment, triage priority,
 * and consultation progression lifecycle (§21, §103 Phase 6).
 */
@Entity
@Table(
        name = "queue_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_queue_dept_date_token",
                        columnNames = {"department_id", "queue_date", "token_number"}
                )
        },
        indexes = {
                @Index(name = "idx_queue_dept_date_status", columnList = "department_id, queue_date, status"),
                @Index(name = "idx_queue_doc_date_status", columnList = "doctor_id, queue_date, status"),
                @Index(name = "idx_queue_patient", columnList = "patient_id"),
                @Index(name = "idx_queue_appointment", columnList = "appointment_id")
        }
)
public class QueueEntry extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "department_id", length = 64, nullable = false)
    private String departmentId;

    @Column(name = "doctor_id", length = 64)
    private String doctorId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "appointment_id", length = 64)
    private String appointmentId;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Column(name = "token_display", length = 32, nullable = false)
    private String tokenDisplay;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private QueuePriority priority = QueuePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "called_time")
    private Instant calledTime;

    @Column(name = "consultation_start_time")
    private Instant consultationStartTime;

    @Column(name = "consultation_end_time")
    private Instant consultationEndTime;

    @Column(name = "notes", length = 500)
    private String notes;

    public QueueEntry() {
    }

    public QueueEntry(String id,
                      String departmentId,
                      String doctorId,
                      String patientId,
                      String appointmentId,
                      LocalDate queueDate,
                      Integer tokenNumber,
                      String tokenDisplay,
                      QueuePriority priority,
                      Instant entryTime,
                      String notes) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.departmentId = Objects.requireNonNull(departmentId, "Department ID must not be null");
        this.doctorId = doctorId;
        this.patientId = Objects.requireNonNull(patientId, "Patient ID must not be null");
        this.appointmentId = appointmentId;
        this.queueDate = Objects.requireNonNull(queueDate, "Queue date must not be null");
        this.tokenNumber = Objects.requireNonNull(tokenNumber, "Token number must not be null");
        this.tokenDisplay = Objects.requireNonNull(tokenDisplay, "Token display must not be null");
        this.priority = priority != null ? priority : QueuePriority.NORMAL;
        this.status = QueueStatus.WAITING;
        this.entryTime = entryTime != null ? entryTime : Instant.now();
        this.notes = notes;
    }

    // Business Methods (§21, §69)

    public void transitionTo(QueueStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidQueueStatusTransitionException(this.id, this.status, newStatus);
        }
        this.status = newStatus;
    }

    public void call(String callingDoctorId) {
        transitionTo(QueueStatus.CALLED);
        this.calledTime = Instant.now();
        if (callingDoctorId != null && !callingDoctorId.isBlank()) {
            this.doctorId = callingDoctorId;
        }
    }

    public void startConsultation() {
        transitionTo(QueueStatus.IN_CONSULTATION);
        this.consultationStartTime = Instant.now();
    }

    public void complete() {
        transitionTo(QueueStatus.COMPLETED);
        this.consultationEndTime = Instant.now();
    }

    public void skip() {
        transitionTo(QueueStatus.SKIPPED);
    }

    public void requeue() {
        transitionTo(QueueStatus.WAITING);
    }

    public void cancel(String reason) {
        transitionTo(QueueStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + " | Cancelled: " : "Cancelled: ") + reason;
        }
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getTokenDisplay() {
        return tokenDisplay;
    }

    public void setTokenDisplay(String tokenDisplay) {
        this.tokenDisplay = tokenDisplay;
    }

    public QueuePriority getPriority() {
        return priority;
    }

    public void setPriority(QueuePriority priority) {
        this.priority = priority;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Instant entryTime) {
        this.entryTime = entryTime;
    }

    public Instant getCalledTime() {
        return calledTime;
    }

    public void setCalledTime(Instant calledTime) {
        this.calledTime = calledTime;
    }

    public Instant getConsultationStartTime() {
        return consultationStartTime;
    }

    public void setConsultationStartTime(Instant consultationStartTime) {
        this.consultationStartTime = consultationStartTime;
    }

    public Instant getConsultationEndTime() {
        return consultationEndTime;
    }

    public void setConsultationEndTime(Instant consultationEndTime) {
        this.consultationEndTime = consultationEndTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueEntry that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
