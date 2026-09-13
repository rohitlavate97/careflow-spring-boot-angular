package com.careflow.scheduling.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.DoctorLeaveResponse;
import com.careflow.scheduling.dto.UpdateDoctorLeaveStatusRequest;
import org.springframework.data.domain.Pageable;

/**
 * Service boundary managing doctor leave requests, holiday exceptions, and approvals (§13, §18).
 */
public interface DoctorLeaveService {

    DoctorLeaveResponse createLeave(String doctorId, CreateDoctorLeaveRequest request);

    DoctorLeaveResponse getLeaveById(String id);

    PageResponse<DoctorLeaveResponse> getDoctorLeaves(String doctorId, Pageable pageable);

    DoctorLeaveResponse updateLeaveStatus(String id, UpdateDoctorLeaveStatusRequest request);
}
