package com.careflow.laboratory.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Diagnostic lab test master catalog entity (§27, §101).
 */
@Entity
@Table(
        name = "lab_tests",
        indexes = {
                @Index(name = "idx_lab_tests_category", columnList = "category"),
                @Index(name = "idx_lab_tests_active", columnList = "active")
        }
)
public class LabTest extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private LabTestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "specimen_type", length = 50, nullable = false)
    private SpecimenType specimenType;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "turnaround_hours", nullable = false)
    private Integer turnaroundHours = 24;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected LabTest() {
    }

    public LabTest(String id,
                   String code,
                   String name,
                   LabTestCategory category,
                   SpecimenType specimenType,
                   String referenceRange,
                   String unit,
                   Integer turnaroundHours,
                   BigDecimal price,
                   boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.specimenType = specimenType;
        this.referenceRange = referenceRange;
        this.unit = unit;
        this.turnaroundHours = turnaroundHours;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LabTestCategory getCategory() {
        return category;
    }

    public void setCategory(LabTestCategory category) {
        this.category = category;
    }

    public SpecimenType getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(SpecimenType specimenType) {
        this.specimenType = specimenType;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getTurnaroundHours() {
        return turnaroundHours;
    }

    public void setTurnaroundHours(Integer turnaroundHours) {
        this.turnaroundHours = turnaroundHours;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
        if (!(o instanceof LabTest labTest)) return false;
        return Objects.equals(id, labTest.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
