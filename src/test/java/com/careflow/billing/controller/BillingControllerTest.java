package com.careflow.billing.controller;

import com.careflow.billing.domain.BillingSource;
import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.PaymentMethod;
import com.careflow.billing.dto.CreateInvoiceItemRequest;
import com.careflow.billing.dto.CreateInvoiceRequest;
import com.careflow.billing.dto.IssueInvoiceRequest;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.billing.repository.PaymentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Patient patient;
    private Invoice existingInvoice;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-CTRL-" + UUID.randomUUID().toString().substring(0, 4),
                "Charles", "Babbage", LocalDate.of(1990, 3, 10), Gender.MALE, "+1-555-0902"
        ));

        existingInvoice = new Invoice(
                UUID.randomUUID().toString(), "INV-CTRL-" + UUID.randomUUID().toString().substring(0, 4),
                patient.getId(), null, null, "USD", "Initial notes"
        );
        InvoiceItem item = new InvoiceItem(
                UUID.randomUUID().toString(), existingInvoice, BillingSource.CONSULTATION,
                "CONS-01", "Physician Consultation", BigDecimal.valueOf(120.00), 1, null
        );
        existingInvoice.addItem(item);
        existingInvoice = invoiceRepository.save(existingInvoice);
    }

    @Test
    @DisplayName("BILLING_OFFICER can create draft invoice (201 Created)")
    @WithMockUser(username = "billing@careflow.local", roles = {"BILLING_OFFICER"})
    void createInvoice_billingOfficer_success() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                patient.getId(), null, null, "USD", "Routine checkup billing",
                List.of(new CreateInvoiceItemRequest(
                        BillingSource.CONSULTATION, "CONS-GEN", "Consultation",
                        BigDecimal.valueOf(100.00), 1, null
                ))
        );

        mockMvc.perform(post("/api/v1/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/billing/invoices/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.subtotal").value(100.00));
    }

    @Test
    @DisplayName("PATIENT cannot create draft invoice (403 Forbidden)")
    @WithMockUser(username = "patient@careflow.local", roles = {"PATIENT"})
    void createInvoice_patient_forbidden() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                patient.getId(), null, null, "USD", "Unauthorized", null
        );

        mockMvc.perform(post("/api/v1/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Full billing lifecycle: Add Item -> Issue -> Pay with Idempotency Key")
    @WithMockUser(username = "billing@careflow.local", roles = {"BILLING_OFFICER"})
    void fullBillingLifecycle_success() throws Exception {
        // 1. Add item
        CreateInvoiceItemRequest addItemReq = new CreateInvoiceItemRequest(
                BillingSource.PHARMACY, "MED-01", "Prescribed Medication",
                BigDecimal.valueOf(30.00), 1, null
        );
        mockMvc.perform(post("/api/v1/billing/invoices/" + existingInvoice.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(150.00)); // 120 + 30

        // 2. Issue invoice with discount
        IssueInvoiceRequest issueReq = new IssueInvoiceRequest(
                LocalDate.now().plusDays(30), BigDecimal.valueOf(10.00), BigDecimal.ZERO, "Discount applied"
        );
        mockMvc.perform(post("/api/v1/billing/invoices/" + existingInvoice.getId() + "/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.totalAmount").value(140.00)) // 150 - 10
                .andExpect(jsonPath("$.balanceDue").value(140.00));

        // 3. Process payment with Idempotency-Key header
        String idempotencyKey = "PAY-HEADER-IDEMP-" + UUID.randomUUID();
        ProcessPaymentRequest payReq = new ProcessPaymentRequest(
                existingInvoice.getId(), BigDecimal.valueOf(140.00), PaymentMethod.CREDIT_CARD,
                null, "AUTH-9876", "Paid in full"
        );

        MvcResult payResult = mockMvc.perform(post("/api/v1/billing/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(140.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey))
                .andReturn();

        // 4. Retry payment with same Idempotency-Key -> Returns same response without creating new payment
        mockMvc.perform(post("/api/v1/billing/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey));

        // 5. Verify invoice status is now PAID
        mockMvc.perform(get("/api/v1/billing/invoices/" + existingInvoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.balanceDue").value(0.00))
                .andExpect(jsonPath("$.paidAmount").value(140.00));
    }

    @Test
    @DisplayName("PATIENT can view own invoice (200 OK)")
    @WithMockUser(username = "patient@careflow.local", roles = {"PATIENT"})
    void getInvoice_patient_success() throws Exception {
        mockMvc.perform(get("/api/v1/billing/invoices/" + existingInvoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingInvoice.getId()));
    }

    @Test
    @DisplayName("Unauthenticated request receives 401 Unauthorized")
    void unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/billing/invoices/" + existingInvoice.getId()))
                .andExpect(status().isUnauthorized());
    }
}
