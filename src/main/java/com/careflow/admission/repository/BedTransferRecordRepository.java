package com.careflow.admission.repository;

import com.careflow.admission.domain.BedTransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for bed transfer audit records (§28).
 */
@Repository
public interface BedTransferRecordRepository extends JpaRepository<BedTransferRecord, String> {

    List<BedTransferRecord> findByAdmissionIdOrderByTransferredAtAsc(String admissionId);
}
