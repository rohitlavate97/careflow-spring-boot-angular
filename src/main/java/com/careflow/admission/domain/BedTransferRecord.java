package com.careflow.admission.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Audit record of bed reallocations and intra-hospital transfers during an admission (§28).
 */
@Entity
@Table(
        name = "bed_transfers",
        indexes = {
                @Index(name = "idx_bed_transfers_admission", columnList = "admission_id"),
                @Index(name = "idx_bed_transfers_to_bed", columnList = "to_bed_id"),
                @Index(name = "idx_bed_transfers_transferred_at", columnList = "transferred_at")
        }
)
public class BedTransferRecord extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_bed_id")
    private Bed fromBed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_bed_id", nullable = false)
    private Bed toBed;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    @Column(name = "transferred_by_id", length = 64, nullable = false)
    private String transferredById;

    protected BedTransferRecord() {
    }

    public BedTransferRecord(String id,
                             Admission admission,
                             Bed fromBed,
                             Bed toBed,
                             Instant transferredAt,
                             String transferReason,
                             String transferredById) {
        this.id = id;
        this.admission = admission;
        this.fromBed = fromBed;
        this.toBed = toBed;
        this.transferredAt = transferredAt != null ? transferredAt : Instant.now();
        this.transferReason = transferReason;
        this.transferredById = transferredById;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public Bed getFromBed() {
        return fromBed;
    }

    public void setFromBed(Bed fromBed) {
        this.fromBed = fromBed;
    }

    public Bed getToBed() {
        return toBed;
    }

    public void setToBed(Bed toBed) {
        this.toBed = toBed;
    }

    public Instant getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(Instant transferredAt) {
        this.transferredAt = transferredAt;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public String getTransferredById() {
        return transferredById;
    }

    public void setTransferredById(String transferredById) {
        this.transferredById = transferredById;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BedTransferRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
