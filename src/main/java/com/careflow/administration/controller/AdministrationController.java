package com.careflow.administration.controller;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.dto.AdminOverviewResponse;
import com.careflow.administration.dto.AdminResetPasswordRequest;
import com.careflow.administration.dto.AdminUserDetailResponse;
import com.careflow.administration.dto.AdminUserSummaryResponse;
import com.careflow.administration.dto.BatchUpdateSettingsRequest;
import com.careflow.administration.dto.MaintenanceModeRequest;
import com.careflow.administration.dto.MaintenanceModeResponse;
import com.careflow.administration.dto.SystemSettingResponse;
import com.careflow.administration.dto.UpdateSystemSettingRequest;
import com.careflow.administration.dto.UpdateUserRolesRequest;
import com.careflow.administration.dto.UpdateUserStatusRequest;
import com.careflow.administration.service.AdministrationService;
import com.careflow.common.dto.PageResponse;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.UserStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for hospital administration, user management, and system configuration (§38, §91).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdministrationController {

    private final AdministrationService administrationService;

    public AdministrationController(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> getAdminOverview() {
        return ResponseEntity.ok(administrationService.getAdminOverview());
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingResponse>> getSettings(
            @RequestParam(required = false) SettingCategory category) {
        return ResponseEntity.ok(administrationService.getSettings(category));
    }

    @GetMapping("/settings/{key}")
    public ResponseEntity<SystemSettingResponse> getSettingByKey(@PathVariable String key) {
        return ResponseEntity.ok(administrationService.getSettingByKey(key));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<SystemSettingResponse> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemSettingRequest request) {
        return ResponseEntity.ok(administrationService.updateSetting(key, request));
    }

    @PostMapping("/settings/batch")
    public ResponseEntity<List<SystemSettingResponse>> batchUpdateSettings(
            @Valid @RequestBody BatchUpdateSettingsRequest request) {
        return ResponseEntity.ok(administrationService.batchUpdateSettings(request));
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserSummaryResponse>> getUsers(
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(administrationService.getUsers(role, status, search, pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(administrationService.getUserById(userId));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserDetailResponse> updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(administrationService.updateUserStatus(userId, request));
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<AdminUserDetailResponse> updateUserRoles(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(administrationService.updateUserRoles(userId, request));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Void> resetUserPassword(
            @PathVariable String userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        administrationService.resetUserPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/maintenance")
    public ResponseEntity<MaintenanceModeResponse> getMaintenanceStatus() {
        return ResponseEntity.ok(administrationService.getMaintenanceStatus());
    }

    @PostMapping("/maintenance")
    public ResponseEntity<MaintenanceModeResponse> setMaintenanceMode(
            @Valid @RequestBody MaintenanceModeRequest request) {
        return ResponseEntity.ok(administrationService.setMaintenanceMode(request));
    }
}
