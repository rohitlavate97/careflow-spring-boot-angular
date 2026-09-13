package com.careflow.consultation.mapper;

import com.careflow.consultation.domain.Consultation;
import com.careflow.consultation.domain.ConsultationDiagnosis;
import com.careflow.consultation.domain.ConsultationVitals;
import com.careflow.consultation.domain.DiagnosisType;
import com.careflow.consultation.dto.ConsultationDiagnosisResponse;
import com.careflow.consultation.dto.ConsultationResponse;
import com.careflow.consultation.dto.ConsultationSummaryResponse;
import com.careflow.consultation.dto.ConsultationVitalsResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Consultation domain entities and DTOs (§22, §40, §90).
 */
@Component
public class ConsultationMapper {

    public ConsultationVitalsResponse toVitalsResponse(ConsultationVitals vitals) {
        if (vitals == null) {
            return new ConsultationVitalsResponse(null, null, null, null, null, null, null, null, null);
        }
        return new ConsultationVitalsResponse(
                vitals.getSystolicBp(),
                vitals.getDiastolicBp(),
                vitals.getHeartRate(),
                vitals.getRespiratoryRate(),
                vitals.getTemperatureCelsius(),
                vitals.getOxygenSaturation(),
                vitals.getHeightCm(),
                vitals.getWeightKg(),
                vitals.getBmi()
        );
    }

    public ConsultationDiagnosisResponse toDiagnosisResponse(ConsultationDiagnosis diagnosis) {
        if (diagnosis == null) {
            return null;
        }
        return new ConsultationDiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getDiagnosisCode(),
                diagnosis.getDiagnosisName(),
                diagnosis.getDiagnosisType(),
                diagnosis.getSeverity(),
                diagnosis.getNotes(),
                diagnosis.getCreatedAt(),
                diagnosis.getCreatedBy()
        );
    }

    public List<ConsultationDiagnosisResponse> toDiagnosisResponseList(Iterable<ConsultationDiagnosis> diagnoses) {
        if (diagnoses == null) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(diagnoses.spliterator(), false)
                .map(this::toDiagnosisResponse)
                .toList();
    }

    public ConsultationResponse toResponse(Consultation consultation) {
        if (consultation == null) {
            return null;
        }

        return new ConsultationResponse(
                consultation.getId(),
                consultation.getPatientId(),
                consultation.getDoctorId(),
                consultation.getAppointmentId(),
                consultation.getQueueEntryId(),
                consultation.getStatus(),
                consultation.getStartedAt(),
                consultation.getCompletedAt(),
                consultation.getChiefComplaint(),
                consultation.getHistoryOfPresentIllness(),
                consultation.getPhysicalExamination(),
                consultation.getTreatmentPlan(),
                consultation.getFollowUpDate(),
                consultation.getFollowUpInstructions(),
                toVitalsResponse(consultation.getVitals()),
                toDiagnosisResponseList(consultation.getDiagnoses()),
                consultation.getCreatedAt(),
                consultation.getUpdatedAt(),
                consultation.getCreatedBy(),
                consultation.getUpdatedBy()
        );
    }

    public ConsultationSummaryResponse toSummaryResponse(Consultation consultation) {
        if (consultation == null) {
            return null;
        }

        String primaryCode = null;
        String primaryName = null;

        if (consultation.getDiagnoses() != null) {
            for (ConsultationDiagnosis d : consultation.getDiagnoses()) {
                if (d.getDiagnosisType() == DiagnosisType.PRIMARY) {
                    primaryCode = d.getDiagnosisCode();
                    primaryName = d.getDiagnosisName();
                    break;
                }
            }
        }

        return new ConsultationSummaryResponse(
                consultation.getId(),
                consultation.getPatientId(),
                consultation.getDoctorId(),
                consultation.getAppointmentId(),
                consultation.getQueueEntryId(),
                consultation.getStatus(),
                consultation.getStartedAt(),
                consultation.getCompletedAt(),
                consultation.getChiefComplaint(),
                primaryCode,
                primaryName
        );
    }
}
