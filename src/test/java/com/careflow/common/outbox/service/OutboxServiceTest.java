package com.careflow.common.outbox.service;

import com.careflow.common.event.AppointmentBookedEvent;
import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.domain.OutboxStatus;
import com.careflow.common.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private OutboxServiceImpl outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        outboxService = new OutboxServiceImpl(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("Should serialize and save domain event to outbox table with PENDING status")
    void shouldSerializeAndSaveDomainEventToOutbox() {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "apt-101", "pat-202", "doc-303", "dept-404",
                LocalDateTime.of(2026, 9, 20, 10, 30), 30
        );

        outboxService.saveEvent(event, "careflow.appointments", "apt-101");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(event.getEventId());
        assertThat(saved.getAggregateType()).isEqualTo("APPOINTMENT");
        assertThat(saved.getAggregateId()).isEqualTo("apt-101");
        assertThat(saved.getEventType()).isEqualTo("AppointmentBooked");
        assertThat(saved.getTopic()).isEqualTo("careflow.appointments");
        assertThat(saved.getPartitionKey()).isEqualTo("apt-101");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getPayload()).contains("apt-101").contains("pat-202");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when serialization fails")
    void shouldThrowWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("Serialization error") {}).when(failingMapper).writeValueAsString(any());

        OutboxServiceImpl failingService = new OutboxServiceImpl(outboxEventRepository, failingMapper);
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "apt-101", "pat-202", "doc-303", "dept-404",
                LocalDateTime.of(2026, 9, 20, 10, 30), 30
        );

        assertThatThrownBy(() -> failingService.saveEvent(event, "careflow.appointments", "apt-101"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize outbox event payload");
    }
}
