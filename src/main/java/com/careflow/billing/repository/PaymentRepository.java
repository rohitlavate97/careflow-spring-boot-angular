package com.careflow.billing.repository;

import com.careflow.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for payment transactions (§31, §32, §57 Lab 5).
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    List<Payment> findByInvoiceId(String invoiceId);
}
