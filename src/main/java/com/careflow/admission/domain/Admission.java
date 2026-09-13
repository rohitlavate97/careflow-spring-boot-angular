package com.careflow.admission.domain;

import com.careflow.admission.exception.InvalidAdmissionStatusTransitionException;
import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Inpatient admission aggregate root managing hospitalizations, transfers, and discharges (§28, §57 Lab 4).
 */
@Entity
@Table(
        name = "admissions",
        indexes = {
                @Index(name = "idx_admissions_patient", columnList = "patient_id"),
                @Index(name = "idx_admissions_doctor", columnList = "admitting_doctor_id"),
                @Index(name = "idx_admissions_bed", columnList = "current_bed_id"),
                @Index(name = "idx_admissions_status", columnList = "status"),
                @Index(name = "idx_admissions_admitted_at", columnList = "admitted_at")
        }
)
public class Admission extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "admission_number", length = 64, nullable = false, unique = true)
    private String admissionNumber;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "admitting_doctor_id", length = 64, nullable = false)
    private String admittingDoctorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bed_id")
    private Bed currentBed;

    @Column(name = "encounter_id", length = 64)
    private String encounterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    @Column(name = "admission_reason", columnDefinition = "TEXT", nullable = false)
    private String admissionReason;

    @Column(name = "admitting_diagnosis", columnDefinition = "TEXT")
    private String admittingDiagnosis;

    @Column(name = "admitted_at", nullable = false)
    private Instant admittedAt;

    @Column(name = "discharged_at")
    private Instant dischargedAt;

    @Column(name = "discharge_summary", columnDefinition = "TEXT")
    private String dischargeSummary;

    @OneToMany(mappedBy = "admission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<BedTransferRecord> transfers = new LinkedHashSet<>();

    protected Admission() {
    }

    public Admission(String id,
                     String admissionNumber,
                     String patientId,
                     String admittingDoctorId,
                     Bed currentBed,
                     String encounterId,
                     String admissionReason,
                     String admittingDiagnosis,
                     Instant admittedAt) {
        this.id = id;
        this.admissionNumber = admissionNumber;
        this.patientId = patientId;
        this.admittingDoctorId = admittingDoctorId;
        this.currentBed = currentBed;
        this.encounterId = encounterId;
        this.admissionReason = admissionReason;
        this.admittingDiagnosis = admittingDiagnosis;
        this.admittedAt = admittedAt != null ? admittedAt : Instant.now();
        this.status = AdmissionStatus.ADMITTED;
    }

    public void transferTo(Bed newBed, String reason, String transferredById) {
        if (this.status != AdmissionStatus.ADMITTED && this.status != AdmissionStatus.TRANSFERRED) {
            throw new InvalidAdmissionStatusTransitionException(
                    "Cannot transfer patient from admission in status: " + this.status);
        }
        Bed previousBed = this.currentBed;
        this.currentBed = newBed;
        this.status = AdmissionStatus.TRANSFERRED;

        BedTransferRecord transferRecord = new BedTransferRecord(
                java.util.UUID.randomUUID().toString(),
                this,
                previousBed,
                newBed,
                Instant.now(),
                reason,
                transferredById
        );
        this.transfers.add(transferRecord);
    }

    public void discharge(String summary) {
        if (this.status != AdmissionStatus.ADMITTED && this.status != AdmissionStatus.TRANSFERRED) {
            throw new InvalidAdmissionStatusTransitionException(
                    "Cannot discharge patient from admission in status: " + this.status);
        }
        this.status = AdmissionStatus.DISCHARGED;
        this.dischargedAt = Instant.now();
        this.dischargeSummary = summary;
        this.currentBed = null;
    }

    public void cancel(String reason) {
        if (this.status == AdmissionStatus.DISCHARGED) {
            throw new InvalidAdmissionStatusTransitionException("Cannot cancel an already discharged admission.");
        }
        this.status = AdmissionStatus.CANCELLED;
        this.dischargeSummary = "Cancelled: " + reason;
        this.currentBed = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getAdmittingDoctorId() {
        return admittingDoctorId;
    }

    public void setAdmittingDoctorId(String admittingDoctorId) {
        this.admittingDoctorId = admittingDoctorId;
    }

    public Bed getCurrentBed() {
        return currentBed;
    }

    public void setCurrentBed(Bed currentBed) {
        this.currentBed = currentBed;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionStatus status) {
        this.status = status;
    }

    public String getAdmissionReason() {
        return admissionReason;
    }

    public void setAdmissionReason(String admissionReason) {
        this.admissionReason = admissionReason;
    }

    public String getAdmittingDiagnosis() {
        return admittingDiagnosis;
    }

    public void setAdmittingDiagnosis(String admittingDiagnosis) {
        this.admittingDiagnosis = admittingDiagnosis;
    }

    public Instant getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public Instant getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(Instant dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public String getDischargeSummary() {
        return dischargeSummary;
    }

    public void setDischargeSummary(String dischargeSummary) {
        this.dischargeSummary = dischargeSummary;
    }

    public Set<BedTransferRecord> getTransfers() {
        return Collections.unmodifiableSet(transfers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Admission admission)) return false;
        return Objects.equals(id, admission.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
