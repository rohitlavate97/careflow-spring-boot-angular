package com.careflow.notification.controller;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.repository.AppointmentRepository;
import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.domain.NotificationType;
import com.careflow.notification.dto.NotificationPreferenceRequest;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.repository.NotificationRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Patient patient;
    private Appointment appointment;
    private Notification userNotification;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(),
                "MRN-NOTIF-" + UUID.randomUUID().toString().substring(0, 4),
                "Linus", "Torvalds", LocalDate.of(1969, 12, 28), Gender.MALE, "+1-555-0900"
        ));
        patient.setEmail("linus@careflow.local");
        patientRepository.save(patient);

        appointment = appointmentRepository.save(new Appointment(
                UUID.randomUUID().toString(),
                patient.getId(),
                "doc-1",
                "dept-1",
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
                30,
                "Routine checkup"
        ));

        userNotification = new Notification(
                UUID.randomUUID().toString(),
                "linus.user",
                "linus@careflow.local",
                "+1-555-0900",
                patient.getId(),
                NotificationType.LAB_RESULT_AVAILABLE,
                NotificationChannel.IN_APP,
                "Lab Result Ready",
                "Your diagnostic lipid panel result is available.",
                "LAB_ORDER",
                "ord-1"
        );
        userNotification.markAsSent();
        userNotification.markAsDelivered();
        notificationRepository.save(userNotification);
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send - Admin dispatches notification returns 201 Created")
    @WithMockUser(username = "admin.user", roles = "ADMIN")
    void sendNotification_Admin_Returns201() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest(
                "target.user",
                "target@careflow.local",
                "+1-555-0999",
                patient.getId(),
                NotificationType.INVOICE_GENERATED,
                NotificationChannel.IN_APP,
                "New Invoice",
                "Invoice INV-2026-001 has been generated.",
                "INVOICE",
                "inv-123"
        );

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("New Invoice"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send - Patient role is forbidden from dispatching notifications (403)")
    @WithMockUser(username = "patient.user", roles = "PATIENT")
    void sendNotification_Patient_Returns403() throws Exception {
        SendNotificationRequest request = new SendNotificationRequest(
                "target.user", null, null, patient.getId(),
                NotificationType.SYSTEM_ALERT, NotificationChannel.IN_APP,
                "Spam", "Invalid dispatch", null, null
        );

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send - Unauthenticated request returns 401")
    void sendNotification_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/my-notifications - Returns user's in-app notifications")
    @WithMockUser(username = "linus.user", roles = "PATIENT")
    void getMyNotifications_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(userNotification.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Lab Result Ready"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count - Returns unread notification count")
    @WithMockUser(username = "linus.user", roles = "PATIENT")
    void getUnreadCount_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Acknowledges notification as READ")
    @WithMockUser(username = "linus.user", roles = "PATIENT")
    void markAsRead_Returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", userNotification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").isString());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/read-all - Bulk acknowledges notifications")
    @WithMockUser(username = "linus.user", roles = "PATIENT")
    void markAllAsRead_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all"))
                .andExpect(status().isOk());

        Notification updated = notificationRepository.findById(userNotification.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.isRead()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/notifications/reminders/appointment/{id} - Receptionist triggers appointment reminder")
    @WithMockUser(username = "rec.user", roles = "RECEPTIONIST")
    void sendAppointmentReminder_Receptionist_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/reminders/appointment/{id}", appointment.getId())
                        .param("channel", "EMAIL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationType").value("APPOINTMENT_REMINDER"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.recipientEmail").value("linus@careflow.local"));
    }

    @Test
    @DisplayName("GET & PUT /api/v1/notifications/preferences - Manages channel preferences")
    @WithMockUser(username = "pref.user", roles = "PATIENT")
    void preferences_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.smsEnabled").value(true));

        NotificationPreferenceRequest updateReq = new NotificationPreferenceRequest(false, true, true);
        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.smsEnabled").value(true));
    }
}
