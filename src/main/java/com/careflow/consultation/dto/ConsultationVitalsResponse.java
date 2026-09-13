package com.careflow.consultation.dto;

import java.math.BigDecimal;

/**
 * Response representation for vital signs measurements (§22).
 */
public record ConsultationVitalsResponse(
        Integer systolicBp,
        Integer diastolicBp,
        Integer heartRate,
        Integer respiratoryRate,
        BigDecimal temperatureCelsius,
        Integer oxygenSaturation,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi
) {
}
