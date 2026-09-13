package com.careflow.notification.service.channel;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;

/**
 * Transport sender interface for dispatching notifications across distinct channels (§35).
 */
public interface NotificationChannelSender {

    /**
     * Determines whether this sender supports the specified transport channel.
     */
    boolean supports(NotificationChannel channel);

    /**
     * Executes the dispatch of the notification.
     */
    void send(Notification notification);
}
