package com.careflow.admission.dto;

import com.careflow.admission.domain.RoomType;

import java.util.List;

/**
 * Hospital room response DTO (§28).
 */
public record RoomResponse(
        String id,
        String roomNumber,
        String wardId,
        RoomType roomType,
        boolean active,
        List<BedResponse> beds
) {
}
