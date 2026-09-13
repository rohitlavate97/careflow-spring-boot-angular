package com.careflow.scheduling.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.DoctorLeaveResponse;
import com.careflow.scheduling.dto.UpdateDoctorLeaveStatusRequest;
import com.careflow.scheduling.exception.DoctorLeaveNotFoundException;
import com.careflow.scheduling.exception.InvalidLeaveStatusTransitionException;
import com.careflow.scheduling.exception.LeaveOverlapException;
import com.careflow.scheduling.mapper.SchedulingMapper;
import com.careflow.scheduling.repository.DoctorLeaveRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of DoctorLeaveService managing leaves, schedule overrides, and status transitions (§13, §18).
 */
@Service
public class DoctorLeaveServiceImpl implements DoctorLeaveService {

    private static final Logger log = LoggerFactory.getLogger(DoctorLeaveServiceImpl.class);

    private final DoctorLeaveRepository doctorLeaveRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final SchedulingMapper schedulingMapper;

    public DoctorLeaveServiceImpl(DoctorLeaveRepository doctorLeaveRepository,
                                  StaffMemberRepository staffMemberRepository,
                                  SchedulingMapper schedulingMapper) {
        this.doctorLeaveRepository = doctorLeaveRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.schedulingMapper = schedulingMapper;
    }

    @Override
    @Transactional
    public DoctorLeaveResponse createLeave(String doctorId, CreateDoctorLeaveRequest request) {
        log.info("Creating leave request for doctor='{}', dates='{} to {}'",
                doctorId, request.startDate(), request.endDate());

        validateDoctor(doctorId);

        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessRuleException("INVALID_LEAVE_DATES",
                    "Leave start date (" + request.startDate() + ") cannot be after end date (" + request.endDate() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        List<DoctorLeave> overlappingLeaves = doctorLeaveRepository.findOverlappingActiveLeaves(
                doctorId, request.startDate(), request.endDate()
        );
        if (!overlappingLeaves.isEmpty()) {
            throw new LeaveOverlapException(
                    String.format("Doctor already has an active leave request overlapping the requested period (%s to %s)",
                            request.startDate(), request.endDate())
            );
        }

        String id = UUID.randomUUID().toString();
        DoctorLeave leave = schedulingMapper.toLeaveEntity(id, doctorId, request);
        DoctorLeave saved = doctorLeaveRepository.save(leave);

        log.info("Successfully created leave request id='{}' for doctor='{}'", saved.getId(), doctorId);
        return schedulingMapper.toLeaveResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorLeaveResponse getLeaveById(String id) {
        log.debug("Fetching leave request by id='{}'", id);
        DoctorLeave leave = doctorLeaveRepository.findById(id)
                .orElseThrow(() -> new DoctorLeaveNotFoundException(id));
        return schedulingMapper.toLeaveResponse(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorLeaveResponse> getDoctorLeaves(String doctorId, Pageable pageable) {
        log.debug("Fetching leaves for doctor='{}'", doctorId);
        validateDoctor(doctorId);
        Page<DoctorLeave> page = doctorLeaveRepository.findByDoctorId(doctorId, pageable);
        return PageResponse.from(page, schedulingMapper::toLeaveResponse);
    }

    @Override
    @Transactional
    public DoctorLeaveResponse updateLeaveStatus(String id, UpdateDoctorLeaveStatusRequest request) {
        log.info("Transitioning leave request id='{}' status to '{}'", id, request.status());

        DoctorLeave leave = doctorLeaveRepository.findById(id)
                .orElseThrow(() -> new DoctorLeaveNotFoundException(id));

        if (!leave.getStatus().canTransitionTo(request.status())) {
            log.warn("Invalid transition: Leave request id='{}' cannot transition from '{}' to '{}'",
                    id, leave.getStatus(), request.status());
            throw new InvalidLeaveStatusTransitionException(leave.getStatus(), request.status());
        }

        leave.transitionTo(request.status());
        DoctorLeave updated = doctorLeaveRepository.save(leave);

        log.info("Leave request id='{}' status transitioned to '{}'", updated.getId(), updated.getStatus());
        return schedulingMapper.toLeaveResponse(updated);
    }

    private void validateDoctor(String doctorId) {
        StaffMember staff = staffMemberRepository.findById(doctorId)
                .orElseThrow(() -> new StaffNotFoundException(doctorId));

        if (staff.getStaffType() != StaffType.DOCTOR) {
            throw new BusinessRuleException("INVALID_DOCTOR",
                    "Staff member '" + doctorId + "' is not a DOCTOR",
                    HttpStatus.BAD_REQUEST);
        }

        if (staff.getStatus() == StaffStatus.TERMINATED || staff.getStatus() == StaffStatus.RESIGNED) {
            throw new BusinessRuleException("INACTIVE_DOCTOR",
                    "Doctor '" + doctorId + "' is inactive with status: " + staff.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }
    }
}
