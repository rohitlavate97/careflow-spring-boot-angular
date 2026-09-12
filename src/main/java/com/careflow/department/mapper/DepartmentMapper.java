package com.careflow.department.mapper;

import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.CreateDepartmentRequest;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.dto.DepartmentSummaryResponse;
import com.careflow.department.dto.UpdateDepartmentRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Department domain entities and DTOs (§89, §90).
 */
@Component
public class DepartmentMapper {

    public Department toEntity(String id, CreateDepartmentRequest request) {
        if (request == null) {
            return null;
        }

        Department department = new Department(
                id,
                request.code(),
                request.name(),
                request.description(),
                request.location()
        );

        if (request.contactPhone() != null) {
            department.setContactPhone(request.contactPhone().trim());
        }
        if (request.contactEmail() != null) {
            department.setContactEmail(request.contactEmail().trim().toLowerCase());
        }
        if (request.headOfDepartmentId() != null && !request.headOfDepartmentId().isBlank()) {
            department.setHeadOfDepartmentId(request.headOfDepartmentId().trim());
        }

        return department;
    }

    public void updateEntity(Department department, UpdateDepartmentRequest request) {
        if (department == null || request == null) {
            return;
        }

        department.setName(request.name().trim());
        department.setDescription(request.description() != null ? request.description().trim() : null);
        department.setContactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null);
        department.setContactEmail(request.contactEmail() != null ? request.contactEmail().trim().toLowerCase() : null);
        department.setLocation(request.location() != null ? request.location().trim() : null);
        department.setHeadOfDepartmentId(request.headOfDepartmentId() != null && !request.headOfDepartmentId().isBlank()
                ? request.headOfDepartmentId().trim() : null);
    }

    public DepartmentResponse toResponse(Department department) {
        if (department == null) {
            return null;
        }

        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getContactPhone(),
                department.getContactEmail(),
                department.getLocation(),
                department.getHeadOfDepartmentId(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt(),
                department.getVersion()
        );
    }

    public List<DepartmentResponse> toResponseList(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return Collections.emptyList();
        }
        return departments.stream()
                .map(this::toResponse)
                .toList();
    }

    public DepartmentSummaryResponse toSummaryResponse(Department department) {
        if (department == null) {
            return null;
        }

        return new DepartmentSummaryResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getLocation(),
                department.getStatus()
        );
    }

    public List<DepartmentSummaryResponse> toSummaryResponseList(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return Collections.emptyList();
        }
        return departments.stream()
                .map(this::toSummaryResponse)
                .toList();
    }
}
