package com.careflow.laboratory.domain;

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

import java.time.Instant;
import java.util.Objects;

/**
 * Accessioned biological specimen entity for a diagnostic lab order (§27).
 */
@Entity
@Table(
        name = "lab_samples",
        indexes = {
                @Index(name = "idx_lab_samples_order", columnList = "lab_order_id"),
                @Index(name = "idx_lab_samples_status", columnList = "status")
        }
)
public class LabSample extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "sample_barcode", length = 64, nullable = false, unique = true)
    private String sampleBarcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "specimen_type", length = 50, nullable = false)
    private SpecimenType specimenType;

    @Column(name = "collected_by_id", length = 64, nullable = false)
    private String collectedById;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "condition_notes", length = 500)
    private String conditionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private SampleStatus status = SampleStatus.COLLECTED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    protected LabSample() {
    }

    public LabSample(String id,
                     String sampleBarcode,
                     SpecimenType specimenType,
                     String collectedById,
                     Instant collectedAt,
                     String conditionNotes) {
        this.id = id;
        this.sampleBarcode = sampleBarcode;
        this.specimenType = specimenType;
        this.collectedById = collectedById;
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
        this.conditionNotes = conditionNotes;
        this.status = SampleStatus.COLLECTED;
    }

    public void accept() {
        this.status = SampleStatus.ACCEPTED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.status = SampleStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSampleBarcode() {
        return sampleBarcode;
    }

    public void setSampleBarcode(String sampleBarcode) {
        this.sampleBarcode = sampleBarcode;
    }

    public LabOrder getLabOrder() {
        return labOrder;
    }

    public void setLabOrder(LabOrder labOrder) {
        this.labOrder = labOrder;
    }

    public SpecimenType getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(SpecimenType specimenType) {
        this.specimenType = specimenType;
    }

    public String getCollectedById() {
        return collectedById;
    }

    public void setCollectedById(String collectedById) {
        this.collectedById = collectedById;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public SampleStatus getStatus() {
        return status;
    }

    public void setStatus(SampleStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabSample sample)) return false;
        return Objects.equals(id, sample.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
