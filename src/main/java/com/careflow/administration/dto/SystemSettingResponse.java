package com.careflow.administration.dto;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SettingDataType;

import java.time.Instant;

/**
 * DTO for displaying a system configuration setting (§38, §40).
 */
public record SystemSettingResponse(
        String id,
        String settingKey,
        String settingValue,
        SettingCategory category,
        SettingDataType dataType,
        String description,
        boolean encrypted,
        boolean editable,
        Instant updatedAt,
        String updatedBy
) {}
