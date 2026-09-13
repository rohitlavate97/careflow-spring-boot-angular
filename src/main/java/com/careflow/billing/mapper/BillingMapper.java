package com.careflow.billing.mapper;

import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.Payment;
import com.careflow.billing.dto.InvoiceItemResponse;
import com.careflow.billing.dto.InvoiceResponse;
import com.careflow.billing.dto.InvoiceSummaryResponse;
import com.careflow.billing.dto.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component mapping Billing entities and records to API response DTOs (§29, §31).
 */
@Component
public class BillingMapper {

    public InvoiceItemResponse toInvoiceItemResponse(InvoiceItem item) {
        if (item == null) {
            return null;
        }
        return new InvoiceItemResponse(
                item.getId(),
                item.getInvoice() != null ? item.getInvoice().getId() : null,
                item.getBillingSource(),
                item.getItemCode(),
                item.getDescription(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice(),
                item.getSourceReferenceId()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getInvoice() != null ? payment.getInvoice().getId() : null,
                payment.getIdempotencyKey(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getNotes(),
                payment.getProcessedAt()
        );
    }

    public InvoiceSummaryResponse toInvoiceSummaryResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPatientId(),
                invoice.getStatus(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getDiscountAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getBalanceDue(),
                invoice.getDueDate(),
                invoice.getIssuedAt(),
                invoice.getCreatedAt()
        );
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        List<InvoiceItemResponse> itemResponses = invoice.getItems() != null
                ? invoice.getItems().stream().map(this::toInvoiceItemResponse).collect(Collectors.toList())
                : Collections.emptyList();

        List<PaymentResponse> paymentResponses = invoice.getPayments() != null
                ? invoice.getPayments().stream().map(this::toPaymentResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPatientId(),
                invoice.getEncounterId(),
                invoice.getAdmissionId(),
                invoice.getStatus(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getDiscountAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getBalanceDue(),
                invoice.getNotes(),
                invoice.getDueDate(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                itemResponses,
                paymentResponses
        );
    }
}
