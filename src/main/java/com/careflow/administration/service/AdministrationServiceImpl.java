package com.careflow.administration.service;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SettingDataType;
import com.careflow.administration.domain.SystemSetting;
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
import com.careflow.administration.mapper.SystemSettingMapper;
import com.careflow.administration.repository.SystemMetadataRepository;
import com.careflow.administration.repository.SystemSettingRepository;
import com.careflow.audit.domain.AuditAction;
import com.careflow.audit.domain.AuditResourceType;
import com.careflow.audit.domain.AuditStatus;
import com.careflow.audit.service.AuditService;
import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.domain.UserStatus;
import com.careflow.identity.repository.RoleRepository;
import com.careflow.identity.repository.UserRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import com.careflow.common.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for enterprise administration, user lifecycle, and system configuration (§38).
 */
@Service
@Transactional
public class AdministrationServiceImpl implements AdministrationService {

    private static final Logger log = LoggerFactory.getLogger(AdministrationServiceImpl.class);

    private static final String MAINTENANCE_SETTING_KEY = "careflow.system.maintenance_mode";
    private static final String MAINTENANCE_REASON_KEY = "careflow.system.maintenance_reason";
    private static final String FACILITY_NAME_KEY = "careflow.facility.name";

    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingMapper systemSettingMapper;
    private final SystemMetadataRepository systemMetadataRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final DepartmentRepository departmentRepository;
    private final StaffMemberRepository staffMemberRepository;

