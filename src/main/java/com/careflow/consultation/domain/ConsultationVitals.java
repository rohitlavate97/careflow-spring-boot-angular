package com.careflow.consultation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Clinical vital signs captured during patient triage or consultation encounter (§22).
 */
@Embeddable
public class ConsultationVitals {

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "temperature_celsius", precision = 4, scale = 1)
    private BigDecimal temperatureCelsius;

    @Column(name = "oxygen_saturation")
    private Integer oxygenSaturation;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "bmi", precision = 4, scale = 1)
    private BigDecimal bmi;

    public ConsultationVitals() {
    }

    public ConsultationVitals(Integer systolicBp,
                              Integer diastolicBp,
                              Integer heartRate,
                              Integer respiratoryRate,
                              BigDecimal temperatureCelsius,
                              Integer oxygenSaturation,
                              BigDecimal heightCm,
                              BigDecimal weightKg) {
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.heartRate = heartRate;
        this.respiratoryRate = respiratoryRate;
        this.temperatureCelsius = temperatureCelsius;
        this.oxygenSaturation = oxygenSaturation;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bmi = calculateBmi(heightCm, weightKg);
    }

    public static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        if (heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // height in meters = heightCm / 100
        BigDecimal heightMeters = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightMeters.multiply(heightMeters);
        if (heightSquared.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightKg.divide(heightSquared, 1, RoundingMode.HALF_UP);
    }

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(BigDecimal temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Integer getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(Integer oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
        this.bmi = calculateBmi(this.heightCm, this.weightKg);
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
        this.bmi = calculateBmi(this.heightCm, this.weightKg);
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultationVitals that)) return false;
        return Objects.equals(systolicBp, that.systolicBp) &&
                Objects.equals(diastolicBp, that.diastolicBp) &&
                Objects.equals(heartRate, that.heartRate) &&
                Objects.equals(respiratoryRate, that.respiratoryRate) &&
                Objects.equals(temperatureCelsius, that.temperatureCelsius) &&
                Objects.equals(oxygenSaturation, that.oxygenSaturation) &&
                Objects.equals(heightCm, that.heightCm) &&
                Objects.equals(weightKg, that.weightKg) &&
                Objects.equals(bmi, that.bmi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systolicBp, diastolicBp, heartRate, respiratoryRate,
                temperatureCelsius, oxygenSaturation, heightCm, weightKg, bmi);
    }
}
