package com.careflow.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log aggregate root (§36, §74, §103 Phase 15).
 * Represents an immutable, append-only compliance record tracking who accessed or mutated
 * protected health information (PHI) or critical operational entities in CareFlow.
 * Modification or deletion of audit logs is strictly prevented by JPA lifecycle listeners.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_actor_timestamp", columnList = "actor_user_id, timestamp"),
                @Index(name = "idx_audit_patient_timestamp", columnList = "patient_id, timestamp"),
                @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
                @Index(name = "idx_audit_action_timestamp", columnList = "action, timestamp"),
                @Index(name = "idx_audit_correlation_id", columnList = "correlation_id")
        }
)
public class AuditLog {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "actor_user_id", length = 100, nullable = false, updatable = false)
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 60, nullable = false, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 60, nullable = false, updatable = false)
    private AuditResourceType resourceType;

    @Column(name = "resource_id", length = 100, updatable = false)
    private String resourceId;

    @Column(name = "patient_id", length = 64, updatable = false)
    private String patientId;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "previous_value", columnDefinition = "TEXT", updatable = false)
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false, updatable = false)
    private AuditStatus status;

    @Column(name = "details", length = 1000, updatable = false)
    private String details;

    protected AuditLog() {
        // JPA requirement
    }

    public AuditLog(String id,
                    String actorUserId,
                    AuditAction action,
                    AuditResourceType resourceType,
                    String resourceId,
                    String patientId,
                    Instant timestamp,
                    String previousValue,
                    String newValue,
                    String ipAddress,
                    String correlationId,
                    AuditStatus status,
                    String details) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");
        this.resourceId = resourceId;
        this.patientId = patientId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
        this.status = status != null ? status : AuditStatus.SUCCESS;
        this.details = details;
    }

    public static AuditLog create(String actorUserId,
                                  AuditAction action,
                                  AuditResourceType resourceType,
                                  String resourceId,
                                  String patientId,
                                  String previousValue,
                                  String newValue,
                                  String ipAddress,
                                  String correlationId,
                                  AuditStatus status,
                                  String details) {
        return new AuditLog(
                UUID.randomUUID().toString(),
                actorUserId,
                action,
                resourceType,
                resourceId,
                patientId,
                Instant.now(),
                previousValue,
                newValue,
                ipAddress,
                correlationId,
                status != null ? status : AuditStatus.SUCCESS,
                details
        );
    }

    @PreUpdate
    public void onPreUpdate() {
        throw new UnsupportedOperationException("Audit logs are strictly immutable and cannot be modified.");
    }

    @PreRemove
    public void onPreRemove() {
        throw new UnsupportedOperationException("Audit logs are strictly immutable and cannot be deleted.");
    }

    public String getId() {
        return id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getPatientId() {
        return patientId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
