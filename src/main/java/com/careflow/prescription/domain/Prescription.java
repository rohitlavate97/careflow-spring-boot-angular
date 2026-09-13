package com.careflow.prescription.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.prescription.exception.InvalidPrescriptionStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Prescription aggregate root managing physician medication orders and pharmacy fulfillment states (§24, §69, §103 Phase 8).
 */
@Entity
@Table(
        name = "prescriptions",
        indexes = {
                @Index(name = "idx_prescription_patient", columnList = "patient_id"),
                @Index(name = "idx_prescription_doctor", columnList = "doctor_id"),
                @Index(name = "idx_prescription_consultation", columnList = "consultation_id"),
                @Index(name = "idx_prescription_status", columnList = "status")
        }
)
public class Prescription extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "consultation_id", length = 64)
    private String consultationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.PENDING_DISPENSE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "prescribed_at", nullable = false)
    private Instant prescribedAt;

    @Column(name = "dispensed_at")
    private Instant dispensedAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PrescriptionItem> items = new LinkedHashSet<>();

    public Prescription() {
    }

    public Prescription(String id,
                        String patientId,
                        String doctorId,
                        String consultationId,
                        String notes,
                        Instant prescribedAt) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.consultationId = consultationId;
        this.notes = notes;
        this.prescribedAt = prescribedAt != null ? prescribedAt : Instant.now();
        this.status = PrescriptionStatus.PENDING_DISPENSE;
    }

    public void addItem(PrescriptionItem item) {
        assertModifiable();
        this.items.add(item);
        item.setPrescription(this);
    }

    public void removeItem(String itemId) {
        assertModifiable();
        this.items.removeIf(i -> Objects.equals(i.getId(), itemId));
    }

    public void transitionTo(PrescriptionStatus targetStatus) {
        if (this.status == targetStatus) {
            return;
        }

        if (this.status == PrescriptionStatus.DISPENSED || this.status == PrescriptionStatus.CANCELLED) {
            throw new InvalidPrescriptionStatusTransitionException(this.id, this.status, targetStatus);
        }

        switch (targetStatus) {
            case PARTIALLY_DISPENSED -> {
                if (this.status != PrescriptionStatus.PENDING_DISPENSE) {
                    throw new InvalidPrescriptionStatusTransitionException(this.id, this.status, targetStatus);
                }
                this.status = PrescriptionStatus.PARTIALLY_DISPENSED;
            }
            case DISPENSED -> {
                this.status = PrescriptionStatus.DISPENSED;
                this.dispensedAt = Instant.now();
            }
            case CANCELLED -> {
                this.status = PrescriptionStatus.CANCELLED;
            }
            default -> throw new InvalidPrescriptionStatusTransitionException(this.id, this.status, targetStatus);
        }
    }

    public void checkAndUpdateDispenseStatus() {
        if (items.isEmpty()) {
            return;
        }

        boolean allDispensed = items.stream().allMatch(i -> i.getStatus() == PrescriptionItemStatus.DISPENSED);
        boolean anyDispensed = items.stream().anyMatch(i -> i.getStatus() == PrescriptionItemStatus.DISPENSED);

        if (allDispensed) {
            this.status = PrescriptionStatus.DISPENSED;
            this.dispensedAt = Instant.now();
        } else if (anyDispensed) {
            this.status = PrescriptionStatus.PARTIALLY_DISPENSED;
        }
    }

    public void assertModifiable() {
        if (this.status != PrescriptionStatus.DRAFT && this.status != PrescriptionStatus.PENDING_DISPENSE) {
            throw new InvalidPrescriptionStatusTransitionException(
                    String.format("Prescription '%s' in status '%s' cannot be modified.", this.id, this.status)
            );
        }
    }

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

    public String getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(String consultationId) {
        this.consultationId = consultationId;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(Instant prescribedAt) {
        this.prescribedAt = prescribedAt;
    }

    public Instant getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(Instant dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public Set<PrescriptionItem> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public void setItems(Set<PrescriptionItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prescription that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
