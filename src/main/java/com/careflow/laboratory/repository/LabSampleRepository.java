package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for laboratory specimen accessions (§27).
 */
@Repository
public interface LabSampleRepository extends JpaRepository<LabSample, String> {

    Optional<LabSample> findBySampleBarcode(String sampleBarcode);

    List<LabSample> findByLabOrderId(String labOrderId);

    boolean existsBySampleBarcode(String sampleBarcode);
}
