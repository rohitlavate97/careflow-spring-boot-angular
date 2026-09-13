package com.careflow.staff.controller;

import com.careflow.common.dto.PageResponse;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorSummaryResponse;
import com.careflow.staff.dto.StaffResponse;
import com.careflow.staff.dto.StaffSummaryResponse;
import com.careflow.staff.dto.UpdateStaffRequest;
import com.careflow.staff.dto.UpdateStaffStatusRequest;
import com.careflow.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.List;

/**
 * REST Controller exposing hospital staff and doctor profile management APIs (§17, §39, §89).
 */
@RestController
@RequestMapping("/api/v1/staff")
@Validated
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        StaffResponse response = staffService.createStaff(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffResponse> getStaffById(@PathVariable String id) {
        StaffResponse response = staffService.getStaffById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{staffCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffResponse> getStaffByCode(@PathVariable String staffCode) {
        StaffResponse response = staffService.getStaffByCode(staffCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffResponse> getStaffByUserId(@PathVariable String userId) {
        StaffResponse response = staffService.getStaffByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<StaffSummaryResponse>> searchStaff(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) StaffStatus status,
            @PageableDefault(page = 0, size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<StaffSummaryResponse> response = staffService.searchStaff(query, departmentId, staffType, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable String id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        StaffResponse response = staffService.updateStaff(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStaffStatusRequest request
    ) {
        StaffResponse response = staffService.updateStaffStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<DoctorSummaryResponse>> searchDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String departmentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        PageResponse<DoctorSummaryResponse> response = staffService.searchDoctors(specialization, departmentId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorSummaryResponse>> getActiveDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String departmentId
    ) {
        List<DoctorSummaryResponse> doctors = staffService.getActiveDoctors(specialization, departmentId);
        return ResponseEntity.ok(doctors);
    }
}
