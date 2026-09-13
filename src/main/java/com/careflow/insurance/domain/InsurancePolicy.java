package com.careflow.insurance.domain;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Patient health insurance policy contract and coverage terms (§33).
 */
@Entity
@Table(
        name = "insurance_policies",
        indexes = {
                @Index(name = "idx_insurance_policies_patient", columnList = "patient_id"),
                @Index(name = "idx_insurance_policies_provider", columnList = "provider_id"),
                @Index(name = "idx_insurance_policies_number", columnList = "policy_number"),
                @Index(name = "idx_insurance_policies_active", columnList = "active")
        }
)
public class InsurancePolicy extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "policy_number", length = 64, nullable = false)
    private String policyNumber;

    @Column(name = "group_number", length = 64)
    private String groupNumber;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private InsuranceProvider provider;

    @Column(name = "policy_holder_name", length = 150, nullable = false)
    private String policyHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", length = 50, nullable = false)
    private PolicyRelationship relationship = PolicyRelationship.SELF;

    @Column(name = "coverage_start_date", nullable = false)
    private LocalDate coverageStartDate;

    @Column(name = "coverage_end_date", nullable = false)
    private LocalDate coverageEndDate;

    @Column(name = "co_pay_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal coPayAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "coverage_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal coveragePercentage = BigDecimal.valueOf(80.00).setScale(2, RoundingMode.HALF_UP);

    @Column(name = "deductible", precision = 10, scale = 2, nullable = false)
    private BigDecimal deductible = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected InsurancePolicy() {
    }

    public InsurancePolicy(String id,
                           String policyNumber,
                           String groupNumber,
                           String patientId,
                           InsuranceProvider provider,
                           String policyHolderName,
                           PolicyRelationship relationship,
                           LocalDate coverageStartDate,
                           LocalDate coverageEndDate,
                           BigDecimal coPayAmount,
                           BigDecimal coveragePercentage,
                           BigDecimal deductible,
                           boolean active) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.groupNumber = groupNumber;
        this.patientId = patientId;
        this.provider = provider;
        this.policyHolderName = policyHolderName;
        this.relationship = relationship != null ? relationship : PolicyRelationship.SELF;
        this.coverageStartDate = coverageStartDate;
        this.coverageEndDate = coverageEndDate;
        this.coPayAmount = coPayAmount != null ? coPayAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.coveragePercentage = coveragePercentage != null ? coveragePercentage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(80.00).setScale(2, RoundingMode.HALF_UP);
        this.deductible = deductible != null ? deductible.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.active = active;
    }

    /**
     * Verifies whether the policy provides active coverage on a specified date of service (§33).
     */
    public boolean isCoveredOn(LocalDate serviceDate) {
        if (!this.active || serviceDate == null) {
            return false;
        }
        return !serviceDate.isBefore(this.coverageStartDate) && !serviceDate.isAfter(this.coverageEndDate);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public InsuranceProvider getProvider() {
        return provider;
    }

    public void setProvider(InsuranceProvider provider) {
        this.provider = provider;
    }

    public String getPolicyHolderName() {
        return policyHolderName;
    }

    public void setPolicyHolderName(String policyHolderName) {
        this.policyHolderName = policyHolderName;
    }

    public PolicyRelationship getRelationship() {
        return relationship;
    }

    public void setRelationship(PolicyRelationship relationship) {
        this.relationship = relationship;
    }

    public LocalDate getCoverageStartDate() {
        return coverageStartDate;
    }

    public void setCoverageStartDate(LocalDate coverageStartDate) {
        this.coverageStartDate = coverageStartDate;
    }

    public LocalDate getCoverageEndDate() {
        return coverageEndDate;
    }

    public void setCoverageEndDate(LocalDate coverageEndDate) {
        this.coverageEndDate = coverageEndDate;
    }

    public BigDecimal getCoPayAmount() {
        return coPayAmount;
    }

    public void setCoPayAmount(BigDecimal coPayAmount) {
        this.coPayAmount = coPayAmount != null ? coPayAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(BigDecimal coveragePercentage) {
        this.coveragePercentage = coveragePercentage != null ? coveragePercentage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(80.00).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible != null ? deductible.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsurancePolicy that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
