package com.careflow.scheduling.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Doctor leave and absence schedule exception (§18).
 */
@Entity
@Table(name = "doctor_leaves")
public class DoctorLeave extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    public DoctorLeave() {
    }

    public DoctorLeave(String id,
                       String doctorId,
                       LocalDate startDate,
                       LocalDate endDate,
                       String reason) {
        this.id = id;
        this.doctorId = doctorId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason != null ? reason.trim() : null;
        this.status = LeaveStatus.PENDING;
    }

    // Business Methods (§18, §69)

    public boolean covers(LocalDate date) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
               (date.isEqual(endDate) || date.isBefore(endDate));
    }

    public boolean overlapsWith(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }

    public void approve() {
        transitionTo(LeaveStatus.APPROVED);
    }

    public void reject() {
        transitionTo(LeaveStatus.REJECTED);
    }

    public void cancel() {
        transitionTo(LeaveStatus.CANCELLED);
    }

    public void transitionTo(LeaveStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    String.format("Cannot transition leave request %s from status %s to %s",
                            this.id, this.status, target)
            );
        }
        this.status = target;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason != null ? reason.trim() : null;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorLeave that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
