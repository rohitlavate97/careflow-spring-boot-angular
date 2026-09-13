package com.careflow.scheduling.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.scheduling.dto.AvailableSlotResponse;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.DoctorLeaveResponse;
import com.careflow.scheduling.dto.DoctorScheduleResponse;
import com.careflow.scheduling.dto.UpdateDoctorLeaveStatusRequest;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;
import com.careflow.scheduling.service.DoctorLeaveService;
import com.careflow.scheduling.service.DoctorScheduleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller exposing doctor schedule, leave management, and appointment slot generation APIs (§18, §39, §89).
 */
@RestController
@RequestMapping("/api/v1/scheduling")
@Validated
public class SchedulingController {

    private final DoctorScheduleService doctorScheduleService;
    private final DoctorLeaveService doctorLeaveService;

    public SchedulingController(DoctorScheduleService doctorScheduleService,
                                DoctorLeaveService doctorLeaveService) {
        this.doctorScheduleService = doctorScheduleService;
        this.doctorLeaveService = doctorLeaveService;
    }

    // Schedule Endpoints (§18)

    @PostMapping("/doctors/{doctorId}/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorScheduleResponse> createSchedule(
            @PathVariable String doctorId,
            @Valid @RequestBody CreateDoctorScheduleRequest request
    ) {
        DoctorScheduleResponse response = doctorScheduleService.createSchedule(doctorId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/scheduling/schedules/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/doctors/{doctorId}/schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorScheduleResponse>> getDoctorSchedules(@PathVariable String doctorId) {
        List<DoctorScheduleResponse> schedules = doctorScheduleService.getDoctorSchedules(doctorId);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorScheduleResponse> getScheduleById(@PathVariable String id) {
        DoctorScheduleResponse response = doctorScheduleService.getScheduleById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorScheduleResponse> updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody UpdateDoctorScheduleRequest request
    ) {
        DoctorScheduleResponse response = doctorScheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String id) {
        doctorScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    // Available Slot Generation Engine (§18, §103 Phase 4)

    @GetMapping("/doctors/{doctorId}/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<AvailableSlotResponse> slots = doctorScheduleService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    // Leave & Schedule Exception Endpoints (§18)

    @PostMapping("/doctors/{doctorId}/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorLeaveResponse> createLeave(
            @PathVariable String doctorId,
            @Valid @RequestBody CreateDoctorLeaveRequest request
    ) {
        DoctorLeaveResponse response = doctorLeaveService.createLeave(doctorId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/scheduling/leaves/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/doctors/{doctorId}/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<PageResponse<DoctorLeaveResponse>> getDoctorLeaves(
            @PathVariable String doctorId,
            @PageableDefault(page = 0, size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<DoctorLeaveResponse> response = doctorLeaveService.getDoctorLeaves(doctorId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaves/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public ResponseEntity<DoctorLeaveResponse> getLeaveById(@PathVariable String id) {
        DoctorLeaveResponse response = doctorLeaveService.getLeaveById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/leaves/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorLeaveResponse> updateLeaveStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateDoctorLeaveStatusRequest request
    ) {
        DoctorLeaveResponse response = doctorLeaveService.updateLeaveStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
