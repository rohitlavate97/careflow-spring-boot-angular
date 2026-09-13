package com.careflow.clinical.dto;

import com.careflow.clinical.domain.NoteType;

import java.time.Instant;

/**
 * Response representation for a clinical documentation note (§23, §40).
 */
public record ClinicalNoteResponse(
        String id,
        String patientId,
        String consultationId,
        String authorId,
        NoteType noteType,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
