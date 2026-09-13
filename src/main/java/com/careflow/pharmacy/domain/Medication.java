package com.careflow.pharmacy.domain;

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
 * Formulary medication master catalog entity (§24, §25).
 */
@Entity
@Table(
        name = "medications",
        indexes = {
                @Index(name = "idx_medication_code", columnList = "code"),
                @Index(name = "idx_medication_name", columnList = "name"),
                @Index(name = "idx_medication_status", columnList = "status")
        }
)
public class Medication extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "generic_name", length = 200, nullable = false)
    private String genericName;

    @Enumerated(EnumType.STRING)
    @Column(name = "form", length = 30, nullable = false)
    private MedicationForm form;

    @Column(name = "strength", length = 50, nullable = false)
    private String strength;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MedicationStatus status = MedicationStatus.ACTIVE;

    public Medication() {
    }

    public Medication(String id,
                      String code,
                      String name,
                      String genericName,
                      MedicationForm form,
                      String strength,
                      BigDecimal unitPrice,
                      Integer reorderThreshold) {
        this.id = id;
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.name = name != null ? name.trim() : null;
        this.genericName = genericName != null ? genericName.trim() : null;
        this.form = form;
        this.strength = strength != null ? strength.trim() : null;
        this.unitPrice = unitPrice;
        this.reorderThreshold = reorderThreshold != null ? reorderThreshold : 10;
        this.status = MedicationStatus.ACTIVE;
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

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public MedicationForm getForm() {
        return form;
    }

    public void setForm(MedicationForm form) {
        this.form = form;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public MedicationStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medication that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
