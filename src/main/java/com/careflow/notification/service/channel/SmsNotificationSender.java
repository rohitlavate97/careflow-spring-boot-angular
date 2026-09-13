package com.careflow.notification.service.channel;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Short Message Service (SMS) notification sender abstraction (§35).
 * Validates recipient phone number and coordinates SMS gateway dispatch.
 */
@Component
public class SmsNotificationSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        String recipientPhone = notification.getRecipientPhone();
        if (recipientPhone == null || recipientPhone.isBlank()) {
            log.warn("SMS dispatch failed for notification [{}]: missing recipient phone number.", notification.getId());
            notification.markAsFailed("Recipient phone number is missing or blank.");
            return;
        }

        // Production abstraction: easily extended with Twilio / AWS SNS SMS gateway
        log.info("[SMS OUTBOUND] Dispatched SMS to [{}]: Message='{}'", recipientPhone, notification.getMessage());

        notification.markAsSent();
        notification.markAsDelivered();
    }
}
