package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for individual lab order requisition items (§27).
 */
@Repository
public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, String> {

    List<LabOrderItem> findByLabOrderId(String labOrderId);
}
