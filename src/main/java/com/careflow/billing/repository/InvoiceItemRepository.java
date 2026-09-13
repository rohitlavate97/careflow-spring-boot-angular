package com.careflow.billing.repository;

import com.careflow.billing.domain.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for invoice line items (§29).
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {

    List<InvoiceItem> findByInvoiceId(String invoiceId);
}
