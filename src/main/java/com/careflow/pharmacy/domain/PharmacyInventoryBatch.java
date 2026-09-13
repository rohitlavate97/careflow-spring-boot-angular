package com.careflow.pharmacy.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.pharmacy.exception.ExpiredMedicationException;
import com.careflow.pharmacy.exception.InsufficientInventoryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Physical pharmaceutical stock batch entity tracking expiration and available quantities (§25, §26).
 */
@Entity
@Table(
        name = "pharmacy_inventory_batches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_batch_medication_number", columnNames = {"medication_id", "batch_number"})
        },
        indexes = {
                @Index(name = "idx_inventory_med_expiry", columnList = "medication_id, expiry_date"),
                @Index(name = "idx_inventory_batch_num", columnList = "batch_number")
        }
)
public class PharmacyInventoryBatch extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "medication_id", length = 64, nullable = false)
    private String medicationId;

    @Column(name = "batch_number", length = 64, nullable = false)
    private String batchNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold = 10;

    public PharmacyInventoryBatch() {
    }

    public PharmacyInventoryBatch(String id,
                                  String medicationId,
                                  String batchNumber,
                                  LocalDate expiryDate,
                                  Integer quantityAvailable,
                                  Integer reorderThreshold) {
        this.id = id;
        this.medicationId = medicationId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantityAvailable = quantityAvailable != null ? quantityAvailable : 0;
        this.reorderThreshold = reorderThreshold != null ? reorderThreshold : 10;
    }

    public void decrementStock(int quantity) {
        if (isExpired()) {
            throw new ExpiredMedicationException(this.batchNumber, this.expiryDate);
        }
        if (this.quantityAvailable < quantity) {
            throw new InsufficientInventoryException(this.id, quantity, this.quantityAvailable);
        }
        this.quantityAvailable -= quantity;
    }

    public boolean isExpired() {
        return this.expiryDate != null && this.expiryDate.isBefore(LocalDate.now());
    }

    public boolean isLowStock() {
        return this.quantityAvailable <= this.reorderThreshold;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PharmacyInventoryBatch that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
