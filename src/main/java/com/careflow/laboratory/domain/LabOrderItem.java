package com.careflow.laboratory.domain;

import com.careflow.common.domain.BaseAuditEntity;
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Individual test request line inside a laboratory requisition (§27).
 */
@Entity
@Table(
        name = "lab_order_items",
        indexes = {
                @Index(name = "idx_lab_order_items_order", columnList = "lab_order_id"),
                @Index(name = "idx_lab_order_items_test", columnList = "lab_test_id"),
                @Index(name = "idx_lab_order_items_status", columnList = "status")
        }
)
public class LabOrderItem extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LabResult> results = new LinkedHashSet<>();

    protected LabOrderItem() {
    }

    public LabOrderItem(String id, LabTest labTest, String notes) {
        this.id = id;
        this.labTest = labTest;
        this.notes = notes;
        this.status = LabOrderStatus.ORDERED;
    }

    public void addResult(LabResult result) {
        this.results.add(result);
        result.setOrderItem(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LabOrder getLabOrder() {
        return labOrder;
    }

    public void setLabOrder(LabOrder labOrder) {
        this.labOrder = labOrder;
    }

    public LabTest getLabTest() {
        return labTest;
    }

    public void setLabTest(LabTest labTest) {
        this.labTest = labTest;
    }

    public LabOrderStatus getStatus() {
        return status;
    }

    public void setStatus(LabOrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<LabResult> getResults() {
        return Collections.unmodifiableSet(results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabOrderItem item)) return false;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
