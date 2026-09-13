package com.careflow.admission.domain;

import com.careflow.admission.exception.BedNotAvailableException;
import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Hospital physical bed entity with real-time operational availability status (§28, §57 Lab 4).
 */
@Entity
@Table(
        name = "beds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_beds_room_number", columnNames = {"room_id", "bed_number"})
        },
        indexes = {
                @Index(name = "idx_beds_room", columnList = "room_id"),
                @Index(name = "idx_beds_status", columnList = "status"),
                @Index(name = "idx_beds_active", columnList = "active")
        }
)
public class Bed extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "bed_number", length = 32, nullable = false)
    private String bedNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private BedStatus status = BedStatus.AVAILABLE;

    @Column(name = "daily_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal dailyRate = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Bed() {
    }

    public Bed(String id, String bedNumber, Room room, BedStatus status, BigDecimal dailyRate, boolean active) {
        this.id = id;
        this.bedNumber = bedNumber;
        this.room = room;
        this.status = status != null ? status : BedStatus.AVAILABLE;
        this.dailyRate = dailyRate != null ? dailyRate : BigDecimal.ZERO;
        this.active = active;
    }

    public void allocate() {
        if (this.status != BedStatus.AVAILABLE) {
            throw new BedNotAvailableException(this.id, this.status.name());
        }
        this.status = BedStatus.OCCUPIED;
    }

    public void release() {
        this.status = BedStatus.AVAILABLE;
    }

    public void reserve() {
        if (this.status != BedStatus.AVAILABLE) {
            throw new BedNotAvailableException(this.id, this.status.name());
        }
        this.status = BedStatus.RESERVED;
    }

    public void setMaintenance() {
        this.status = BedStatus.MAINTENANCE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
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
        if (!(o instanceof Bed bed)) return false;
        return Objects.equals(id, bed.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
