package com.careflow.administration.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing core system initialization metadata.
 */
@Entity
@Table(name = "system_metadata")
public class SystemMetadata extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "metadata_key", length = 128, nullable = false, unique = true)
    private String metadataKey;

    @Column(name = "metadata_value", length = 512, nullable = false)
    private String metadataValue;

    public SystemMetadata() {
    }

    public SystemMetadata(String id, String metadataKey, String metadataValue) {
        this.id = id;
        this.metadataKey = metadataKey;
        this.metadataValue = metadataValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }
}
