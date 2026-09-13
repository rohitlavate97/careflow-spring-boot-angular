package com.careflow.notification.service;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.exception.AppointmentNotFoundException;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.common.dto.PageResponse;
import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationPreference;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.domain.NotificationType;
import com.careflow.notification.dto.NotificationPreferenceRequest;
import com.careflow.notification.dto.NotificationPreferenceResponse;
import com.careflow.notification.dto.NotificationResponse;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.dto.UnreadCountResponse;
import com.careflow.notification.exception.InvalidNotificationStateException;
import com.careflow.notification.exception.NotificationNotFoundException;
import com.careflow.notification.mapper.NotificationMapper;
import com.careflow.notification.repository.NotificationPreferenceRepository;
import com.careflow.notification.repository.NotificationRepository;
import com.careflow.notification.service.channel.EmailNotificationSender;
import com.careflow.notification.service.channel.InAppNotificationSender;
import com.careflow.notification.service.channel.NotificationChannelSender;
import com.careflow.notification.service.channel.SmsNotificationSender;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = new NotificationMapper();
        List<NotificationChannelSender> senders = List.of(
                new InAppNotificationSender(),
                new EmailNotificationSender(),
                new SmsNotificationSender()
        );
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                preferenceRepository,
                patientRepository,
                appointmentRepository,
                senders,
                notificationMapper
        );
    }

    @Test
    @DisplayName("Dispatches IN_APP notification successfully")
    void sendNotification_InApp_Success() {
        SendNotificationRequest request = new SendNotificationRequest(
                "user-100",
                null,
                null,
                "pat-100",
                NotificationType.LAB_RESULT_AVAILABLE,
                NotificationChannel.IN_APP,
                "Lab Results Available",
                "Your lipid panel results are ready for viewing.",
                "LAB_ORDER",
                "ord-99"
        );

        when(preferenceRepository.findByUserId("user-100")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Lab Results Available");
        assertThat(response.channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(response.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(response.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("Dispatches EMAIL notification successfully")
    void sendNotification_Email_Success() {
        SendNotificationRequest request = new SendNotificationRequest(
                "user-200",
                "patient@careflow.local",
                null,
                "pat-200",
                NotificationType.PRESCRIPTION_READY,
                NotificationChannel.EMAIL,
                "Prescription Ready",
                "Your Amoxicillin is ready for pickup at the pharmacy.",
                "PRESCRIPTION",
                "rx-101"
        );

        when(preferenceRepository.findByUserId("user-200")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.status()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    @DisplayName("Dispatches SMS notification successfully")
    void sendNotification_Sms_Success() {
        SendNotificationRequest request = new SendNotificationRequest(
                "user-300",
                null,
                "+1-555-0199",
                "pat-300",
                NotificationType.APPOINTMENT_CONFIRMATION,
                NotificationChannel.SMS,
                "Appointment Confirmed",
                "Your appointment with Dr. House is confirmed for tomorrow 10:00 AM.",
                "APPOINTMENT",
                "apt-55"
        );

        when(preferenceRepository.findByUserId("user-300")).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.channel()).isEqualTo(NotificationChannel.SMS);
        assertThat(response.status()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    @DisplayName("Marks notification as FAILED when user has disabled that channel in preferences")
    void sendNotification_ChannelDisabledInPreferences_MarksFailed() {
        SendNotificationRequest request = new SendNotificationRequest(
                "user-optout",
                null,
                "+1-555-9999",
                "pat-optout",
                NotificationType.APPOINTMENT_REMINDER,
                NotificationChannel.SMS,
                "Reminder",
                "Test message",
                null,
                null
        );

        // SMS disabled
        NotificationPreference pref = new NotificationPreference("pref-1", "user-optout", true, false, true);
        when(preferenceRepository.findByUserId("user-optout")).thenReturn(Optional.of(pref));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.sendNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(response.failureReason()).contains("disabled by recipient preferences");
    }

    @Test
    @DisplayName("Marks in-app notification as READ by target recipient")
    void markAsRead_Success() {
        Notification notification = new Notification(
                "notif-1", "user-1", null, null, "pat-1",
                NotificationType.SYSTEM_ALERT, NotificationChannel.IN_APP,
                "Notice", "Update details", null, null
        );
        notification.markAsSent();
        notification.markAsDelivered();

        when(notificationRepository.findByIdForUpdate("notif-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead("notif-1", "user-1");

        assertThat(response.status()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    @DisplayName("Throws AccessDeniedException when another user attempts to mark notification as read")
    void markAsRead_AccessDenied_ThrowsException() {
        Notification notification = new Notification(
                "notif-1", "user-owner", null, null, "pat-1",
                NotificationType.SYSTEM_ALERT, NotificationChannel.IN_APP,
                "Notice", "Update details", null, null
        );

        when(notificationRepository.findByIdForUpdate("notif-1")).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead("notif-1", "other-user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Throws InvalidNotificationStateException when marking failed notification as read")
    void markAsRead_FailedNotification_ThrowsException() {
        Notification notification = new Notification(
                "notif-1", "user-1", null, null, "pat-1",
                NotificationType.SYSTEM_ALERT, NotificationChannel.IN_APP,
                "Notice", "Update details", null, null
        );
        notification.markAsFailed("Network error");

        when(notificationRepository.findByIdForUpdate("notif-1")).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead("notif-1", "user-1"))
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    @DisplayName("Marks all notifications as read for a user")
    void markAllAsRead_Success() {
        Notification n1 = new Notification("n1", "user-1", null, null, null, NotificationType.INVOICE_GENERATED, NotificationChannel.IN_APP, "T1", "M1", null, null);
        n1.markAsDelivered();
        Notification n2 = new Notification("n2", "user-1", null, null, null, NotificationType.PRESCRIPTION_READY, NotificationChannel.IN_APP, "T2", "M2", null, null);
        n2.markAsSent();

        when(notificationRepository.findByRecipientUserIdAndStatus("user-1", NotificationStatus.DELIVERED)).thenReturn(List.of(n1));
        when(notificationRepository.findByRecipientUserIdAndStatus("user-1", NotificationStatus.SENT)).thenReturn(List.of(n2));

        notificationService.markAllAsRead("user-1");

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).save(n1);
        verify(notificationRepository).save(n2);
    }

    @Test
    @DisplayName("Calculates unread notification count correctly")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientUserIdAndStatus("user-1", NotificationStatus.DELIVERED)).thenReturn(3L);
        when(notificationRepository.countByRecipientUserIdAndStatus("user-1", NotificationStatus.SENT)).thenReturn(2L);

        UnreadCountResponse response = notificationService.getUnreadCount("user-1");
        assertThat(response.unreadCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Triggers appointment reminder successfully")
    void sendAppointmentReminder_Success() {
        Appointment apt = new Appointment();
        // Set fields via reflection or helper if needed, or instantiate
        // Let's create appointment using reflection or setters
        java.lang.reflect.Field idField;
        try {
            idField = Appointment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(apt, "apt-999");

            java.lang.reflect.Field patField = Appointment.class.getDeclaredField("patientId");
            patField.setAccessible(true);
            patField.set(apt, "pat-999");

            java.lang.reflect.Field dateField = Appointment.class.getDeclaredField("appointmentDateTime");
            dateField.setAccessible(true);
            dateField.set(apt, LocalDateTime.of(2026, 9, 20, 14, 0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Patient patient = new Patient(
                "pat-999", "MRN-REM-001", "Ada", "Lovelace", LocalDate.of(1990, 1, 1),
                Gender.FEMALE, "+1-555-4321"
        );
        patient.setEmail("ada@careflow.local");

        when(appointmentRepository.findById("apt-999")).thenReturn(Optional.of(apt));
        when(patientRepository.findById("pat-999")).thenReturn(Optional.of(patient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.sendAppointmentReminder("apt-999", NotificationChannel.EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.notificationType()).isEqualTo(NotificationType.APPOINTMENT_REMINDER);
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.recipientEmail()).isEqualTo("ada@careflow.local");
        assertThat(response.message()).contains("Ada Lovelace");
    }

    @Test
    @DisplayName("Updates and retrieves user notification preferences")
    void userPreferences_Success() {
        when(preferenceRepository.findByUserId("user-pref")).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferenceResponse initial = notificationService.getUserPreferences("user-pref");
        assertThat(initial.emailEnabled()).isTrue();
        assertThat(initial.smsEnabled()).isTrue();
        assertThat(initial.inAppEnabled()).isTrue();

        NotificationPreferenceRequest updateReq = new NotificationPreferenceRequest(false, true, false);
        NotificationPreferenceResponse updated = notificationService.updateUserPreferences("user-pref", updateReq);

        assertThat(updated.emailEnabled()).isFalse();
        assertThat(updated.smsEnabled()).isTrue();
        assertThat(updated.inAppEnabled()).isFalse();
    }
}
