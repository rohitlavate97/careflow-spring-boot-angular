package com.careflow.administration.mapper;

import com.careflow.administration.domain.SystemSetting;
import com.careflow.administration.dto.AdminUserDetailResponse;
import com.careflow.administration.dto.AdminUserSummaryResponse;
import com.careflow.administration.dto.SystemSettingResponse;
import com.careflow.identity.domain.Permission;
import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for Administration module entities and DTOs (§38, §40).
 */
@Component
public class SystemSettingMapper {

    public SystemSettingResponse toResponse(SystemSetting setting) {
        if (setting == null) return null;
        return new SystemSettingResponse(
                setting.getId(),
                setting.getSettingKey(),
                setting.isEncrypted() ? "********" : setting.getSettingValue(),
                setting.getCategory(),
                setting.getDataType(),
                setting.getDescription(),
                setting.isEncrypted(),
                setting.isEditable(),
                setting.getUpdatedAt(),
                setting.getUpdatedBy()
        );
    }

    public AdminUserSummaryResponse toUserSummary(User user) {
        if (user == null) return null;
        Set<RoleType> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                roles,
                user.getCreatedAt()
        );
    }

    public AdminUserDetailResponse toUserDetail(User user) {
        if (user == null) return null;
        Set<RoleType> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> permissions = user.getRoles() != null
                ? user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                roles,
                permissions,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
    }
}
