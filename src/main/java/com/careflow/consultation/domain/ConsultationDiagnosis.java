package com.careflow.consultation.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Clinical diagnosis established and recorded during a consultation encounter (§22).
 */
@Entity
@Table(
        name = "consultation_diagnoses",
        indexes = {
                @Index(name = "idx_diagnosis_consultation", columnList = "consultation_id"),
                @Index(name = "idx_diagnosis_code", columnList = "diagnosis_code")
        }
)
public class ConsultationDiagnosis extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(name = "diagnosis_code", length = 32, nullable = false)
    private String diagnosisCode;

    @Column(name = "diagnosis_name", length = 255, nullable = false)
    private String diagnosisName;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", length = 30, nullable = false)
    private DiagnosisType diagnosisType = DiagnosisType.PRIMARY;

    @Column(name = "severity", length = 30)
    private String severity;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public ConsultationDiagnosis() {
    }

    public ConsultationDiagnosis(String id,
                                 Consultation consultation,
                                 String diagnosisCode,
                                 String diagnosisName,
                                 DiagnosisType diagnosisType,
                                 String severity,
                                 String notes) {
        this.id = id;
        this.consultation = consultation;
        this.diagnosisCode = diagnosisCode;
        this.diagnosisName = diagnosisName;
        this.diagnosisType = diagnosisType != null ? diagnosisType : DiagnosisType.PRIMARY;
        this.severity = severity;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public void setDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public DiagnosisType getDiagnosisType() {
        return diagnosisType;
    }

    public void setDiagnosisType(DiagnosisType diagnosisType) {
        this.diagnosisType = diagnosisType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultationDiagnosis that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
