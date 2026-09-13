package com.careflow.notification.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.exception.AppointmentNotFoundException;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.common.dto.PageResponse;
import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationPreference;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.dto.NotificationPreferenceRequest;
import com.careflow.notification.dto.NotificationPreferenceResponse;
import com.careflow.notification.dto.NotificationResponse;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.dto.UnreadCountResponse;
import com.careflow.notification.exception.NotificationDeliveryException;
import com.careflow.notification.exception.NotificationNotFoundException;
import com.careflow.notification.mapper.NotificationMapper;
import com.careflow.notification.repository.NotificationPreferenceRepository;
import com.careflow.notification.repository.NotificationRepository;
import com.careflow.notification.service.channel.NotificationChannelSender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of the NotificationService orchestrating channel routing,
 * user preferences, and appointment reminders (§35, §103 Phase 14).
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final List<NotificationChannelSender> channelSenders;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationPreferenceRepository preferenceRepository,
                                   PatientRepository patientRepository,
                                   AppointmentRepository appointmentRepository,
                                   List<NotificationChannelSender> channelSenders,
                                   NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.channelSenders = channelSenders;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        // Preference check if recipient user is registered
        boolean channelPermitted = true;
        if (request.recipientUserId() != null && !request.recipientUserId().isBlank()) {
            Optional<NotificationPreference> pref = preferenceRepository.findByUserId(request.recipientUserId());
            if (pref.isPresent() && !pref.get().isChannelEnabled(request.channel())) {
                channelPermitted = false;
            }
        }

        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                request.recipientUserId(),
                request.recipientEmail(),
                request.recipientPhone(),
                request.patientId(),
                request.notificationType(),
                request.channel(),
                request.title().trim(),
                request.message().trim(),
                request.referenceType(),
                request.referenceId()
        );

        if (!channelPermitted) {
            notification.markAsFailed("Channel [" + request.channel() + "] disabled by recipient preferences.");
            log.info("Notification [{}] skipped: Channel [{}] is disabled by recipient [{}] preferences.",
                    notification.getId(), request.channel(), request.recipientUserId());
        } else {
            NotificationChannelSender sender = channelSenders.stream()
                    .filter(s -> s.supports(request.channel()))
                    .findFirst()
                    .orElseThrow(() -> new NotificationDeliveryException("Unsupported transport channel: " + request.channel()));

            sender.send(notification);
        }

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(String userId, NotificationStatus status, Pageable pageable) {
        Page<Notification> page;
        if (status != null) {
            page = notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            page = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return PageResponse.from(page, notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String userId) {
        long delivered = notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.DELIVERED);
        long sent = notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.SENT);
        return new UnreadCountResponse(delivered + sent);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findByIdForUpdate(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (notification.getRecipientUserId() != null && !notification.getRecipientUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this notification.");
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification [{}] marked as READ by user [{}]", notificationId, userId);

        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> delivered = notificationRepository.findByRecipientUserIdAndStatus(userId, NotificationStatus.DELIVERED);
        List<Notification> sent = notificationRepository.findByRecipientUserIdAndStatus(userId, NotificationStatus.SENT);

        for (Notification n : delivered) {
            n.markAsRead();
            notificationRepository.save(n);
        }
        for (Notification n : sent) {
            n.markAsRead();
            notificationRepository.save(n);
        }
        log.info("Marked all notifications as read for user [{}]: total={}", userId, delivered.size() + sent.size());
    }

    @Override
    @Transactional
    public NotificationResponse sendAppointmentReminder(String appointmentId, NotificationChannel channel) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        Patient patient = patientRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(appointment.getPatientId()));

        NotificationChannel targetChannel = (channel != null) ? channel : NotificationChannel.EMAIL;
        String title = "Appointment Reminder: Upcoming Clinical Consultation";
        String message = String.format("Dear %s %s, this is a reminder for your upcoming appointment scheduled on %s. " +
                        "Please arrive 15 minutes prior to your allocated slot.",
                patient.getFirstName(), patient.getLastName(), appointment.getAppointmentDateTime());

        SendNotificationRequest request = new SendNotificationRequest(
                null,
                patient.getEmail(),
                patient.getPhone(),
                patient.getId(),
                com.careflow.notification.domain.NotificationType.APPOINTMENT_REMINDER,
                targetChannel,
                title,
                message,
                "APPOINTMENT",
                appointment.getId()
        );

        return sendNotification(request);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getUserPreferences(String userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> new NotificationPreference(UUID.randomUUID().toString(), userId, true, true, true));
        return notificationMapper.toPreferenceResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updateUserPreferences(String userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> new NotificationPreference(UUID.randomUUID().toString(), userId, true, true, true));

        preference.updatePreferences(request.emailEnabled(), request.smsEnabled(), request.inAppEnabled());
        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Updated notification preferences for user [{}]: email={}, sms={}, inApp={}",
                userId, saved.isEmailEnabled(), saved.isSmsEnabled(), saved.isInAppEnabled());

        return notificationMapper.toPreferenceResponse(saved);
    }
}
