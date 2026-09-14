package com.careflow.appointment.listener;

import com.careflow.appointment.exception.AppointmentNotFoundException;
import com.careflow.common.config.KafkaConfig;
import com.careflow.common.event.AppointmentBookedEvent;
import com.careflow.common.event.AppointmentCancelledEvent;
import com.careflow.common.event.domain.ProcessedEvent;
import com.careflow.common.event.repository.ProcessedEventRepository;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationType;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous Kafka consumer for appointment domain events (§51, §52, §76).
 * Enforces database-backed idempotency against duplicate deliveries (Scenario 7)
 * and guarantees eventual consistency with notifications (Scenario 6).
 */
@Component
@ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AppointmentEventConsumer(
            ProcessedEventRepository processedEventRepository,
            @Autowired(required = false) NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_APPOINTMENTS,
            groupId = KafkaConfig.GROUP_NOTIFICATIONS
    )
    @Transactional
    public void handleAppointmentEvent(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String eventId = rootNode.path("eventId").asText();
            String eventType = rootNode.path("eventType").asText();

            if (eventId.isBlank() || eventType.isBlank()) {
                log.warn("Discarding malformed appointment event payload: {}", payload);
                return;
            }

            // Database-level idempotency check (§51, §76 Scenario 7)
            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, KafkaConfig.GROUP_NOTIFICATIONS)) {
                log.info("Duplicate Kafka event discarded: id='{}', type='{}', group='{}' (Exactly-once guaranteed)",
                        eventId, eventType, KafkaConfig.GROUP_NOTIFICATIONS);
                return;
            }

            log.info("Processing Kafka event: id='{}', type='{}', group='{}'",
                    eventId, eventType, KafkaConfig.GROUP_NOTIFICATIONS);

            switch (eventType) {
                case "AppointmentBooked" -> {
                    AppointmentBookedEvent event = objectMapper.treeToValue(rootNode, AppointmentBookedEvent.class);
                    processAppointmentBooked(event);
                }
                case "AppointmentCancelled" -> {
                    AppointmentCancelledEvent event = objectMapper.treeToValue(rootNode, AppointmentCancelledEvent.class);
                    processAppointmentCancelled(event);
                }
                default -> log.debug("Unhandled event type '{}' in appointment consumer", eventType);
            }

            // Record processed event to prevent duplicate execution on redelivery
            try {
                processedEventRepository.saveAndFlush(
                        new ProcessedEvent(eventId, KafkaConfig.GROUP_NOTIFICATIONS, eventType, "SUCCESS")
                );
            } catch (DataIntegrityViolationException dive) {
                log.info("Concurrent duplicate event detected during commit: id='{}', skipping", eventId);
            }

        } catch (Exception e) {
            log.error("Error processing appointment Kafka event: {}", payload, e);
            throw new RuntimeException("Failed to process Kafka appointment event", e);
        }
    }

    private void processAppointmentBooked(AppointmentBookedEvent event) {
        log.info("Handling AppointmentBookedEvent for appointmentId='{}', patientId='{}'",
                event.appointmentId(), event.patientId());

        if (notificationService != null) {
            try {
                // Eventual consistency (§52, §76 Scenario 6): Notification failures do not rollback appointment booking
                notificationService.sendNotification(new SendNotificationRequest(
                        null,
                        null,
                        null,
                        event.patientId(),
                        NotificationType.APPOINTMENT_CONFIRMATION,
                        NotificationChannel.IN_APP,
                        "Appointment Scheduled",
                        "Your appointment has been successfully scheduled for " + event.appointmentDateTime(),
                        "APPOINTMENT",
                        event.appointmentId()
                ));
            } catch (Exception ex) {
                log.warn("Asynchronous notification failed for booked appointmentId='{}'. Core booking remains intact: {}",
                        event.appointmentId(), ex.getMessage());
            }
        }
    }

    private void processAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Handling AppointmentCancelledEvent for appointmentId='{}', reason='{}'",
                event.appointmentId(), event.cancellationReason());

        if (notificationService != null) {
            try {
                notificationService.sendNotification(new SendNotificationRequest(
                        null,
                        null,
                        null,
                        event.patientId(),
                        NotificationType.APPOINTMENT_CANCELLATION,
                        NotificationChannel.IN_APP,
                        "Appointment Cancelled",
                        "Your appointment has been cancelled. Reason: " + event.cancellationReason(),
                        "APPOINTMENT",
                        event.appointmentId()
                ));
            } catch (Exception ex) {
                log.warn("Asynchronous notification failed for cancelled appointmentId='{}'. Core cancellation remains intact: {}",
                        event.appointmentId(), ex.getMessage());
            }
        }
    }
}
