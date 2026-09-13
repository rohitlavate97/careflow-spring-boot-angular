package com.careflow.staff.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorSummaryResponse;
import com.careflow.staff.dto.StaffResponse;
import com.careflow.staff.dto.StaffSummaryResponse;
import com.careflow.staff.dto.UpdateStaffRequest;
import com.careflow.staff.dto.UpdateStaffStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service boundary managing hospital staff profiles, medical specializations, and employment status (§13, §17).
 */
public interface StaffService {

    StaffResponse createStaff(CreateStaffRequest request);

    StaffResponse getStaffById(String id);

    StaffResponse getStaffByCode(String staffCode);

    StaffResponse getStaffByUserId(String userId);

    PageResponse<StaffSummaryResponse> searchStaff(
            String query,
            String departmentId,
            StaffType staffType,
            StaffStatus status,
            Pageable pageable
    );

    StaffResponse updateStaff(String id, UpdateStaffRequest request);

    StaffResponse updateStaffStatus(String id, UpdateStaffStatusRequest request);

    PageResponse<DoctorSummaryResponse> searchDoctors(
            String specialization,
            String departmentId,
            Pageable pageable
    );

    List<DoctorSummaryResponse> getActiveDoctors(
            String specialization,
            String departmentId
    );
}
