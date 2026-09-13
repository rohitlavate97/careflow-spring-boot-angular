package com.careflow.prescription.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Individual medication order item within a prescription (§24).
 */
@Entity
@Table(
        name = "prescription_items",
        indexes = {
                @Index(name = "idx_prescription_item_prescription", columnList = "prescription_id"),
                @Index(name = "idx_prescription_item_medication", columnList = "medication_id")
        }
)
public class PrescriptionItem extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medication_id", length = 64, nullable = false)
    private String medicationId;

    @Column(name = "dosage", length = 100, nullable = false)
    private String dosage;

    @Column(name = "frequency", length = 100, nullable = false)
    private String frequency;

    @Column(name = "duration", length = 100, nullable = false)
    private String duration;

    @Column(name = "quantity_prescribed", nullable = false)
    private Integer quantityPrescribed;

    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed = 0;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PrescriptionItemStatus status = PrescriptionItemStatus.PENDING;

    public PrescriptionItem() {
    }

    public PrescriptionItem(String id,
                            Prescription prescription,
                            String medicationId,
                            String dosage,
                            String frequency,
                            String duration,
                            Integer quantityPrescribed,
                            String instructions) {
        this.id = id;
        this.prescription = prescription;
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.quantityPrescribed = quantityPrescribed;
        this.quantityDispensed = 0;
        this.instructions = instructions;
        this.status = PrescriptionItemStatus.PENDING;
    }

    public void recordDispense(int quantity) {
        this.quantityDispensed += quantity;
        if (this.quantityDispensed >= this.quantityPrescribed) {
            this.status = PrescriptionItemStatus.DISPENSED;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getQuantityPrescribed() {
        return quantityPrescribed;
    }

    public void setQuantityPrescribed(Integer quantityPrescribed) {
        this.quantityPrescribed = quantityPrescribed;
    }

    public Integer getQuantityDispensed() {
        return quantityDispensed;
    }

    public void setQuantityDispensed(Integer quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public PrescriptionItemStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionItemStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
