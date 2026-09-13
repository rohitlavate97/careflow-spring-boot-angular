package com.careflow.pharmacy.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable audit record representing medication fulfillment by a pharmacist (§25).
 */
@Entity
@Table(
        name = "dispense_records",
        indexes = {
                @Index(name = "idx_dispense_prescription", columnList = "prescription_id"),
                @Index(name = "idx_dispense_item", columnList = "prescription_item_id"),
                @Index(name = "idx_dispense_batch", columnList = "inventory_batch_id"),
                @Index(name = "idx_dispense_pharmacist", columnList = "pharmacist_id")
        }
)
public class DispenseRecord extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "prescription_id", length = 64, nullable = false)
    private String prescriptionId;

    @Column(name = "prescription_item_id", length = 64, nullable = false)
    private String prescriptionItemId;

    @Column(name = "inventory_batch_id", length = 64, nullable = false)
    private String inventoryBatchId;

    @Column(name = "pharmacist_id", length = 64, nullable = false)
    private String pharmacistId;

    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed;

    @Column(name = "dispensed_at", nullable = false)
    private Instant dispensedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public DispenseRecord() {
    }

    public DispenseRecord(String id,
                          String prescriptionId,
                          String prescriptionItemId,
                          String inventoryBatchId,
                          String pharmacistId,
                          Integer quantityDispensed,
                          Instant dispensedAt,
                          String notes) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.prescriptionItemId = prescriptionItemId;
        this.inventoryBatchId = inventoryBatchId;
        this.pharmacistId = pharmacistId;
        this.quantityDispensed = quantityDispensed;
        this.dispensedAt = dispensedAt != null ? dispensedAt : Instant.now();
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPrescriptionItemId() {
        return prescriptionItemId;
    }

    public void setPrescriptionItemId(String prescriptionItemId) {
        this.prescriptionItemId = prescriptionItemId;
    }

    public String getInventoryBatchId() {
        return inventoryBatchId;
    }

    public void setInventoryBatchId(String inventoryBatchId) {
        this.inventoryBatchId = inventoryBatchId;
    }

    public String getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(String pharmacistId) {
        this.pharmacistId = pharmacistId;
    }

    public Integer getQuantityDispensed() {
        return quantityDispensed;
    }

    public void setQuantityDispensed(Integer quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
    }

    public Instant getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(Instant dispensedAt) {
        this.dispensedAt = dispensedAt;
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
        if (!(o instanceof DispenseRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
