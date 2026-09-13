package com.careflow.notification.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.dto.NotificationPreferenceRequest;
import com.careflow.notification.dto.NotificationPreferenceResponse;
import com.careflow.notification.dto.NotificationResponse;
import com.careflow.notification.dto.SendNotificationRequest;
import com.careflow.notification.dto.UnreadCountResponse;
import com.careflow.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for multi-channel hospital notifications, in-app feed, and preferences (§35, §91).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'BILLING_OFFICER')")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse response = notificationService.sendNotification(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/notifications/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(value = "status", required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        String userId = resolvePrincipal(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, status, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        String userId = resolvePrincipal(authentication);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = resolvePrincipal(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String userId = resolvePrincipal(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reminders/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<NotificationResponse> sendAppointmentReminder(
            @PathVariable("appointmentId") String appointmentId,
            @RequestParam(value = "channel", required = false) NotificationChannel channel) {

        NotificationResponse response = notificationService.sendAppointmentReminder(appointmentId, channel);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/notifications/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getUserPreferences(Authentication authentication) {
        String userId = resolvePrincipal(authentication);
        return ResponseEntity.ok(notificationService.getUserPreferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updateUserPreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {

        String userId = resolvePrincipal(authentication);
        return ResponseEntity.ok(notificationService.updateUserPreferences(userId, request));
    }

    private String resolvePrincipal(Authentication authentication) {
        return (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "ANONYMOUS";
    }
}
