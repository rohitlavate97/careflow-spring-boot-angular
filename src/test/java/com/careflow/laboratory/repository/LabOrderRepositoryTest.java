package com.careflow.laboratory.repository;

import com.careflow.laboratory.domain.LabOrder;
import com.careflow.laboratory.domain.LabOrderItem;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabOrderStatus;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LabOrderRepositoryTest {

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private LabTestRepository labTestRepository;

    private LabTest labTest;

    @BeforeEach
    void setUp() {
        labTest = new LabTest(
                UUID.randomUUID().toString(),
                "TEST-GLUC",
                "Glucose Fasting",
                LabTestCategory.BIOCHEMISTRY,
                SpecimenType.BLOOD,
                "70-99",
                "mg/dL",
                4,
                BigDecimal.valueOf(15.00),
                true
        );
        labTestRepository.saveAndFlush(labTest);
    }

    @Test
    @DisplayName("Should persist lab order with items and retrieve by order number")
    void persistAndRetrieveByOrderNumber() {
        String orderId = UUID.randomUUID().toString();
        String orderNumber = "ORD-LAB-20260913-0001";
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        LabOrder order = new LabOrder(
                orderId, orderNumber, "pat-001", "staff-doc-001", null,
                LabOrderPriority.STAT, "Patient experiencing acute dizziness", now
        );

        LabOrderItem item = new LabOrderItem(UUID.randomUUID().toString(), labTest, "Fasting 8 hours verified");
        order.addItem(item);

        labOrderRepository.saveAndFlush(order);

        Optional<LabOrder> fetched = labOrderRepository.findByOrderNumber(orderNumber);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getOrderNumber()).isEqualTo(orderNumber);
        assertThat(fetched.get().getPriority()).isEqualTo(LabOrderPriority.STAT);
        assertThat(fetched.get().getStatus()).isEqualTo(LabOrderStatus.ORDERED);
        assertThat(fetched.get().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should query lab orders by patient ID paginated")
    void findByPatientId() {
        String patientId = "pat-special-01";
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        LabOrder order1 = new LabOrder(UUID.randomUUID().toString(), "ORD-LAB-1", patientId, "staff-doc-001", null, LabOrderPriority.ROUTINE, null, now.minusSeconds(100));
        LabOrder order2 = new LabOrder(UUID.randomUUID().toString(), "ORD-LAB-2", patientId, "staff-doc-001", null, LabOrderPriority.URGENT, null, now);

        labOrderRepository.save(order1);
        labOrderRepository.save(order2);
        labOrderRepository.flush();

        Page<LabOrder> page = labOrderRepository.findByPatientIdOrderByOrderedAtDesc(patientId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getOrderNumber()).isEqualTo("ORD-LAB-2");
        assertThat(page.getContent().get(1).getOrderNumber()).isEqualTo("ORD-LAB-1");
    }
}
