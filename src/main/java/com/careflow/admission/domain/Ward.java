package com.careflow.admission.domain;

import com.careflow.common.domain.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Hospital ward aggregate grouping inpatient rooms and clinical care beds (§28).
 */
@Entity
@Table(
        name = "wards",
        indexes = {
                @Index(name = "idx_wards_department", columnList = "department_id"),
                @Index(name = "idx_wards_type", columnList = "ward_type"),
                @Index(name = "idx_wards_active", columnList = "active")
        }
)
public class Ward extends BaseAuditEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "ward_code", length = 32, nullable = false, unique = true)
    private String wardCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "department_id", length = 64, nullable = false)
    private String departmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ward_type", length = 50, nullable = false)
    private WardType wardType;

    @Column(name = "floor", length = 50)
    private String floor;

    @Column(name = "total_beds", nullable = false)
    private int totalBeds = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Room> rooms = new LinkedHashSet<>();

    protected Ward() {
    }

    public Ward(String id,
                String wardCode,
                String name,
                String departmentId,
                WardType wardType,
                String floor,
                int totalBeds,
                boolean active) {
        this.id = id;
        this.wardCode = wardCode;
        this.name = name;
        this.departmentId = departmentId;
        this.wardType = wardType;
        this.floor = floor;
        this.totalBeds = totalBeds;
        this.active = active;
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
        room.setWard(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public WardType getWardType() {
        return wardType;
    }

    public void setWardType(WardType wardType) {
        this.wardType = wardType;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(int totalBeds) {
        this.totalBeds = totalBeds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Room> getRooms() {
        return Collections.unmodifiableSet(rooms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ward ward)) return false;
        return Objects.equals(id, ward.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
