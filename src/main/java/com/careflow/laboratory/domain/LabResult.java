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
 * Diagnostic lab result observation for an individual test parameter (§27).
 */
@Entity
@Table(
        name = "lab_results",
        indexes = {
                @Index(name = "idx_lab_results_item", columnList = "order_item_id"),
                @Index(name = "idx_lab_results_sample", columnList = "sample_id"),
                @Index(name = "idx_lab_results_flag", columnList = "abnormality_flag")
        }
)
public class LabResult extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private LabOrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id")
    private LabSample sample;

    @Column(name = "test_parameter", length = 100, nullable = false)
    private String testParameter;

    @Column(name = "result_value", length = 255, nullable = false)
    private String resultValue;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "abnormality_flag", length = 30, nullable = false)
    private AbnormalityFlag abnormalityFlag = AbnormalityFlag.NORMAL;

    @Column(name = "performed_by_id", length = 64, nullable = false)
    private String performedById;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    protected LabResult() {
    }

    public LabResult(String id,
                     LabOrderItem orderItem,
                     LabSample sample,
                     String testParameter,
                     String resultValue,
                     Double numericValue,
                     String unit,
                     String referenceRange,
                     AbnormalityFlag abnormalityFlag,
                     String performedById,
                     Instant performedAt,
                     String technicianNotes) {
        this.id = id;
        this.orderItem = orderItem;
        this.sample = sample;
        this.testParameter = testParameter;
        this.resultValue = resultValue;
        this.numericValue = numericValue;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.abnormalityFlag = abnormalityFlag != null ? abnormalityFlag : AbnormalityFlag.NORMAL;
        this.performedById = performedById;
        this.performedAt = performedAt != null ? performedAt : Instant.now();
        this.technicianNotes = technicianNotes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LabOrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(LabOrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public LabSample getSample() {
        return sample;
    }

    public void setSample(LabSample sample) {
        this.sample = sample;
    }

    public String getTestParameter() {
        return testParameter;
    }

    public void setTestParameter(String testParameter) {
        this.testParameter = testParameter;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public AbnormalityFlag getAbnormalityFlag() {
        return abnormalityFlag;
    }

    public void setAbnormalityFlag(AbnormalityFlag abnormalityFlag) {
        this.abnormalityFlag = abnormalityFlag;
    }

    public String getPerformedById() {
        return performedById;
    }

    public void setPerformedById(String performedById) {
        this.performedById = performedById;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public void setTechnicianNotes(String technicianNotes) {
        this.technicianNotes = technicianNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabResult result)) return false;
        return Objects.equals(id, result.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
