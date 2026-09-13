package com.careflow.clinical.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Longitudinal clinical note entity preserving progressive patient documentation (§23, §103 Phase 7).
 */
@Entity
@Table(
        name = "clinical_notes",
        indexes = {
                @Index(name = "idx_clinical_note_patient", columnList = "patient_id"),
                @Index(name = "idx_clinical_note_consultation", columnList = "consultation_id"),
                @Index(name = "idx_clinical_note_author", columnList = "author_id"),
                @Index(name = "idx_clinical_note_created", columnList = "created_at")
        }
)
public class ClinicalNote extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "consultation_id", length = 64)
    private String consultationId;

    @Column(name = "author_id", length = 64, nullable = false)
    private String authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", length = 30, nullable = false)
    private NoteType noteType = NoteType.GENERAL;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    public ClinicalNote() {
    }

    public ClinicalNote(String id,
                        String patientId,
                        String consultationId,
                        String authorId,
                        NoteType noteType,
                        String title,
                        String content) {
        this.id = id;
        this.patientId = patientId;
        this.consultationId = consultationId;
        this.authorId = authorId;
        this.noteType = noteType != null ? noteType : NoteType.GENERAL;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(String consultationId) {
        this.consultationId = consultationId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClinicalNote that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
