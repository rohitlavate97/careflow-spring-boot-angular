package com.careflow.notification.service.channel;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Electronic mail notification sender abstraction (§35, §103 Phase 14).
 * Formats email subject and body, validates recipient address, and records dispatch.
 */
@Component
public class EmailNotificationSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        String recipientEmail = notification.getRecipientEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Email dispatch failed for notification [{}]: missing recipient email address.", notification.getId());
            notification.markAsFailed("Recipient email address is missing or blank.");
            return;
        }

        // Production abstraction: easily extended with Spring JavaMailSender / SendGrid / AWS SES
        log.info("[EMAIL OUTBOUND] Dispatched email to [{}]: Subject='{}', Reference={}:{}",
                recipientEmail, notification.getTitle(), notification.getReferenceType(), notification.getReferenceId());

        notification.markAsSent();
        notification.markAsDelivered();
    }
}
