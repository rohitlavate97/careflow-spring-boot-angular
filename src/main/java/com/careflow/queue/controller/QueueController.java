package com.careflow.queue.controller;

import com.careflow.queue.dto.DepartmentQueueLiveStatusResponse;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;
import com.careflow.queue.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * REST controller exposing endpoints for Patient Queue operations (§21, §40, §89).
 */
@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> enqueuePatient(@Valid @RequestBody EnqueuePatientRequest request) {
        QueueEntryResponse response = queueService.enqueuePatient(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/entries/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QueueEntryResponse> getQueueEntry(@PathVariable String id) {
        QueueEntryResponse response = queueService.getQueueEntry(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/departments/{departmentId}/call-next")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> callNextPatient(
            @PathVariable String departmentId,
            @RequestParam(required = false) String doctorId) {
        QueueEntryResponse response = queueService.callNextPatient(departmentId, doctorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}/start-consultation")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> startConsultation(@PathVariable String id) {
        QueueEntryResponse response = queueService.startConsultation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> completeConsultation(@PathVariable String id) {
        QueueEntryResponse response = queueService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}/skip")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> skipPatient(@PathVariable String id) {
        QueueEntryResponse response = queueService.skipPatient(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}/requeue")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> requeuePatient(@PathVariable String id) {
        QueueEntryResponse response = queueService.requeuePatient(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/entries/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> cancelQueueEntry(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        QueueEntryResponse response = queueService.cancelQueueEntry(id, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/departments/{departmentId}/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentQueueLiveStatusResponse> getDepartmentLiveStatus(@PathVariable String departmentId) {
        DepartmentQueueLiveStatusResponse response = queueService.getDepartmentLiveStatus(departmentId);
        return ResponseEntity.ok(response);
    }
}
