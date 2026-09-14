package com.careflow.appointment.listener;

import com.careflow.common.config.KafkaConfig;
import com.careflow.common.event.AppointmentBookedEvent;
import com.careflow.common.event.AppointmentCancelledEvent;
import com.careflow.common.event.domain.ProcessedEvent;
import com.careflow.common.event.repository.ProcessedEventRepository;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private AppointmentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new AppointmentEventConsumer(processedEventRepository, notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should process AppointmentBookedEvent, trigger notification, and record processed event")
    void shouldProcessAppointmentBookedEvent() throws Exception {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "apt-101", "pat-202", "doc-303", "dept-404",
                LocalDateTime.of(2026, 9, 20, 10, 30), 30
        );
        String payload = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventIdAndConsumerGroup(event.getEventId(), KafkaConfig.GROUP_NOTIFICATIONS))
                .thenReturn(false);

        consumer.handleAppointmentEvent(payload);

        // Verifies notification was triggered
        ArgumentCaptor<SendNotificationRequest> notifCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationService).sendNotification(notifCaptor.capture());
        assertThat(notifCaptor.getValue().patientId()).isEqualTo("pat-202");
        assertThat(notifCaptor.getValue().referenceId()).isEqualTo("apt-101");

        // Verifies event was recorded for idempotency
        ArgumentCaptor<ProcessedEvent> processedCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).saveAndFlush(processedCaptor.capture());
        assertThat(processedCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
        assertThat(processedCaptor.getValue().getConsumerGroup()).isEqualTo(KafkaConfig.GROUP_NOTIFICATIONS);
    }

    @Test
    @DisplayName("Failure Scenario 7 (§76): Should drop duplicate Kafka event and prevent duplicate notification")
    void shouldDropDuplicateKafkaEvent() throws Exception {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "apt-dup", "pat-dup", "doc-dup", "dept-dup",
                LocalDateTime.of(2026, 9, 20, 11, 0), 15
        );
        String payload = objectMapper.writeValueAsString(event);

        // Simulate event already exists in processed_events table
        when(processedEventRepository.existsByEventIdAndConsumerGroup(event.getEventId(), KafkaConfig.GROUP_NOTIFICATIONS))
                .thenReturn(true);

        consumer.handleAppointmentEvent(payload);

        // Zero notifications sent, zero duplicate DB entries saved
        verify(notificationService, never()).sendNotification(any());
        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Failure Scenario 6 (§76, §52): Notification failure must not crash consumer or roll back booking")
    void shouldTolerateNotificationFailureGracefully() throws Exception {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "apt-notif-fail", "pat-fail", "doc-fail", "dept-fail",
                LocalDateTime.of(2026, 9, 20, 12, 0), 45
        );
        String payload = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventIdAndConsumerGroup(event.getEventId(), KafkaConfig.GROUP_NOTIFICATIONS))
                .thenReturn(false);
        doThrow(new RuntimeException("SMTP transport unavailable")).when(notificationService).sendNotification(any());

        // Must complete without throwing unhandled exception
        consumer.handleAppointmentEvent(payload);

        // Event is still recorded as processed so consumer won't poison loop
        verify(processedEventRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should process AppointmentCancelledEvent and trigger cancellation notification")
    void shouldProcessAppointmentCancelledEvent() throws Exception {
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                "apt-cancel-1", "pat-cancel-1", "doc-cancel-1", "Patient requested reschedule"
        );
        String payload = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventIdAndConsumerGroup(event.getEventId(), KafkaConfig.GROUP_NOTIFICATIONS))
                .thenReturn(false);

        consumer.handleAppointmentEvent(payload);

        ArgumentCaptor<SendNotificationRequest> notifCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationService).sendNotification(notifCaptor.capture());
        assertThat(notifCaptor.getValue().message()).contains("Patient requested reschedule");
        verify(processedEventRepository).saveAndFlush(any());
    }
}
