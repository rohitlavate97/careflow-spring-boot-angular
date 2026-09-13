package com.careflow.admission.dto;

import com.careflow.admission.domain.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a room inside a ward (§28).
 */
public record CreateRoomRequest(
        @NotBlank(message = "Room number is required")
        @Size(max = 32, message = "Room number must not exceed 32 characters")
        String roomNumber,

        @NotBlank(message = "Ward ID is required")
        String wardId,

        @NotNull(message = "Room type is required")
        RoomType roomType
) {
}
