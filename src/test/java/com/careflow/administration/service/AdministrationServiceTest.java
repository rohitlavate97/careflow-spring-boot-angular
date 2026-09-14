package com.careflow.administration.service;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SettingDataType;
import com.careflow.administration.domain.SystemMetadata;
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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrationServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private SystemMetadataRepository systemMetadataRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    private SystemSettingMapper systemSettingMapper;
    private AdministrationServiceImpl administrationService;

    @BeforeEach
    void setUp() {
        systemSettingMapper = new SystemSettingMapper();
        administrationService = new AdministrationServiceImpl(
                systemSettingRepository,
                systemSettingMapper,
                systemMetadataRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                auditService,
                departmentRepository,
                staffMemberRepository
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("superadmin", "n/a", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getSettings returns all settings ordered")
    void getSettings_ReturnsAllSettings() {
        SystemSetting s1 = new SystemSetting("1", "key1", "val1", SettingCategory.FACILITY, SettingDataType.STRING, "desc1", false, true);
        when(systemSettingRepository.findAllByOrderByCategoryAscSettingKeyAsc()).thenReturn(List.of(s1));

        List<SystemSettingResponse> result = administrationService.getSettings(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().settingKey()).isEqualTo("key1");
    }

    @Test
    @DisplayName("getSettings with category filter returns filtered settings")
    void getSettings_CategoryFilter_ReturnsFilteredSettings() {
        SystemSetting s1 = new SystemSetting("1", "key1", "val1", SettingCategory.BILLING, SettingDataType.STRING, "desc1", false, true);
        when(systemSettingRepository.findByCategoryOrderBySettingKeyAsc(SettingCategory.BILLING)).thenReturn(List.of(s1));

        List<SystemSettingResponse> result = administrationService.getSettings(SettingCategory.BILLING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().category()).isEqualTo(SettingCategory.BILLING);
    }

    @Test
    @DisplayName("getSettingByKey returns matching setting")
    void getSettingByKey_Found_ReturnsSetting() {
        SystemSetting s = new SystemSetting("1", "careflow.facility.name", "CareFlow Hospital", SettingCategory.FACILITY, SettingDataType.STRING, "desc", false, true);
        when(systemSettingRepository.findBySettingKey("careflow.facility.name")).thenReturn(Optional.of(s));

        SystemSettingResponse response = administrationService.getSettingByKey("careflow.facility.name");

        assertThat(response.settingKey()).isEqualTo("careflow.facility.name");
        assertThat(response.settingValue()).isEqualTo("CareFlow Hospital");
    }

    @Test
    @DisplayName("getSettingByKey throws ResourceNotFoundException when missing")
    void getSettingByKey_NotFound_ThrowsException() {
        when(systemSettingRepository.findBySettingKey("invalid.key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administrationService.getSettingByKey("invalid.key"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("updateSetting successfully modifies setting and audits change")
    void updateSetting_Success_UpdatesAndAudits() {
        SystemSetting s = new SystemSetting("1", "careflow.appointment.default_slot_minutes", "30", SettingCategory.SCHEDULING, SettingDataType.NUMBER, "desc", false, true);
        when(systemSettingRepository.findBySettingKey("careflow.appointment.default_slot_minutes")).thenReturn(Optional.of(s));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        SystemSettingResponse updated = administrationService.updateSetting(
                "careflow.appointment.default_slot_minutes",
                new UpdateSystemSettingRequest("45")
        );

        assertThat(updated.settingValue()).isEqualTo("45");
        verify(auditService).recordSensitiveAccess(
                eq("superadmin"),
                eq(AuditAction.CONFIGURATION_CHANGED),
                eq(AuditResourceType.SYSTEM),
                eq("1"),
                eq(null),
                eq("30"),
                eq("45"),
                eq(AuditStatus.SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("updateSetting throws BusinessRuleException when setting is protected")
    void updateSetting_Protected_ThrowsException() {
        SystemSetting s = new SystemSetting("1", "careflow.facility.code", "CF-01", SettingCategory.FACILITY, SettingDataType.STRING, "desc", false, false);
        when(systemSettingRepository.findBySettingKey("careflow.facility.code")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> administrationService.updateSetting("careflow.facility.code", new UpdateSystemSettingRequest("NEW-CODE")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("protected and cannot be edited");
    }

    @Test
    @DisplayName("updateSetting throws BusinessRuleException when boolean format is invalid")
    void updateSetting_InvalidBoolean_ThrowsException() {
        SystemSetting s = new SystemSetting("1", "careflow.system.maintenance_mode", "false", SettingCategory.MAINTENANCE, SettingDataType.BOOLEAN, "desc", false, true);
        when(systemSettingRepository.findBySettingKey("careflow.system.maintenance_mode")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> administrationService.updateSetting("careflow.system.maintenance_mode", new UpdateSystemSettingRequest("not-a-boolean")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must be a boolean");
    }

    @Test
    @DisplayName("updateSetting throws BusinessRuleException when number format is invalid")
    void updateSetting_InvalidNumber_ThrowsException() {
        SystemSetting s = new SystemSetting("1", "careflow.appointment.default_slot_minutes", "30", SettingCategory.SCHEDULING, SettingDataType.NUMBER, "desc", false, true);
        when(systemSettingRepository.findBySettingKey("careflow.appointment.default_slot_minutes")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> administrationService.updateSetting("careflow.appointment.default_slot_minutes", new UpdateSystemSettingRequest("invalid-number")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must be a valid number");
    }

    @Test
    @DisplayName("batchUpdateSettings modifies multiple settings and audits each")
    void batchUpdateSettings_Success_UpdatesAndAudits() {
        SystemSetting s1 = new SystemSetting("1", "key1", "old1", SettingCategory.GENERAL, SettingDataType.STRING, "d1", false, true);
        SystemSetting s2 = new SystemSetting("2", "key2", "10", SettingCategory.GENERAL, SettingDataType.NUMBER, "d2", false, true);

        when(systemSettingRepository.findBySettingKey("key1")).thenReturn(Optional.of(s1));
        when(systemSettingRepository.findBySettingKey("key2")).thenReturn(Optional.of(s2));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        List<SystemSettingResponse> responses = administrationService.batchUpdateSettings(
                new BatchUpdateSettingsRequest(Map.of("key1", "new1", "key2", "20"))
        );

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("getUsers returns paginated user summaries")
    void getUsers_ReturnsPaginatedUsers() {
        User user = new User("u-1", "john.doe", "john@careflow.local", "hash", "John", "Doe", "+1-555");
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findUsersByAdminCriteria(null, null, null, pageable)).thenReturn(page);

        PageResponse<AdminUserSummaryResponse> response = administrationService.getUsers(null, null, null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().username()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("getUserById returns full user details")
    void getUserById_Found_ReturnsDetail() {
        User user = new User("u-1", "john.doe", "john@careflow.local", "hash", "John", "Doe", "+1-555");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        AdminUserDetailResponse detail = administrationService.getUserById("u-1");

        assertThat(detail.id()).isEqualTo("u-1");
        assertThat(detail.username()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("updateUserStatus updates status and audits change")
    void updateUserStatus_Success_UpdatesStatus() {
        User user = new User("u-1", "john.doe", "john@careflow.local", "hash", "John", "Doe", "+1-555");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailResponse response = administrationService.updateUserStatus(
                "u-1",
                new UpdateUserStatusRequest(UserStatus.SUSPENDED, "Security review")
        );

        assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
        verify(auditService).recordSensitiveAccess(
                eq("superadmin"),
                eq(AuditAction.USER_STATUS_CHANGED),
                eq(AuditResourceType.USER),
                eq("u-1"),
                eq(null),
                eq("ACTIVE"),
                eq("SUSPENDED"),
                eq(AuditStatus.SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("updateUserStatus throws BusinessRuleException when administrator tries to deactivate self")
    void updateUserStatus_SelfDeactivation_ThrowsException() {
        User adminUser = new User("u-admin", "superadmin", "admin@careflow.local", "hash", "Super", "Admin", "+1-555");
        when(userRepository.findById("u-admin")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> administrationService.updateUserStatus(
                "u-admin",
                new UpdateUserStatusRequest(UserStatus.INACTIVE, "Testing self-deactivation")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot deactivate or suspend their own account");
    }

    @Test
    @DisplayName("updateUserRoles modifies user role assignments and audits change")
    void updateUserRoles_Success_UpdatesRoles() {
        User user = new User("u-1", "nurse.clara", "clara@careflow.local", "hash", "Clara", "Oswald", "+1-555");
        Role doctorRole = new Role("r-doc", RoleType.ROLE_DOCTOR, "Doctor Role");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleType.ROLE_DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailResponse response = administrationService.updateUserRoles(
                "u-1",
                new UpdateUserRolesRequest(Set.of(RoleType.ROLE_DOCTOR))
        );

        assertThat(response.roles()).contains(RoleType.ROLE_DOCTOR);
        verify(auditService).recordSensitiveAccess(
                eq("superadmin"),
                eq(AuditAction.USER_ROLE_CHANGED),
                eq(AuditResourceType.USER),
                eq("u-1"),
                eq(null),
                any(),
                any(),
                eq(AuditStatus.SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("updateUserRoles throws BusinessRuleException when administrator tries to remove ROLE_ADMIN from self")
    void updateUserRoles_SelfDemotion_ThrowsException() {
        User adminUser = new User("u-admin", "superadmin", "admin@careflow.local", "hash", "Super", "Admin", "+1-555");
        when(userRepository.findById("u-admin")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> administrationService.updateUserRoles(
                "u-admin",
                new UpdateUserRolesRequest(Set.of(RoleType.ROLE_DOCTOR))
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot remove the administrator role from their own account");
    }

    @Test
    @DisplayName("resetUserPassword hashes password and resets failed attempts")
    void resetUserPassword_Success_EncodesPassword() {
        User user = new User("u-1", "john.doe", "john@careflow.local", "old-hash", "John", "Doe", "+1-555");
        user.setFailedLoginAttempts(3);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecret@123")).thenReturn("new-bCrypt-hash");

        administrationService.resetUserPassword("u-1", new AdminResetPasswordRequest("NewSecret@123"));

        assertThat(user.getPasswordHash()).isEqualTo("new-bCrypt-hash");
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository).save(user);
        verify(auditService).recordSensitiveAccess(
                eq("superadmin"),
                eq(AuditAction.USER_PASSWORD_RESET),
                eq(AuditResourceType.USER),
                eq("u-1"),
                eq(null),
                eq(null),
                eq(null),
                eq(AuditStatus.SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("getMaintenanceStatus returns current maintenance state")
    void getMaintenanceStatus_ReturnsState() {
        SystemSetting modeSetting = new SystemSetting("1", "careflow.system.maintenance_mode", "true", SettingCategory.MAINTENANCE, SettingDataType.BOOLEAN, "desc", false, true);
        SystemSetting reasonSetting = new SystemSetting("2", "careflow.system.maintenance_reason", "Database upgrade", SettingCategory.MAINTENANCE, SettingDataType.STRING, "desc", false, true);

        when(systemSettingRepository.findBySettingKey("careflow.system.maintenance_mode")).thenReturn(Optional.of(modeSetting));
        when(systemSettingRepository.findBySettingKey("careflow.system.maintenance_reason")).thenReturn(Optional.of(reasonSetting));

        MaintenanceModeResponse response = administrationService.getMaintenanceStatus();

        assertThat(response.enabled()).isTrue();
        assertThat(response.reason()).isEqualTo("Database upgrade");
    }

    @Test
    @DisplayName("setMaintenanceMode enables maintenance and audits action")
    void setMaintenanceMode_Success_UpdatesAndAudits() {
        when(systemSettingRepository.findBySettingKey("careflow.system.maintenance_mode")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("careflow.system.maintenance_reason")).thenReturn(Optional.empty());

        MaintenanceModeResponse response = administrationService.setMaintenanceMode(
                new MaintenanceModeRequest(true, "Routine server patch")
        );

        assertThat(response.enabled()).isTrue();
        assertThat(response.reason()).isEqualTo("Routine server patch");
        verify(auditService).recordSensitiveAccess(
                eq("superadmin"),
                eq(AuditAction.CONFIGURATION_CHANGED),
                eq(AuditResourceType.SYSTEM),
                any(),
                eq(null),
                any(),
                eq("true"),
                eq(AuditStatus.SUCCESS),
                any()
        );
    }

    @Test
    @DisplayName("getAdminOverview calculates administrative telemetry metrics correctly")
    void getAdminOverview_CalculatesMetrics() {
        when(userRepository.count()).thenReturn(150L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(140L);
        when(userRepository.countByStatus(UserStatus.LOCKED)).thenReturn(5L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(5L);

        List<Object[]> rolesData = Collections.singletonList(new Object[]{RoleType.ROLE_DOCTOR, 35L});
        when(userRepository.countUsersByRole()).thenReturn(rolesData);

        when(departmentRepository.count()).thenReturn(8L);
        when(staffMemberRepository.count()).thenReturn(50L);
        when(systemSettingRepository.count()).thenReturn(10L);

        when(systemMetadataRepository.findByMetadataKey("schema.version"))
                .thenReturn(Optional.of(new SystemMetadata("meta-1", "schema.version", "1.0.0")));

        AdminOverviewResponse overview = administrationService.getAdminOverview();

        assertThat(overview.totalUsers()).isEqualTo(150L);
        assertThat(overview.activeUsers()).isEqualTo(140L);
        assertThat(overview.lockedUsers()).isEqualTo(5L);
        assertThat(overview.suspendedUsers()).isEqualTo(5L);
        assertThat(overview.usersByRole()).containsEntry("ROLE_DOCTOR", 35L);
        assertThat(overview.totalDepartments()).isEqualTo(8L);
        assertThat(overview.totalStaff()).isEqualTo(50L);
        assertThat(overview.totalSettings()).isEqualTo(10L);
        assertThat(overview.systemVersion()).isEqualTo("1.0.0");
    }
}
