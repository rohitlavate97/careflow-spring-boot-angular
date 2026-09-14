package com.careflow.common.outbox.service;

import com.careflow.common.outbox.domain.OutboxEvent;
import com.careflow.common.outbox.domain.OutboxStatus;
import com.careflow.common.outbox.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxRelayService = new OutboxRelayService(outboxEventRepository, kafkaTemplate, true);
    }

    @Test
    @DisplayName("Should relay pending events to Kafka and transition status to PUBLISHED")
    void shouldRelayPendingEventsToKafka() {
        OutboxEvent event = new OutboxEvent(
                "evt-1", "APPOINTMENT", "apt-1", "AppointmentBooked",
                "{\"appointmentId\":\"apt-1\"}", "careflow.appointments", "apt-1", Instant.now()
        );

        when(outboxEventRepository.findTop50ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        SendResult<String, String> sendResult = new SendResult<>(
                null,
                new RecordMetadata(new TopicPartition("careflow.appointments", 0), 0, 0, 0, 0, 0)
        );
        when(kafkaTemplate.send(eq("careflow.appointments"), eq("apt-1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        int dispatched = outboxRelayService.relayPendingEvents();

        assertThat(dispatched).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getErrorMessage()).isNull();
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Failure Scenario 8 (§76): Should catch Kafka downtime and keep event PENDING without crashing")
    void shouldHandleKafkaDowntimeGracefully() {
        OutboxEvent event = new OutboxEvent(
                "evt-fail", "APPOINTMENT", "apt-fail", "AppointmentBooked",
                "{\"appointmentId\":\"apt-fail\"}", "careflow.appointments", "apt-fail", Instant.now()
        );

        when(outboxEventRepository.findTop50ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> failingFuture = new CompletableFuture<>();
        failingFuture.completeExceptionally(new org.apache.kafka.common.errors.TimeoutException("Kafka broker unreachable"));
        when(kafkaTemplate.send(eq("careflow.appointments"), eq("apt-fail"), anyString()))
                .thenReturn(failingFuture);

        int dispatched = outboxRelayService.relayPendingEvents();

        // Relay completes without throwing unhandled exceptions
        assertThat(dispatched).isZero();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).contains("Kafka broker unreachable");
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    @DisplayName("Should do nothing when Kafka is disabled")
    void shouldDoNothingWhenKafkaIsDisabled() {
        OutboxRelayService disabledRelay = new OutboxRelayService(outboxEventRepository, kafkaTemplate, false);

        int dispatched = disabledRelay.relayPendingEvents();

        assertThat(dispatched).isZero();
        verify(outboxEventRepository, never()).findTop50ByStatusOrderByOccurredAtAsc(any());
    }
}
