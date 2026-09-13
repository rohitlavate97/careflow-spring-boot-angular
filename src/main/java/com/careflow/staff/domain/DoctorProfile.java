package com.careflow.staff.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Specialized clinical credentials and profile for medical doctors (§17, §18).
 */
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    private StaffMember staffMember;

    @Column(name = "specialization", length = 100, nullable = false)
    private String specialization;

    @Column(name = "qualifications", length = 255, nullable = false)
    private String qualifications;

    @Column(name = "medical_license_number", length = 100, nullable = false, unique = true)
    private String medicalLicenseNumber;

    @Column(name = "consultation_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal consultationFee;

    @Column(name = "consultation_room", length = 50)
    private String consultationRoom;

    @Column(name = "bio", length = 1000)
    private String bio;

    public DoctorProfile() {
    }

    public DoctorProfile(String id,
                         String specialization,
                         String qualifications,
                         String medicalLicenseNumber,
                         BigDecimal consultationFee,
                         String consultationRoom,
                         String bio) {
        this.id = id;
        this.specialization = specialization != null ? specialization.trim() : null;
        this.qualifications = qualifications != null ? qualifications.trim() : null;
        this.medicalLicenseNumber = medicalLicenseNumber != null ? medicalLicenseNumber.trim().toUpperCase() : null;
        this.consultationFee = consultationFee;
        this.consultationRoom = consultationRoom != null ? consultationRoom.trim() : null;
        this.bio = bio != null ? bio.trim() : null;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StaffMember getStaffMember() {
        return staffMember;
    }

    public void setStaffMember(StaffMember staffMember) {
        this.staffMember = staffMember;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization != null ? specialization.trim() : null;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications != null ? qualifications.trim() : null;
    }

    public String getMedicalLicenseNumber() {
        return medicalLicenseNumber;
    }

    public void setMedicalLicenseNumber(String medicalLicenseNumber) {
        this.medicalLicenseNumber = medicalLicenseNumber != null ? medicalLicenseNumber.trim().toUpperCase() : null;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getConsultationRoom() {
        return consultationRoom;
    }

    public void setConsultationRoom(String consultationRoom) {
        this.consultationRoom = consultationRoom != null ? consultationRoom.trim() : null;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio != null ? bio.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorProfile that)) return false;
        return Objects.equals(medicalLicenseNumber, that.medicalLicenseNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicalLicenseNumber);
    }
}
