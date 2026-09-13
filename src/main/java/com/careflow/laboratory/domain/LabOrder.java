package com.careflow.laboratory.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.laboratory.exception.InvalidLabOrderStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Diagnostic laboratory requisition aggregate root managing order lifecycle,
 * specimen accessioning, testing, and physician sign-off (§27, §103 Phase 9).
 */
@Entity
@Table(
        name = "lab_orders",
        indexes = {
                @Index(name = "idx_lab_orders_patient", columnList = "patient_id"),
                @Index(name = "idx_lab_orders_doctor", columnList = "ordering_doctor_id"),
                @Index(name = "idx_lab_orders_encounter", columnList = "encounter_id"),
                @Index(name = "idx_lab_orders_status", columnList = "status"),
                @Index(name = "idx_lab_orders_review_status", columnList = "review_status"),
                @Index(name = "idx_lab_orders_ordered_at", columnList = "ordered_at")
        }
)
public class LabOrder extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "order_number", length = 64, nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "ordering_doctor_id", length = 64, nullable = false)
    private String orderingDoctorId;

    @Column(name = "encounter_id", length = 64)
    private String encounterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private LabOrderPriority priority = LabOrderPriority.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 30, nullable = false)
    private LabReviewStatus reviewStatus = LabReviewStatus.PENDING_REVIEW;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "reviewed_by_id", length = 64)
    private String reviewedById;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LabOrderItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LabSample> samples = new LinkedHashSet<>();

    protected LabOrder() {
    }

    public LabOrder(String id,
                    String orderNumber,
                    String patientId,
                    String orderingDoctorId,
                    String encounterId,
                    LabOrderPriority priority,
                    String clinicalNotes,
                    Instant orderedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.patientId = patientId;
        this.orderingDoctorId = orderingDoctorId;
        this.encounterId = encounterId;
        this.priority = priority != null ? priority : LabOrderPriority.ROUTINE;
        this.status = LabOrderStatus.ORDERED;
        this.reviewStatus = LabReviewStatus.PENDING_REVIEW;
        this.clinicalNotes = clinicalNotes;
        this.orderedAt = orderedAt != null ? orderedAt : Instant.now();
    }

    public void addItem(LabOrderItem item) {
        if (this.status != LabOrderStatus.ORDERED) {
            throw new InvalidLabOrderStatusTransitionException(
                    "Cannot add items to lab order in status: " + this.status);
        }
        this.items.add(item);
        item.setLabOrder(this);
    }

    public void addSample(LabSample sample) {
        if (this.status == LabOrderStatus.CANCELLED || this.status == LabOrderStatus.COMPLETED) {
            throw new InvalidLabOrderStatusTransitionException(
                    "Cannot collect specimen for lab order in status: " + this.status);
        }
        this.samples.add(sample);
        sample.setLabOrder(this);
        if (this.status == LabOrderStatus.ORDERED) {
            this.status = LabOrderStatus.SAMPLE_COLLECTED;
            for (LabOrderItem item : this.items) {
                if (item.getStatus() == LabOrderStatus.ORDERED) {
                    item.setStatus(LabOrderStatus.SAMPLE_COLLECTED);
                }
            }
        }
    }

    public void startProcessing() {
        if (this.status != LabOrderStatus.SAMPLE_COLLECTED && this.status != LabOrderStatus.ORDERED) {
            throw new InvalidLabOrderStatusTransitionException(
                    "Cannot transition order to PROCESSING from status: " + this.status);
        }
        this.status = LabOrderStatus.PROCESSING;
        for (LabOrderItem item : this.items) {
            if (item.getStatus() == LabOrderStatus.ORDERED || item.getStatus() == LabOrderStatus.SAMPLE_COLLECTED) {
                item.setStatus(LabOrderStatus.PROCESSING);
            }
        }
    }

    public void complete() {
        if (this.status == LabOrderStatus.CANCELLED) {
            throw new InvalidLabOrderStatusTransitionException("Cannot complete a cancelled lab order.");
        }
        if (this.status == LabOrderStatus.COMPLETED) {
            return; // idempotent
        }
        this.status = LabOrderStatus.COMPLETED;
        this.completedAt = Instant.now();
        for (LabOrderItem item : this.items) {
            if (item.getStatus() != LabOrderStatus.CANCELLED) {
                item.setStatus(LabOrderStatus.COMPLETED);
            }
        }
    }

    public void review(String reviewerDoctorId, LabReviewStatus targetReviewStatus, String notes) {
        if (this.status != LabOrderStatus.COMPLETED) {
            throw new InvalidLabOrderStatusTransitionException(
                    "Cannot review lab order before it is completed. Current status: " + this.status);
        }
        this.reviewedById = reviewerDoctorId;
        this.reviewedAt = Instant.now();
        this.reviewStatus = targetReviewStatus != null ? targetReviewStatus : LabReviewStatus.REVIEWED;
        this.reviewNotes = notes;
    }

    public void cancel(String reason) {
        if (this.status == LabOrderStatus.COMPLETED) {
            throw new InvalidLabOrderStatusTransitionException("Cannot cancel a completed lab order.");
        }
        this.status = LabOrderStatus.CANCELLED;
        this.cancellationReason = reason;
        for (LabOrderItem item : this.items) {
            item.setStatus(LabOrderStatus.CANCELLED);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getOrderingDoctorId() {
        return orderingDoctorId;
    }

    public void setOrderingDoctorId(String orderingDoctorId) {
        this.orderingDoctorId = orderingDoctorId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public LabOrderPriority getPriority() {
        return priority;
    }

    public void setPriority(LabOrderPriority priority) {
        this.priority = priority;
    }

    public LabOrderStatus getStatus() {
        return status;
    }

    public void setStatus(LabOrderStatus status) {
        this.status = status;
    }

    public LabReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(LabReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(Instant orderedAt) {
        this.orderedAt = orderedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(String reviewedById) {
        this.reviewedById = reviewedById;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Set<LabOrderItem> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public Set<LabSample> getSamples() {
        return Collections.unmodifiableSet(samples);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabOrder labOrder)) return false;
        return Objects.equals(id, labOrder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
