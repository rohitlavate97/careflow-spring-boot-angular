package com.careflow.consultation.domain;

import com.careflow.common.domain.BaseAuditEntity;
import com.careflow.consultation.exception.ConsultationLockedException;
import com.careflow.consultation.exception.InvalidConsultationStatusTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Consultation encounter aggregate root managing patient evaluation, clinical findings,
 * vital signs, diagnostic formulations, and encounter lifecycle transitions (§22, §69, §103 Phase 7).
 */
@Entity
@Table(
        name = "consultations",
        indexes = {
                @Index(name = "idx_consultation_patient", columnList = "patient_id"),
                @Index(name = "idx_consultation_doctor", columnList = "doctor_id"),
                @Index(name = "idx_consultation_status", columnList = "status"),
                @Index(name = "idx_consultation_started", columnList = "started_at"),
                @Index(name = "idx_consultation_appointment", columnList = "appointment_id"),
                @Index(name = "idx_consultation_queue", columnList = "queue_entry_id")
        }
)
public class Consultation extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "appointment_id", length = 64)
    private String appointmentId;

    @Column(name = "queue_entry_id", length = 64)
    private String queueEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ConsultationStatus status = ConsultationStatus.STARTED;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "history_of_present_illness", columnDefinition = "TEXT")
    private String historyOfPresentIllness;

    @Column(name = "physical_examination", columnDefinition = "TEXT")
    private String physicalExamination;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;

    @Embedded
    private ConsultationVitals vitals;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ConsultationDiagnosis> diagnoses = new LinkedHashSet<>();

    public Consultation() {
    }

    public Consultation(String id,
                        String patientId,
                        String doctorId,
                        String appointmentId,
                        String queueEntryId,
                        Instant startedAt) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentId = appointmentId;
        this.queueEntryId = queueEntryId;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.status = ConsultationStatus.STARTED;
        this.vitals = new ConsultationVitals();
    }

    /**
     * Executes a guarded state transition on the consultation aggregate (§69).
     *
     * @param targetStatus Target lifecycle state
     */
    public void transitionTo(ConsultationStatus targetStatus) {
        if (this.status == targetStatus) {
            return;
        }

        if (this.status == ConsultationStatus.COMPLETED || this.status == ConsultationStatus.CANCELLED) {
            throw new InvalidConsultationStatusTransitionException(this.id, this.status, targetStatus);
        }

        switch (targetStatus) {
            case IN_PROGRESS -> {
                if (this.status != ConsultationStatus.STARTED) {
                    throw new InvalidConsultationStatusTransitionException(this.id, this.status, targetStatus);
                }
                this.status = ConsultationStatus.IN_PROGRESS;
            }
            case COMPLETED -> {
                this.status = ConsultationStatus.COMPLETED;
                this.completedAt = Instant.now();
            }
            case CANCELLED -> {
                this.status = ConsultationStatus.CANCELLED;
                this.completedAt = Instant.now();
            }
            default -> throw new InvalidConsultationStatusTransitionException(this.id, this.status, targetStatus);
        }
    }

    /**
     * Asserts that this consultation encounter has not been finalized or cancelled (§22, §23).
     */
    public void assertEditable() {
        if (this.status == ConsultationStatus.COMPLETED || this.status == ConsultationStatus.CANCELLED) {
            throw new ConsultationLockedException(this.id, this.status);
        }
    }

    public void addDiagnosis(ConsultationDiagnosis diagnosis) {
        assertEditable();
        this.diagnoses.add(diagnosis);
        diagnosis.setConsultation(this);
    }

    public void removeDiagnosis(String diagnosisId) {
        assertEditable();
        this.diagnoses.removeIf(d -> Objects.equals(d.getId(), diagnosisId));
    }

    public void updateVitals(ConsultationVitals newVitals) {
        assertEditable();
        this.vitals = newVitals;
    }

    public void updateClinicalFindings(String chiefComplaint,
                                       String historyOfPresentIllness,
                                       String physicalExamination,
                                       String treatmentPlan,
                                       LocalDate followUpDate,
                                       String followUpInstructions) {
        assertEditable();
        this.chiefComplaint = chiefComplaint;
        this.historyOfPresentIllness = historyOfPresentIllness;
        this.physicalExamination = physicalExamination;
        this.treatmentPlan = treatmentPlan;
        this.followUpDate = followUpDate;
        this.followUpInstructions = followUpInstructions;
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

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getQueueEntryId() {
        return queueEntryId;
    }

    public void setQueueEntryId(String queueEntryId) {
        this.queueEntryId = queueEntryId;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getHistoryOfPresentIllness() {
        return historyOfPresentIllness;
    }

    public void setHistoryOfPresentIllness(String historyOfPresentIllness) {
        this.historyOfPresentIllness = historyOfPresentIllness;
    }

    public String getPhysicalExamination() {
        return physicalExamination;
    }

    public void setPhysicalExamination(String physicalExamination) {
        this.physicalExamination = physicalExamination;
    }

    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getFollowUpInstructions() {
        return followUpInstructions;
    }

    public void setFollowUpInstructions(String followUpInstructions) {
        this.followUpInstructions = followUpInstructions;
    }

    public ConsultationVitals getVitals() {
        return vitals;
    }

    public void setVitals(ConsultationVitals vitals) {
        this.vitals = vitals;
    }

    public Set<ConsultationDiagnosis> getDiagnoses() {
        return Collections.unmodifiableSet(diagnoses);
    }

    public void setDiagnoses(Set<ConsultationDiagnosis> diagnoses) {
        this.diagnoses = diagnoses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consultation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
