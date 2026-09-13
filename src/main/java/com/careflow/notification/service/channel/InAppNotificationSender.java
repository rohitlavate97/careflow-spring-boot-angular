package com.careflow.notification.service.channel;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-App notification sender delivering messages to the user's persistent notification center feed (§35).
 */
@Component
public class InAppNotificationSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getRecipientUserId() == null && notification.getPatientId() == null) {
            log.warn("In-app notification [{}] has neither recipientUserId nor patientId targeted.", notification.getId());
            notification.markAsFailed("Target recipient user or patient ID is required for in-app delivery.");
            return;
        }

        notification.markAsSent();
        notification.markAsDelivered();
        log.info("In-app notification delivered: id={}, recipient={}, title='{}'",
                notification.getId(), notification.getRecipientUserId(), notification.getTitle());
    }
}
