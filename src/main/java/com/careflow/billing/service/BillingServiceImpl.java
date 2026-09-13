package com.careflow.billing.service;

import com.careflow.billing.domain.Invoice;
import com.careflow.billing.domain.InvoiceItem;
import com.careflow.billing.domain.InvoiceStatus;
import com.careflow.billing.domain.Payment;
import com.careflow.billing.domain.PaymentStatus;
import com.careflow.billing.dto.CreateInvoiceItemRequest;
import com.careflow.billing.dto.CreateInvoiceRequest;
import com.careflow.billing.dto.InvoiceResponse;
import com.careflow.billing.dto.InvoiceSummaryResponse;
import com.careflow.billing.dto.IssueInvoiceRequest;
import com.careflow.billing.dto.PaymentResponse;
import com.careflow.billing.dto.ProcessPaymentRequest;
import com.careflow.billing.exception.InvoiceNotFoundException;
import com.careflow.billing.exception.PaymentNotFoundException;
import com.careflow.billing.mapper.BillingMapper;
import com.careflow.billing.repository.InvoiceItemRepository;
import com.careflow.billing.repository.InvoiceRepository;
import com.careflow.billing.repository.PaymentRepository;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of billing operations, clinical line item aggregation, invoice state progression,
 * and concurrency-safe idempotent payment processing (§29, §30, §31, §32, §57 Lab 5).
 */
@Service
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final BillingMapper billingMapper;

    public BillingServiceImpl(InvoiceRepository invoiceRepository,
                              InvoiceItemRepository invoiceItemRepository,
                              PaymentRepository paymentRepository,
                              PatientRepository patientRepository,
                              BillingMapper billingMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.billingMapper = billingMapper;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice for patient ID: {}", request.patientId());

        if (!patientRepository.existsById(request.patientId())) {
            throw new ResourceNotFoundException("Patient", request.patientId());
        }

        String invoiceId = UUID.randomUUID().toString();
        String invoiceNumber = "INV-" + LocalDate.now().format(DATE_FORMATTER) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = new Invoice(
                invoiceId,
                invoiceNumber,
                request.patientId(),
                request.encounterId(),
                request.admissionId(),
                request.currency(),
                request.notes()
        );

        if (request.items() != null && !request.items().isEmpty()) {
            for (CreateInvoiceItemRequest itemReq : request.items()) {
                InvoiceItem item = new InvoiceItem(
                        UUID.randomUUID().toString(),
                        invoice,
                        itemReq.billingSource(),
                        itemReq.itemCode(),
                        itemReq.description(),
                        itemReq.unitPrice(),
                        itemReq.quantity(),
                        itemReq.sourceReferenceId()
                );
                invoice.addItem(item);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice [{}] with initial subtotal: {}", saved.getInvoiceNumber(), saved.getSubtotal());
        return billingMapper.toInvoiceResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse addItemToInvoice(String invoiceId, CreateInvoiceItemRequest request) {
        log.info("Adding line item [{}] to invoice ID: {}", request.itemCode(), invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        InvoiceItem item = new InvoiceItem(
                UUID.randomUUID().toString(),
                invoice,
                request.billingSource(),
                request.itemCode(),
                request.description(),
                request.unitPrice(),
                request.quantity(),
                request.sourceReferenceId()
        );

        invoice.addItem(item);
        Invoice saved = invoiceRepository.save(invoice);
        return billingMapper.toInvoiceResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse removeItemFromInvoice(String invoiceId, String itemId) {
        log.info("Removing line item ID [{}] from invoice ID: {}", itemId, invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.removeItem(itemId);
        Invoice saved = invoiceRepository.save(invoice);
        return billingMapper.toInvoiceResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(String invoiceId, IssueInvoiceRequest request) {
        log.info("Issuing invoice ID: {}", invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.issue(
                request != null ? request.dueDate() : null,
                request != null ? request.discountAmount() : null,
                request != null ? request.taxAmount() : null
        );

        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") + request.notes());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Issued invoice [{}] with total amount: {} and balance due: {}",
                saved.getInvoiceNumber(), saved.getTotalAmount(), saved.getBalanceDue());
        return billingMapper.toInvoiceResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(String invoiceId, String reason) {
        log.info("Cancelling invoice ID: {} for reason: {}", invoiceId, reason);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.cancel(reason);
        Invoice saved = invoiceRepository.save(invoice);
        return billingMapper.toInvoiceResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(String id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        return billingMapper.toInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceNumber));
        return billingMapper.toInvoiceResponse(invoice);
    }

    @Override
    public Page<InvoiceSummaryResponse> getInvoicesByPatient(String patientId, Pageable pageable) {
        return invoiceRepository.findByPatientId(patientId, pageable)
                .map(billingMapper::toInvoiceSummaryResponse);
    }

    @Override
    public Page<InvoiceSummaryResponse> getInvoicesByStatus(InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable)
                .map(billingMapper::toInvoiceSummaryResponse);
    }

    @Override
    public Page<InvoiceSummaryResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(billingMapper::toInvoiceSummaryResponse);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        String key = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey().trim()
                : UUID.randomUUID().toString();

        log.info("Processing payment for invoice [{}] with idempotency key [{}]", request.invoiceId(), key);

        // 1. Fast-path check: Return existing payment transaction if already processed
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(key);
        if (existingPayment.isPresent()) {
            log.info("Idempotent payment replay detected for key: {}. Returning existing record.", key);
            return billingMapper.toPaymentResponse(existingPayment.get());
        }

        // 2. Acquire pessimistic write lock on the invoice row to prevent race conditions (§57 Lab 5)
        Invoice invoice = invoiceRepository.findByIdForUpdate(request.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(request.invoiceId()));

        // 3. Re-verify idempotency key in case a concurrent thread inserted it while waiting for the lock
        Optional<Payment> concurrentPayment = paymentRepository.findByIdempotencyKey(key);
        if (concurrentPayment.isPresent()) {
            log.info("Idempotent payment replay detected post-lock for key: {}. Returning existing record.", key);
            return billingMapper.toPaymentResponse(concurrentPayment.get());
        }

        // 4. Apply payment amount to invoice (validates balance due and updates status)
        invoice.recordPayment(request.amount());

        // 5. Generate and persist payment entity
        String paymentId = UUID.randomUUID().toString();
        String paymentNumber = "PAY-" + LocalDate.now().format(DATE_FORMATTER) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment(
                paymentId,
                paymentNumber,
                invoice,
                key,
                request.paymentMethod(),
                request.amount(),
                PaymentStatus.SUCCESS,
                request.transactionReference(),
                request.notes(),
                Instant.now()
        );

        Payment savedPayment = paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        log.info("Successfully recorded payment [{}] of amount [{}] against invoice [{}]. Remaining balance: {}",
                savedPayment.getPaymentNumber(), savedPayment.getAmount(), invoice.getInvoiceNumber(), invoice.getBalanceDue());

        return billingMapper.toPaymentResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return billingMapper.toPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByInvoice(String invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(billingMapper::toPaymentResponse)
                .toList();
    }
}
