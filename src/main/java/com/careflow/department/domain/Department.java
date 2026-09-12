package com.careflow.department.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Hospital department entity representing an organizational and clinical unit (§17).
 */
@Entity
@Table(name = "departments")
public class Department extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "head_of_department_id", length = 64)
    private String headOfDepartmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    public Department() {
    }

    public Department(String id, String code, String name, String description, String location) {
        this.id = id;
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.name = name != null ? name.trim() : null;
        this.description = description != null ? description.trim() : null;
        this.location = location != null ? location.trim() : null;
        this.status = DepartmentStatus.ACTIVE;
    }

    // Business Methods (§69)

    public void activate() {
        this.status = DepartmentStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = DepartmentStatus.INACTIVE;
    }

    public void suspend() {
        this.status = DepartmentStatus.SUSPENDED;
    }

    public void assignHead(String headOfDepartmentId) {
        this.headOfDepartmentId = headOfDepartmentId;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail != null ? contactEmail.trim().toLowerCase() : null;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHeadOfDepartmentId() {
        return headOfDepartmentId;
    }

    public void setHeadOfDepartmentId(String headOfDepartmentId) {
        this.headOfDepartmentId = headOfDepartmentId;
    }

    public DepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(DepartmentStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
