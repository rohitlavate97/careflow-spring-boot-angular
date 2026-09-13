package com.careflow.insurance.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Health insurance payer entity maintaining third-party clearinghouse directory details (§33).
 */
@Entity
@Table(
        name = "insurance_providers",
        indexes = {
                @Index(name = "idx_insurance_providers_payer_id", columnList = "payer_id"),
                @Index(name = "idx_insurance_providers_active", columnList = "active")
        }
)
public class InsuranceProvider extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "provider_code", length = 50, nullable = false, unique = true)
    private String providerCode;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "payer_id", length = 64, nullable = false)
    private String payerId;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected InsuranceProvider() {
    }

    public InsuranceProvider(String id,
                             String providerCode,
                             String name,
                             String payerId,
                             String contactEmail,
                             String contactPhone,
                             String address,
                             boolean active) {
        this.id = id;
        this.providerCode = providerCode;
        this.name = name;
        this.payerId = payerId;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.address = address;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsuranceProvider that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
