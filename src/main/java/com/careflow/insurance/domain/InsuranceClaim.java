package com.careflow.insurance.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.insurance.exception.InvalidClaimStatusTransitionException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Insurance reimbursement claim aggregate root tracking adjudication and settlement (§33).
 */
@Entity
@Table(
        name = "insurance_claims",
        indexes = {
                @Index(name = "idx_insurance_claims_policy", columnList = "policy_id"),
                @Index(name = "idx_insurance_claims_patient", columnList = "patient_id"),
                @Index(name = "idx_insurance_claims_invoice", columnList = "invoice_id"),
                @Index(name = "idx_insurance_claims_status", columnList = "status"),
                @Index(name = "idx_insurance_claims_created_at", columnList = "created_at")
        }
)
public class InsuranceClaim extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "claim_number", length = 64, nullable = false, unique = true)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "invoice_id", length = 64)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(name = "total_claimed_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalClaimedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "approved_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal approvedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "patient_responsibility", precision = 12, scale = 2, nullable = false)
    private BigDecimal patientResponsibility = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;

    @Column(name = "adjudication_notes", columnDefinition = "TEXT")
    private String adjudicationNotes;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "adjudicated_at")
    private Instant adjudicatedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClaimItem> items = new ArrayList<>();

    protected InsuranceClaim() {
    }

    public InsuranceClaim(String id,
                          String claimNumber,
                          InsurancePolicy policy,
                          String patientId,
                          String invoiceId) {
        this.id = id;
        this.claimNumber = claimNumber;
        this.policy = policy;
        this.patientId = patientId;
        this.invoiceId = invoiceId;
        this.status = ClaimStatus.DRAFT;
        this.totalClaimedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.approvedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.patientResponsibility = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void addItem(ClaimItem item) {
        if (this.status != ClaimStatus.DRAFT) {
            throw new InvalidClaimStatusTransitionException("Cannot add items to claim in status: " + this.status);
        }
        item.setClaim(this);
        this.items.add(item);
        recalculateClaimedAmount();
    }

    public void removeItem(String itemId) {
        if (this.status != ClaimStatus.DRAFT) {
            throw new InvalidClaimStatusTransitionException("Cannot remove items from claim in status: " + this.status);
        }
        this.items.removeIf(item -> Objects.equals(item.getId(), itemId));
        recalculateClaimedAmount();
    }

    public void recalculateClaimedAmount() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (ClaimItem item : this.items) {
            if (item.getClaimedAmount() != null) {
                sum = sum.add(item.getClaimedAmount());
            }
        }
        this.totalClaimedAmount = sum.setScale(2, RoundingMode.HALF_UP);
    }

    public void submit() {
        if (this.status != ClaimStatus.DRAFT) {
            throw new InvalidClaimStatusTransitionException(
                    "Only DRAFT claims can be submitted. Current status: " + this.status);
        }
        if (this.items.isEmpty()) {
            throw new BusinessRuleException("CANNOT_SUBMIT_EMPTY_CLAIM", "Cannot submit a claim with no service items.");
        }
        if (this.totalClaimedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_CLAIM_AMOUNT", "Total claimed amount must be greater than zero.");
        }
        this.status = ClaimStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void startReview() {
        if (this.status != ClaimStatus.SUBMITTED) {
            throw new InvalidClaimStatusTransitionException(
                    "Only SUBMITTED claims can enter UNDER_REVIEW. Current status: " + this.status);
        }
        this.status = ClaimStatus.UNDER_REVIEW;
    }

    public void adjudicate(ClaimStatus decision, BigDecimal approvedAmount, String notes, String denialReason) {
        if (this.status != ClaimStatus.SUBMITTED && this.status != ClaimStatus.UNDER_REVIEW) {
            throw new InvalidClaimStatusTransitionException(
                    "Claims can only be adjudicated from SUBMITTED or UNDER_REVIEW status. Current status: " + this.status);
        }

        if (decision == ClaimStatus.APPROVED) {
            this.approvedAmount = this.totalClaimedAmount;
            this.patientResponsibility = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.status = ClaimStatus.APPROVED;
        } else if (decision == ClaimStatus.PARTIALLY_APPROVED) {
            if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_APPROVED_AMOUNT", "Partially approved amount must be greater than zero.");
            }
            if (approvedAmount.compareTo(this.totalClaimedAmount) >= 0) {
                throw new BusinessRuleException("INVALID_APPROVED_AMOUNT", "Partially approved amount must be less than total claimed amount.");
            }
            this.approvedAmount = approvedAmount.setScale(2, RoundingMode.HALF_UP);
            this.patientResponsibility = this.totalClaimedAmount.subtract(this.approvedAmount).setScale(2, RoundingMode.HALF_UP);
            this.status = ClaimStatus.PARTIALLY_APPROVED;
        } else if (decision == ClaimStatus.REJECTED) {
            if (denialReason == null || denialReason.isBlank()) {
                throw new BusinessRuleException("DENIAL_REASON_REQUIRED", "Denial reason is required for rejected claims.");
            }
            this.approvedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.patientResponsibility = this.totalClaimedAmount;
            this.denialReason = denialReason;
            this.status = ClaimStatus.REJECTED;
        } else {
            throw new InvalidClaimStatusTransitionException("Unsupported adjudication decision: " + decision);
        }

        this.adjudicationNotes = notes;
        this.adjudicatedAt = Instant.now();
    }

    public void settle() {
        if (this.status != ClaimStatus.APPROVED && this.status != ClaimStatus.PARTIALLY_APPROVED) {
            throw new InvalidClaimStatusTransitionException(
                    "Only APPROVED or PARTIALLY_APPROVED claims can be settled. Current status: " + this.status);
        }
        this.status = ClaimStatus.SETTLED;
        this.settledAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public InsurancePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(InsurancePolicy policy) {
        this.policy = policy;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalClaimedAmount() {
        return totalClaimedAmount;
    }

    public void setTotalClaimedAmount(BigDecimal totalClaimedAmount) {
        this.totalClaimedAmount = totalClaimedAmount != null ? totalClaimedAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount != null ? approvedAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPatientResponsibility() {
        return patientResponsibility;
    }

    public void setPatientResponsibility(BigDecimal patientResponsibility) {
        this.patientResponsibility = patientResponsibility != null ? patientResponsibility.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public String getAdjudicationNotes() {
        return adjudicationNotes;
    }

    public void setAdjudicationNotes(String adjudicationNotes) {
        this.adjudicationNotes = adjudicationNotes;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getAdjudicatedAt() {
        return adjudicatedAt;
    }

    public void setAdjudicatedAt(Instant adjudicatedAt) {
        this.adjudicatedAt = adjudicatedAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }

    public List<ClaimItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsuranceClaim that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
