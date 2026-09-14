package com.careflow.administration.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Summary telemetry and operational metrics for administrator console dashboard (§38).
 */
public record AdminOverviewResponse(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long suspendedUsers,
        Map<String, Long> usersByRole,
        long totalDepartments,
        long totalStaff,
        long totalSettings,
        boolean maintenanceMode,
        String maintenanceReason,
        String systemVersion,
        String facilityName,
        Instant serverTime
) {}
