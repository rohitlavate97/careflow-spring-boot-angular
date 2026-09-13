package com.careflow.notification.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Recipient communication channel preference settings (§35).
 */
@Entity
@Table(
        name = "notification_preferences",
        indexes = {
                @Index(name = "idx_notification_preferences_user", columnList = "user_id")
        }
)
public class NotificationPreference extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false, unique = true)
    private String userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    protected NotificationPreference() {
        // JPA requirement
    }

    public NotificationPreference(String id, String userId, boolean emailEnabled, boolean smsEnabled, boolean inAppEnabled) {
        this.id = id;
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailEnabled;
            case SMS -> smsEnabled;
            case IN_APP -> inAppEnabled;
        };
    }

    public void updatePreferences(boolean emailEnabled, boolean smsEnabled, boolean inAppEnabled) {
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.inAppEnabled = inAppEnabled;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationPreference that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
