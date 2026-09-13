package com.careflow.notification.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.notification.exception.InvalidNotificationStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Hospital notification aggregate root managing recipient targeting, channel dispatch,
 * delivery acknowledgment, and in-app read tracking (§35, §103 Phase 14).
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_user", columnList = "recipient_user_id, status"),
                @Index(name = "idx_notifications_patient", columnList = "patient_id"),
                @Index(name = "idx_notifications_type", columnList = "notification_type"),
                @Index(name = "idx_notifications_channel", columnList = "channel"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at"),
                @Index(name = "idx_notifications_reference", columnList = "reference_type, reference_id")
        }
)
public class Notification extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "recipient_user_id", length = 64)
    private String recipientUserId;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "patient_id", length = 64)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50, nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", length = 2000, nullable = false)
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
        // JPA requirement
    }

    public Notification(String id,
                        String recipientUserId,
                        String recipientEmail,
                        String recipientPhone,
                        String patientId,
                        NotificationType notificationType,
                        NotificationChannel channel,
                        String title,
                        String message,
                        String referenceType,
                        String referenceId) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.patientId = patientId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = NotificationStatus.PENDING;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.failureReason = null;
    }

    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
    }

    public void markAsRead() {
        if (this.status == NotificationStatus.FAILED) {
            throw new InvalidNotificationStateException("Cannot mark a failed notification as read.");
        }
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean isRead() {
        return this.status == NotificationStatus.READ;
    }

    public String getId() {
        return id;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
