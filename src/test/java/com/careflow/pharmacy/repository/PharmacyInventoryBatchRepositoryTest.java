package com.careflow.pharmacy.repository;

import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationForm;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PharmacyInventoryBatchRepositoryTest {

    @Autowired
    private PharmacyInventoryBatchRepository batchRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private Medication medication;

    @BeforeEach
    void setUp() {
        medication = new Medication(
                UUID.randomUUID().toString(), "MED-TEST-01", "Amoxicillin Test", "Amoxicillin",
                MedicationForm.CAPSULE, "500 mg", BigDecimal.valueOf(10.00), 10
        );
        medicationRepository.saveAndFlush(medication);
    }

    @Test
    @DisplayName("Should query active non-expired batches ordered by earliest expiry date (FEFO)")
    void queryBatches_fefoOrdering() {
        LocalDate today = LocalDate.now();

        // Expiring in 6 months
        PharmacyInventoryBatch batchEarlier = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "BATCH-EARLY", today.plusMonths(6), 20, 5
        );
        batchRepository.save(batchEarlier);

        // Expiring in 18 months
        PharmacyInventoryBatch batchLater = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "BATCH-LATER", today.plusMonths(18), 50, 10
        );
        batchRepository.save(batchLater);

        // Expired batch
        PharmacyInventoryBatch batchExpired = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "BATCH-EXPIRED", today.minusMonths(1), 30, 5
        );
        batchRepository.save(batchExpired);

        batchRepository.flush();

        List<PharmacyInventoryBatch> fefoBatches = batchRepository
                .findByMedicationIdAndExpiryDateAfterAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(
                        medication.getId(), today, 0
                );

        assertThat(fefoBatches).hasSize(2);
        assertThat(fefoBatches.get(0).getBatchNumber()).isEqualTo("BATCH-EARLY");
        assertThat(fefoBatches.get(1).getBatchNumber()).isEqualTo("BATCH-LATER");
    }

    @Test
    @DisplayName("findByIdForUpdate should lock and retrieve batch successfully")
    void findByIdForUpdate_success() {
        PharmacyInventoryBatch batch = new PharmacyInventoryBatch(
                UUID.randomUUID().toString(), medication.getId(), "BATCH-LOCK", LocalDate.now().plusYears(1), 100, 10
        );
        batchRepository.saveAndFlush(batch);

        Optional<PharmacyInventoryBatch> locked = batchRepository.findByIdForUpdate(batch.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getBatchNumber()).isEqualTo("BATCH-LOCK");
    }
}
