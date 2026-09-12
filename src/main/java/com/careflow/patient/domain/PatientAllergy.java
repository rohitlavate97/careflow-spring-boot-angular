package com.careflow.patient.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain entity representing a documented patient allergy and clinical alert (§16).
 */
@Entity
@Table(name = "patient_allergies")
public class PatientAllergy extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "allergen", length = 100, nullable = false)
    private String allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private AllergenCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 30, nullable = false)
    private AllergySeverity severity;

    @Column(name = "reaction", length = 255)
    private String reaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AllergyStatus status = AllergyStatus.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    public PatientAllergy() {
    }

    public PatientAllergy(String id, String patientId, String allergen,
                          AllergenCategory category, AllergySeverity severity,
                          String reaction, AllergyStatus status, String notes,
                          LocalDate diagnosedDate) {
        this.id = id;
        this.patientId = patientId;
        this.allergen = allergen;
        this.category = category;
        this.severity = severity;
        this.reaction = reaction;
        this.status = status != null ? status : AllergyStatus.ACTIVE;
        this.notes = notes;
        this.diagnosedDate = diagnosedDate;
    }

    // Business Methods (§69)

    public boolean isHighRisk() {
        return this.status == AllergyStatus.ACTIVE &&
                (this.severity == AllergySeverity.SEVERE || this.severity == AllergySeverity.LIFE_THREATENING);
    }

    public void deactivate() {
        this.status = AllergyStatus.INACTIVE;
    }

    public void resolve() {
        this.status = AllergyStatus.RESOLVED;
    }

    public void reactivate() {
        this.status = AllergyStatus.ACTIVE;
    }

    // Getters and Setters

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

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

    public AllergenCategory getCategory() {
        return category;
    }

    public void setCategory(AllergenCategory category) {
        this.category = category;
    }

    public AllergySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AllergySeverity severity) {
        this.severity = severity;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public AllergyStatus getStatus() {
        return status;
    }

    public void setStatus(AllergyStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDiagnosedDate() {
        return diagnosedDate;
    }

    public void setDiagnosedDate(LocalDate diagnosedDate) {
        this.diagnosedDate = diagnosedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientAllergy that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
