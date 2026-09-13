package com.careflow.consultation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * Payload for recording or updating patient vital signs during triage or consultation (§22).
 */
public record RecordVitalsRequest(
        @Min(value = 40, message = "Systolic blood pressure must be at least 40 mmHg")
        @Max(value = 300, message = "Systolic blood pressure cannot exceed 300 mmHg")
        Integer systolicBp,

        @Min(value = 20, message = "Diastolic blood pressure must be at least 20 mmHg")
        @Max(value = 200, message = "Diastolic blood pressure cannot exceed 200 mmHg")
        Integer diastolicBp,

        @Min(value = 20, message = "Heart rate must be at least 20 bpm")
        @Max(value = 300, message = "Heart rate cannot exceed 300 bpm")
        Integer heartRate,

        @Min(value = 5, message = "Respiratory rate must be at least 5 breaths/min")
        @Max(value = 60, message = "Respiratory rate cannot exceed 60 breaths/min")
        Integer respiratoryRate,

        @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0 °C")
        @DecimalMax(value = "45.0", message = "Temperature cannot exceed 45.0 °C")
        BigDecimal temperatureCelsius,

        @Min(value = 50, message = "Oxygen saturation must be at least 50%")
        @Max(value = 100, message = "Oxygen saturation cannot exceed 100%")
        Integer oxygenSaturation,

        @DecimalMin(value = "30.0", message = "Height must be at least 30.0 cm")
        @DecimalMax(value = "260.0", message = "Height cannot exceed 260.0 cm")
        BigDecimal heightCm,

        @DecimalMin(value = "1.0", message = "Weight must be at least 1.0 kg")
        @DecimalMax(value = "500.0", message = "Weight cannot exceed 500.0 kg")
        BigDecimal weightKg
) {
}