    public AdministrationServiceImpl(SystemSettingRepository systemSettingRepository,
                                      SystemSettingMapper systemSettingMapper,
                                      SystemMetadataRepository systemMetadataRepository,
                                      UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuditService auditService,
                                      DepartmentRepository departmentRepository,
                                      StaffMemberRepository staffMemberRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemSettingMapper = systemSettingMapper;
        this.systemMetadataRepository = systemMetadataRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.departmentRepository = departmentRepository;
        this.staffMemberRepository = staffMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_SYSTEM_SETTINGS, key = "'category:' + (#category != null ? #category.name() : 'ALL')")
    public List<SystemSettingResponse> getSettings(SettingCategory category) {
        List<SystemSetting> settings = category != null
                ? systemSettingRepository.findByCategoryOrderBySettingKeyAsc(category)
                : systemSettingRepository.findAllByOrderByCategoryAscSettingKeyAsc();

        return settings.stream()
                .map(systemSettingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_SYSTEM_SETTINGS, key = "#key")
    public SystemSettingResponse getSettingByKey(String key) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("System setting not found with key: " + key));
        return systemSettingMapper.toResponse(setting);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_SYSTEM_SETTINGS, allEntries = true)
    public SystemSettingResponse updateSetting(String key, UpdateSystemSettingRequest request) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("System setting not found with key: " + key));

        if (!setting.isEditable()) {
            throw new BusinessRuleException("SETTING_NOT_EDITABLE", "Setting '" + key + "' is protected and cannot be edited");
        }

        String previousValue = setting.getSettingValue();
        String newValue = request.settingValue().trim();
        validateSettingValue(key, setting.getDataType(), newValue);

        setting.setSettingValue(newValue);
        SystemSetting updated = systemSettingRepository.save(setting);

        String currentActor = resolveCurrentActor();
        auditService.recordSensitiveAccess(
                currentActor,
                AuditAction.CONFIGURATION_CHANGED,
                AuditResourceType.SYSTEM,
                setting.getId(),
                null,
                previousValue,
                newValue,
                AuditStatus.SUCCESS,
                "Updated system setting '" + key + "'"
        );

        log.info("System setting '{}' updated by {}", key, currentActor);
        return systemSettingMapper.toResponse(updated);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_SYSTEM_SETTINGS, allEntries = true)
    public List<SystemSettingResponse> batchUpdateSettings(BatchUpdateSettingsRequest request) {
        List<SystemSettingResponse> results = new ArrayList<>();
        String currentActor = resolveCurrentActor();

        for (Map.Entry<String, String> entry : request.settings().entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue() != null ? entry.getValue().trim() : "";

            SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                    .orElseThrow(() -> new ResourceNotFoundException("System setting not found with key: " + key));

            if (!setting.isEditable()) {
                throw new BusinessRuleException("SETTING_NOT_EDITABLE", "Setting '" + key + "' is protected and cannot be edited");
            }

            validateSettingValue(key, setting.getDataType(), newValue);
            String previousValue = setting.getSettingValue();
            setting.setSettingValue(newValue);
            SystemSetting saved = systemSettingRepository.save(setting);

            auditService.recordSensitiveAccess(
                    currentActor,
                    AuditAction.CONFIGURATION_CHANGED,
                    AuditResourceType.SYSTEM,
                    setting.getId(),
                    null,
                    previousValue,
                    newValue,
                    AuditStatus.SUCCESS,
                    "Batch updated system setting '" + key + "'"
            );

            results.add(systemSettingMapper.toResponse(saved));
        }

        log.info("Batch updated {} system settings by {}", results.size(), currentActor);
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> getUsers(RoleType role, UserStatus status, String search, Pageable pageable) {
        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;
        Page<User> page = userRepository.findUsersByAdminCriteria(role, status, cleanSearch, pageable);
        return PageResponse.from(page, systemSettingMapper::toUserSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return systemSettingMapper.toUserDetail(user);
    }

    @Override
    public AdminUserDetailResponse updateUserStatus(String userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String currentActor = resolveCurrentActor();
        if (user.getUsername().equalsIgnoreCase(currentActor) && request.status() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("ADMIN_SELF_DEACTIVATION_PROHIBITED", "Administrators cannot deactivate or suspend their own account");
        }

        UserStatus previousStatus = user.getStatus();
        user.setStatus(request.status());

        if (request.status() == UserStatus.ACTIVE) {
            user.resetFailedLoginAttempts();
        }

        User saved = userRepository.save(user);

        auditService.recordSensitiveAccess(
                currentActor,
                AuditAction.USER_STATUS_CHANGED,
                AuditResourceType.USER,
                user.getId(),
                null,
                previousStatus.name(),
                request.status().name(),
                AuditStatus.SUCCESS,
                "Updated status for user '" + user.getUsername() + "' to " + request.status() + (request.reason() != null ? " Reason: " + request.reason() : "")
        );

        log.info("User '{}' status updated from {} to {} by {}", user.getUsername(), previousStatus, request.status(), currentActor);
        return systemSettingMapper.toUserDetail(saved);
    }

    @Override
    public AdminUserDetailResponse updateUserRoles(String userId, UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String currentActor = resolveCurrentActor();
        if (user.getUsername().equalsIgnoreCase(currentActor) && !request.roles().contains(RoleType.ROLE_ADMIN)) {
            throw new BusinessRuleException("ADMIN_SELF_DEMOTION_PROHIBITED", "Administrators cannot remove the administrator role from their own account");
        }

        Set<Role> newRoles = new HashSet<>();
        for (RoleType roleType : request.roles()) {
            Role role = roleRepository.findByName(roleType)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleType));
            newRoles.add(role);
        }

        String previousRoles = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.joining(","));
        user.setRoles(newRoles);
        User saved = userRepository.save(user);

        String newRolesStr = newRoles.stream().map(r -> r.getName().name()).collect(Collectors.joining(","));
        auditService.recordSensitiveAccess(
                currentActor,
                AuditAction.USER_ROLE_CHANGED,
                AuditResourceType.USER,
                user.getId(),
                null,
                previousRoles,
                newRolesStr,
                AuditStatus.SUCCESS,
                "Updated roles for user '" + user.getUsername() + "' to [" + newRolesStr + "]"
        );

        log.info("User '{}' roles updated to [{}] by {}", user.getUsername(), newRolesStr, currentActor);
        return systemSettingMapper.toUserDetail(saved);
    }

    @Override
    public void resetUserPassword(String userId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String currentActor = resolveCurrentActor();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        auditService.recordSensitiveAccess(
                currentActor,
                AuditAction.USER_PASSWORD_RESET,
                AuditResourceType.USER,
                user.getId(),
                null,
                null,
                null,
                AuditStatus.SUCCESS,
                "Administrative password reset performed for user '" + user.getUsername() + "'"
        );

        log.info("Administrative password reset completed for user '{}' by {}", user.getUsername(), currentActor);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceModeResponse getMaintenanceStatus() {
        SystemSetting modeSetting = systemSettingRepository.findBySettingKey(MAINTENANCE_SETTING_KEY).orElse(null);
        SystemSetting reasonSetting = systemSettingRepository.findBySettingKey(MAINTENANCE_REASON_KEY).orElse(null);

        boolean enabled = modeSetting != null && "true".equalsIgnoreCase(modeSetting.getSettingValue());
        String reason = reasonSetting != null ? reasonSetting.getSettingValue() : "";
        String updatedBy = modeSetting != null ? modeSetting.getUpdatedBy() : "SYSTEM";
        Instant updatedAt = modeSetting != null ? modeSetting.getUpdatedAt() : Instant.now();

        return new MaintenanceModeResponse(enabled, reason, updatedBy, updatedAt);
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_SYSTEM_SETTINGS, allEntries = true)
    public MaintenanceModeResponse setMaintenanceMode(MaintenanceModeRequest request) {
        String currentActor = resolveCurrentActor();
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        String reason = request.reason() != null ? request.reason().trim() : "";

        SystemSetting modeSetting = systemSettingRepository.findBySettingKey(MAINTENANCE_SETTING_KEY)
                .orElseGet(() -> new SystemSetting(
                        UUID.randomUUID().toString(),
                        MAINTENANCE_SETTING_KEY,
                        "false",
                        SettingCategory.MAINTENANCE,
                        SettingDataType.BOOLEAN,
                        "System maintenance mode flag",
                        false,
                        true
                ));

        SystemSetting reasonSetting = systemSettingRepository.findBySettingKey(MAINTENANCE_REASON_KEY)
                .orElseGet(() -> new SystemSetting(
                        UUID.randomUUID().toString(),
                        MAINTENANCE_REASON_KEY,
                        "",
                        SettingCategory.MAINTENANCE,
                        SettingDataType.STRING,
                        "System maintenance reason",
                        false,
                        true
                ));

        String prevMode = modeSetting.getSettingValue();
        modeSetting.setSettingValue(String.valueOf(enabled));
        reasonSetting.setSettingValue(reason);

        systemSettingRepository.save(modeSetting);
        systemSettingRepository.save(reasonSetting);

        auditService.recordSensitiveAccess(
                currentActor,
                AuditAction.CONFIGURATION_CHANGED,
                AuditResourceType.SYSTEM,
                modeSetting.getId(),
                null,
                prevMode,
                String.valueOf(enabled),
                AuditStatus.SUCCESS,
                "Maintenance mode toggled to " + enabled + ". Reason: " + reason
        );

        log.warn("System maintenance mode changed to {} by {}. Reason: {}", enabled, currentActor, reason);
        return new MaintenanceModeResponse(enabled, reason, currentActor, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse getAdminOverview() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long lockedUsers = userRepository.countByStatus(UserStatus.LOCKED);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);

        Map<String, Long> roleCounts = new LinkedHashMap<>();
        List<Object[]> rolesData = userRepository.countUsersByRole();
        for (Object[] row : rolesData) {
            RoleType rt = (RoleType) row[0];
            long count = ((Number) row[1]).longValue();
            roleCounts.put(rt.name(), count);
        }

        long totalDepartments = departmentRepository.count();
        long totalStaff = staffMemberRepository.count();
        long totalSettings = systemSettingRepository.count();

        MaintenanceModeResponse maintenance = getMaintenanceStatus();

        String facilityName = systemSettingRepository.findBySettingKey(FACILITY_NAME_KEY)
                .map(SystemSetting::getSettingValue)
                .orElse("CareFlow Central Hospital");

        String systemVersion = systemMetadataRepository.findByMetadataKey("schema.version")
                .map(com.careflow.administration.domain.SystemMetadata::getMetadataValue)
                .orElse("1.0.0");

        return new AdminOverviewResponse(
                totalUsers,
                activeUsers,
                lockedUsers,
                suspendedUsers,
                roleCounts,
                totalDepartments,
                totalStaff,
                totalSettings,
                maintenance.enabled(),
                maintenance.reason(),
                systemVersion,
                facilityName,
                Instant.now()
        );
    }

    private void validateSettingValue(String key, SettingDataType dataType, String value) {
        if (dataType == SettingDataType.BOOLEAN) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new BusinessRuleException("INVALID_SETTING_FORMAT", "Setting '" + key + "' must be a boolean ('true' or 'false')");
            }
        } else if (dataType == SettingDataType.NUMBER) {
            try {
                new BigDecimal(value);
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("INVALID_SETTING_FORMAT", "Setting '" + key + "' must be a valid number");
            }
        }
    }

    private String resolveCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "SYSTEM";
    }
}
