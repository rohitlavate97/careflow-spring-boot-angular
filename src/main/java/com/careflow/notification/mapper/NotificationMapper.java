package com.careflow.notification.mapper;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationPreference;
import com.careflow.notification.dto.NotificationPreferenceResponse;
import com.careflow.notification.dto.NotificationResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Notification domain entities into API DTOs (§89).
 */
@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getRecipientEmail(),
                notification.getRecipientPhone(),
                notification.getPatientId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getStatus(),
                notification.getFailureReason(),
                notification.getSentAt(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    public NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        if (preference == null) {
            return null;
        }

        return new NotificationPreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.isEmailEnabled(),
                preference.isSmsEnabled(),
                preference.isInAppEnabled()
        );
    }
}
