package com.careflow.administration.service;

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
import com.careflow.common.dto.PageResponse;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.UserStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for platform administration, user lifecycle management, and system configuration (§38).
 */
public interface AdministrationService {

    List<SystemSettingResponse> getSettings(SettingCategory category);

    SystemSettingResponse getSettingByKey(String key);

    SystemSettingResponse updateSetting(String key, UpdateSystemSettingRequest request);

    List<SystemSettingResponse> batchUpdateSettings(BatchUpdateSettingsRequest request);

    PageResponse<AdminUserSummaryResponse> getUsers(RoleType role, UserStatus status, String search, Pageable pageable);

    AdminUserDetailResponse getUserById(String userId);

    AdminUserDetailResponse updateUserStatus(String userId, UpdateUserStatusRequest request);

    AdminUserDetailResponse updateUserRoles(String userId, UpdateUserRolesRequest request);

    void resetUserPassword(String userId, AdminResetPasswordRequest request);

    MaintenanceModeResponse getMaintenanceStatus();

    MaintenanceModeResponse setMaintenanceMode(MaintenanceModeRequest request);

    AdminOverviewResponse getAdminOverview();
}
