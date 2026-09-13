package com.careflow.consultation.dto;

import java.time.LocalDate;

/**
 * Payload for updating clinical evaluation findings during a consultation (§22).
 */
public record UpdateConsultationRequest(
        String chiefComplaint,
        String historyOfPresentIllness,
        String physicalExamination,
        String treatmentPlan,
        LocalDate followUpDate,
        String followUpInstructions
) {
}
