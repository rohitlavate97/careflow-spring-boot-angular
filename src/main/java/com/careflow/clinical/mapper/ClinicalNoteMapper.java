package com.careflow.clinical.mapper;

import com.careflow.clinical.domain.ClinicalNote;
import com.careflow.clinical.dto.ClinicalNoteResponse;
import com.careflow.clinical.dto.CreateClinicalNoteRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for ClinicalNote domain entities and DTOs (§23, §40, §90).
 */
@Component
public class ClinicalNoteMapper {

    public ClinicalNote toEntity(String id, CreateClinicalNoteRequest request) {
        if (request == null) {
            return null;
        }

        return new ClinicalNote(
                id,
                request.patientId().trim(),
                request.consultationId() != null ? request.consultationId().trim() : null,
                request.authorId().trim(),
                request.noteType(),
                request.title().trim(),
                request.content().trim()
        );
    }

    public ClinicalNoteResponse toResponse(ClinicalNote note) {
        if (note == null) {
            return null;
        }

        return new ClinicalNoteResponse(
                note.getId(),
                note.getPatientId(),
                note.getConsultationId(),
                note.getAuthorId(),
                note.getNoteType(),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getCreatedBy(),
                note.getUpdatedBy()
        );
    }

    public List<ClinicalNoteResponse> toResponseList(List<ClinicalNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }
        return notes.stream()
                .map(this::toResponse)
                .toList();
    }
}
