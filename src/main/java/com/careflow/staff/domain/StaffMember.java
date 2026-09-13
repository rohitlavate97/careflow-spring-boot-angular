package com.careflow.staff.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Hospital staff entity representing employees and clinical professionals (§17).
 */
@Entity
@Table(name = "staff_members")
public class StaffMember extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "staff_code", length = 32, nullable = false, unique = true)
    private String staffCode;

    @Column(name = "user_id", length = 64, unique = true)
    private String userId;

    @Column(name = "department_id", length = 64, nullable = false)
    private String departmentId;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", length = 30, nullable = false)
    private StaffType staffType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @OneToOne(mappedBy = "staffMember", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DoctorProfile doctorProfile;

    public StaffMember() {
    }

    public StaffMember(String id,
                       String staffCode,
                       String departmentId,
                       String firstName,
                       String lastName,
                       String email,
                       String phone,
                       StaffType staffType,
                       LocalDate dateOfJoining) {
        this.id = id;
        this.staffCode = staffCode != null ? staffCode.trim().toUpperCase() : null;
        this.departmentId = departmentId;
        this.firstName = firstName != null ? firstName.trim() : null;
        this.lastName = lastName != null ? lastName.trim() : null;
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.phone = phone != null ? phone.trim() : null;
        this.staffType = staffType;
        this.dateOfJoining = dateOfJoining;
        this.status = StaffStatus.ACTIVE;
    }

    // Business Methods (§69)

    public void transitionStatus(StaffStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition staff member %s from status %s to %s",
                            this.staffCode, this.status, targetStatus)
            );
        }
        this.status = targetStatus;
    }

    public void setDoctorProfile(DoctorProfile doctorProfile) {
        this.doctorProfile = doctorProfile;
        if (doctorProfile != null) {
            doctorProfile.setStaffMember(this);
        }
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode != null ? staffCode.trim().toUpperCase() : null;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : null;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public StaffType getStaffType() {
        return staffType;
    }

    public void setStaffType(StaffType staffType) {
        this.staffType = staffType;
    }

    public StaffStatus getStatus() {
        return status;
    }

    public void setStatus(StaffStatus status) {
        this.status = status;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public DoctorProfile getDoctorProfile() {
        return doctorProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffMember that)) return false;
        return Objects.equals(staffCode, that.staffCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffCode);
    }
}
