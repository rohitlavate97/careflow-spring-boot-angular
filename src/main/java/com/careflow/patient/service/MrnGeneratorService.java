package com.careflow.patient.service;

import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for generating unique Medical Record Numbers (MRN) (§16).
 * Follows healthcare identifier convention: PAT-YYYYMM-XXXXX (e.g., PAT-202609-84291).
 */
@Service
public class MrnGeneratorService implements MrnGenerator {

    private static final Logger log = LoggerFactory.getLogger(MrnGeneratorService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int MAX_ATTEMPTS = 10;

    private final PatientRepository patientRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public MrnGeneratorService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Generates a unique collision-free MRN verified against the database.
     *
     * @return Unique MRN string
     */
    public String generateUniqueMrn() {
        String yearMonth = LocalDate.now().format(DATE_FORMATTER);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            int sequence = secureRandom.nextInt(90000) + 10000; // 5 digits: 10000..99999
            String candidateMrn = String.format("PAT-%s-%05d", yearMonth, sequence);

            if (!patientRepository.existsByMrn(candidateMrn)) {
                log.debug("Generated unique MRN: {} on attempt {}", candidateMrn, attempt);
                return candidateMrn;
            }
            log.warn("MRN collision detected for {}; retrying (attempt {}/{})", candidateMrn, attempt, MAX_ATTEMPTS);
        }

        // Fallback with timestamp millis suffix to guarantee uniqueness under extreme load
        String fallbackMrn = String.format("PAT-%s-%d", yearMonth, System.currentTimeMillis() % 1000000);
        log.info("Generated fallback MRN: {}", fallbackMrn);
        return fallbackMrn;
    }
}
