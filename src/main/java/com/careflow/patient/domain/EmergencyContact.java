package com.careflow.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Embeddable value object representing an emergency contact person (§11).
 */
@Embeddable
public class EmergencyContact {

    @Column(name = "emergency_contact_name", length = 100)
    private String name;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String relationship;

    @Column(name = "emergency_contact_phone", length = 20)
    private String phone;

    public EmergencyContact() {
    }

    public EmergencyContact(String name, String relationship, String phone) {
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmergencyContact that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, phone);
    }
}
