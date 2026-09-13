package com.careflow.billing.domain;

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
import java.util.Objects;

/**
 * Itemized clinical or operational charge line item on an invoice (§29, §30).
 */
@Entity
@Table(
        name = "invoice_items",
        indexes = {
                @Index(name = "idx_invoice_items_invoice", columnList = "invoice_id"),
                @Index(name = "idx_invoice_items_source", columnList = "billing_source"),
                @Index(name = "idx_invoice_items_code", columnList = "item_code")
        }
)
public class InvoiceItem extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_source", length = 50, nullable = false)
    private BillingSource billingSource;

    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "total_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "source_reference_id", length = 64)
    private String sourceReferenceId;

    protected InvoiceItem() {
    }

    public InvoiceItem(String id,
                       Invoice invoice,
                       BillingSource billingSource,
                       String itemCode,
                       String description,
                       BigDecimal unitPrice,
                       int quantity,
                       String sourceReferenceId) {
        this.id = id;
        this.invoice = invoice;
        this.billingSource = billingSource;
        this.itemCode = itemCode;
        this.description = description;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.quantity = Math.max(1, quantity);
        this.sourceReferenceId = sourceReferenceId;
        this.totalPrice = calculateTotalPrice();
    }

    public BigDecimal calculateTotalPrice() {
        if (this.unitPrice == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public void updateQuantityAndPrice(int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
        if (unitPrice != null) {
            this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
        this.totalPrice = calculateTotalPrice();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public BillingSource getBillingSource() {
        return billingSource;
    }

    public void setBillingSource(BillingSource billingSource) {
        this.billingSource = billingSource;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalPrice = calculateTotalPrice();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = calculateTotalPrice();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice != null ? totalPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(String sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
