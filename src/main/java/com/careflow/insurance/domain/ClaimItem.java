package com.careflow.insurance.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Service line item for an insurance claim breakdown (§33).
 */
@Entity
@Table(
        name = "claim_items",
        indexes = {
                @Index(name = "idx_claim_items_claim", columnList = "claim_id"),
                @Index(name = "idx_claim_items_service", columnList = "service_code")
        }
)
public class ClaimItem extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private InsuranceClaim claim;

    @Column(name = "invoice_item_id", length = 64)
    private String invoiceItemId;

    @Column(name = "service_code", length = 50, nullable = false)
    private String serviceCode;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "claimed_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal claimedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "approved_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal approvedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    protected ClaimItem() {
    }

    public ClaimItem(String id,
                     InsuranceClaim claim,
                     String invoiceItemId,
                     String serviceCode,
                     String description,
                     BigDecimal claimedAmount) {
        this.id = id;
        this.claim = claim;
        this.invoiceItemId = invoiceItemId;
        this.serviceCode = serviceCode;
        this.description = description;
        this.claimedAmount = claimedAmount != null ? claimedAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.approvedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InsuranceClaim getClaim() {
        return claim;
    }

    public void setClaim(InsuranceClaim claim) {
        this.claim = claim;
    }

    public String getInvoiceItemId() {
        return invoiceItemId;
    }

    public void setInvoiceItemId(String invoiceItemId) {
        this.invoiceItemId = invoiceItemId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(BigDecimal claimedAmount) {
        this.claimedAmount = claimedAmount != null ? claimedAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount != null ? approvedAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
        if (!(o instanceof ClaimItem claimItem)) return false;
        return Objects.equals(id, claimItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
