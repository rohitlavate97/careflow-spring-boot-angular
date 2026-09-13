package com.careflow.billing.repository;

import com.careflow.billing.domain.BillingSource;
import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient patient;
    private Invoice invoiceDraft;
    private Invoice invoiceIssued;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-REPO-TEST", "Marie", "Curie",
                LocalDate.of(1985, 5, 15), Gender.FEMALE, "+1-555-0901"
        ));

        invoiceDraft = new Invoice(
                UUID.randomUUID().toString(), "INV-TEST-001", patient.getId(),
                null, null, "USD", "Draft invoice"
        );
        InvoiceItem item1 = new InvoiceItem(
                UUID.randomUUID().toString(), invoiceDraft, BillingSource.CONSULTATION,
                "CONS-01", "Consultation", BigDecimal.valueOf(100.00), 1, null
        );
        invoiceDraft.addItem(item1);
        invoiceDraft = invoiceRepository.save(invoiceDraft);

        invoiceIssued = new Invoice(
                UUID.randomUUID().toString(), "INV-TEST-002", patient.getId(),
                null, null, "USD", "Issued invoice"
        );
        InvoiceItem item2 = new InvoiceItem(
                UUID.randomUUID().toString(), invoiceIssued, BillingSource.LABORATORY,
                "LAB-01", "Blood Work", BigDecimal.valueOf(50.00), 2, null
        );
        invoiceIssued.addItem(item2);
        invoiceIssued.issue(LocalDate.now().plusDays(30), BigDecimal.ZERO, BigDecimal.ZERO);
        invoiceIssued = invoiceRepository.save(invoiceIssued);
    }

    @Test
    @DisplayName("Find invoice by unique invoice number")
    void findByInvoiceNumber_success() {
        Optional<Invoice> found = invoiceRepository.findByInvoiceNumber("INV-TEST-001");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(invoiceDraft.getId());
        assertThat(found.get().getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(found.get().getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Retrieve invoice with pessimistic lock (findByIdForUpdate)")
    void findByIdForUpdate_success() {
        Optional<Invoice> locked = invoiceRepository.findByIdForUpdate(invoiceIssued.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(locked.get().getBalanceDue()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Find invoices by patient ID with pagination")
    void findByPatientId_success() {
        Page<Invoice> page = invoiceRepository.findByPatientId(patient.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Find invoices by status filter")
    void findByStatus_success() {
        Page<Invoice> drafts = invoiceRepository.findByStatus(InvoiceStatus.DRAFT, PageRequest.of(0, 10));
        assertThat(drafts.getContent()).extracting(Invoice::getInvoiceNumber).contains("INV-TEST-001");

        Page<Invoice> issued = invoiceRepository.findByStatus(InvoiceStatus.ISSUED, PageRequest.of(0, 10));
        assertThat(issued.getContent()).extracting(Invoice::getInvoiceNumber).contains("INV-TEST-002");
    }
}
