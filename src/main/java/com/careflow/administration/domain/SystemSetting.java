package com.careflow.administration.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Enterprise system configuration setting entity (§38).
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "setting_key", length = 128, nullable = false, unique = true)
    private String settingKey;

    @Column(name = "setting_value", length = 1024, nullable = false)
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64, nullable = false)
    private SettingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 32, nullable = false)
    private SettingDataType dataType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_encrypted", nullable = false)
    private boolean encrypted = false;

    @Column(name = "is_editable", nullable = false)
    private boolean editable = true;

    public SystemSetting() {
    }

    public SystemSetting(String id,
                         String settingKey,
                         String settingValue,
                         SettingCategory category,
                         SettingDataType dataType,
                         String description,
                         boolean encrypted,
                         boolean editable) {
        this.id = id;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.category = category;
        this.dataType = dataType;
        this.description = description;
        this.encrypted = encrypted;
        this.editable = editable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public SettingCategory getCategory() {
        return category;
    }

    public void setCategory(SettingCategory category) {
        this.category = category;
    }

    public SettingDataType getDataType() {
        return dataType;
    }

    public void setDataType(SettingDataType dataType) {
        this.dataType = dataType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSetting that = (SystemSetting) o;
        return Objects.equals(id, that.id) || Objects.equals(settingKey, that.settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingKey);
    }
}
