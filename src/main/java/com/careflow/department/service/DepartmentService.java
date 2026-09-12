package com.careflow.department.service;

import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.CreateDepartmentRequest;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.dto.DepartmentSummaryResponse;
import com.careflow.department.dto.UpdateDepartmentRequest;
import com.careflow.department.dto.UpdateDepartmentStatusRequest;

import java.util.List;

/**
 * Service boundary managing hospital departments and organizational units (§13, §17).
 */
public interface DepartmentService {

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    DepartmentResponse getDepartmentById(String id);

    DepartmentResponse getDepartmentByCode(String code);

    List<DepartmentResponse> getAllDepartments(DepartmentStatus status);

    List<DepartmentSummaryResponse> searchDepartments(String query);

    DepartmentResponse updateDepartment(String id, UpdateDepartmentRequest request);

    DepartmentResponse updateDepartmentStatus(String id, UpdateDepartmentStatusRequest request);

    void deactivateDepartment(String id);
}
