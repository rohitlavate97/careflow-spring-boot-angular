package com.careflow.appointment.controller;

import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.AppointmentSummaryResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.dto.RescheduleAppointmentRequest;
import com.careflow.appointment.service.AppointmentService;
import com.careflow.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * REST Controller exposing appointment booking, lifecycle transitions, and search APIs (§19, §20, §39).
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<AppointmentSummaryResponse>> searchAppointments(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime,
            @PageableDefault(page = 0, size = 20, sort = "appointmentDateTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<AppointmentSummaryResponse> response = appointmentService.searchAppointments(
                patientId, doctorId, departmentId, status, fromDateTime, toDateTime, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> checkInAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.checkInAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> startConsultation(@PathVariable String id) {
        AppointmentResponse response = appointmentService.startConsultation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> completeConsultation(@PathVariable String id) {
        AppointmentResponse response = appointmentService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable String id,
            @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        AppointmentResponse response = appointmentService.cancelAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> markNoShow(@PathVariable String id) {
        AppointmentResponse response = appointmentService.markNoShow(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable String id,
            @Valid @RequestBody RescheduleAppointmentRequest request
    ) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(response);
    }
}
