package com.careflow.billing.domain;

import com.careflow.billing.exception.InvalidInvoiceStatusTransitionException;
import com.careflow.billing.exception.OverpaymentException;
import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.common.exception.BusinessRuleException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Invoice aggregate root managing clinical charge itemization, taxes, discounts,
 * lifecycle state progression, and payment balance tracking (§29, §30, §31, §57 Lab 5).
 */
@Entity
@Table(
        name = "invoices",
        indexes = {
                @Index(name = "idx_invoices_patient", columnList = "patient_id"),
                @Index(name = "idx_invoices_encounter", columnList = "encounter_id"),
                @Index(name = "idx_invoices_admission", columnList = "admission_id"),
                @Index(name = "idx_invoices_status", columnList = "status"),
                @Index(name = "idx_invoices_created_at", columnList = "created_at")
        }
)
public class Invoice extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "invoice_number", length = 64, nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "encounter_id", length = 64)
    private String encounterId;

    @Column(name = "admission_id", length = 64)
    private String admissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "USD";

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "tax_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "paid_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "balance_due", precision = 12, scale = 2, nullable = false)
    private BigDecimal balanceDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(String id,
                   String invoiceNumber,
                   String patientId,
                   String encounterId,
                   String admissionId,
                   String currency,
                   String notes) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.patientId = patientId;
        this.encounterId = encounterId;
        this.admissionId = admissionId;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "USD";
        this.notes = notes;
        this.status = InvoiceStatus.DRAFT;
        this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.balanceDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void addItem(InvoiceItem item) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStatusTransitionException(
                    "Cannot add items to invoice in status: " + this.status);
        }
        item.setInvoice(this);
        this.items.add(item);
        recalculateTotals();
    }

    public void removeItem(String itemId) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStatusTransitionException(
                    "Cannot remove items from invoice in status: " + this.status);
        }
        this.items.removeIf(item -> Objects.equals(item.getId(), itemId));
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (InvoiceItem item : this.items) {
            if (item.getTotalPrice() != null) {
                sum = sum.add(item.getTotalPrice());
            }
        }
        this.subtotal = sum.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedTotal = this.subtotal.subtract(discount).add(tax).setScale(2, RoundingMode.HALF_UP);
        if (calculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            calculatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        this.totalAmount = calculatedTotal;

        BigDecimal paid = this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal calculatedBalance = this.totalAmount.subtract(paid).setScale(2, RoundingMode.HALF_UP);
        if (calculatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            calculatedBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        this.balanceDue = calculatedBalance;
    }

    public void issue(LocalDate dueDate, BigDecimal discount, BigDecimal tax) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStatusTransitionException(
                    "Only DRAFT invoices can be issued. Current status: " + this.status);
        }
        if (this.items.isEmpty()) {
            throw new BusinessRuleException("CANNOT_ISSUE_EMPTY_INVOICE",
                    "Cannot issue an invoice with no line items.");
        }
        if (discount != null) {
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_DISCOUNT", "Discount amount cannot be negative.");
            }
            this.discountAmount = discount.setScale(2, RoundingMode.HALF_UP);
        }
        if (tax != null) {
            if (tax.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_TAX", "Tax amount cannot be negative.");
            }
            this.taxAmount = tax.setScale(2, RoundingMode.HALF_UP);
        }
        this.dueDate = dueDate != null ? dueDate : LocalDate.now().plusDays(30);
        this.issuedAt = Instant.now();
        this.status = InvoiceStatus.ISSUED;
        recalculateTotals();
    }

    public void recordPayment(BigDecimal paymentAmount) {
        if (this.status != InvoiceStatus.ISSUED && this.status != InvoiceStatus.PARTIALLY_PAID) {
            throw new InvalidInvoiceStatusTransitionException(
                    "Payments can only be applied to ISSUED or PARTIALLY_PAID invoices. Current status: " + this.status);
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_PAYMENT_AMOUNT", "Payment amount must be greater than zero.");
        }
        BigDecimal normalizedAmount = paymentAmount.setScale(2, RoundingMode.HALF_UP);
        if (normalizedAmount.compareTo(this.balanceDue) > 0) {
            throw new OverpaymentException(
                    String.format("Payment amount %s exceeds remaining balance due %s.", normalizedAmount, this.balanceDue));
        }

        this.paidAmount = this.paidAmount.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP);
        this.balanceDue = this.totalAmount.subtract(this.paidAmount).setScale(2, RoundingMode.HALF_UP);

        if (this.balanceDue.compareTo(BigDecimal.ZERO) == 0) {
            this.status = InvoiceStatus.PAID;
            this.paidAt = Instant.now();
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void cancel(String reason) {
        if (this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.PARTIALLY_PAID) {
            throw new InvalidInvoiceStatusTransitionException(
                    "Cannot cancel an invoice with recorded payments. Current status: " + this.status);
        }
        if (this.status == InvoiceStatus.CANCELLED) {
            throw new InvalidInvoiceStatusTransitionException("Invoice is already cancelled.");
        }
        this.status = InvoiceStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "Cancelled: " + reason;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(String admissionId) {
        this.admissionId = admissionId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount != null ? taxAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount != null ? paidAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue != null ? balanceDue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
