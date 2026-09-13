package com.careflow.scheduling.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.DoctorSchedule;
import com.careflow.scheduling.dto.AvailableSlotResponse;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.DoctorScheduleResponse;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;
import com.careflow.scheduling.exception.DoctorScheduleNotFoundException;
import com.careflow.scheduling.exception.InvalidScheduleTimeException;
import com.careflow.scheduling.exception.ScheduleOverlapException;
import com.careflow.scheduling.mapper.SchedulingMapper;
import com.careflow.scheduling.repository.DoctorLeaveRepository;
import com.careflow.scheduling.repository.DoctorScheduleRepository;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of DoctorScheduleService managing shift templates and slot generation engine (§13, §18).
 */
@Service
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private static final Logger log = LoggerFactory.getLogger(DoctorScheduleServiceImpl.class);

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final SchedulingMapper schedulingMapper;

    public DoctorScheduleServiceImpl(DoctorScheduleRepository doctorScheduleRepository,
                                     DoctorLeaveRepository doctorLeaveRepository,
                                     StaffMemberRepository staffMemberRepository,
                                     SchedulingMapper schedulingMapper) {
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorLeaveRepository = doctorLeaveRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.schedulingMapper = schedulingMapper;
    }

    @Override
    @Transactional
    public DoctorScheduleResponse createSchedule(String doctorId, CreateDoctorScheduleRequest request) {
        log.info("Creating schedule for doctor='{}', day='{}', time='{}-{}'",
                doctorId, request.dayOfWeek(), request.startTime(), request.endTime());

        validateDoctor(doctorId);
        validateTimes(request.startTime(), request.endTime(), request.breakStartTime(), request.breakEndTime());
        checkNoScheduleOverlap(doctorId, request.dayOfWeek(), request.startTime(), request.endTime(), null);

        String id = UUID.randomUUID().toString();
        DoctorSchedule schedule = schedulingMapper.toScheduleEntity(id, doctorId, request);
        DoctorSchedule saved = doctorScheduleRepository.save(schedule);

        log.info("Successfully created schedule id='{}' for doctor='{}'", saved.getId(), doctorId);
        return schedulingMapper.toScheduleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getDoctorSchedules(String doctorId) {
        log.debug("Fetching schedules for doctor='{}'", doctorId);
        validateDoctor(doctorId);
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorId(doctorId);
        return schedulingMapper.toScheduleResponseList(schedules);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorScheduleResponse getScheduleById(String id) {
        log.debug("Fetching schedule by id='{}'", id);
        DoctorSchedule schedule = doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new DoctorScheduleNotFoundException(id));
        return schedulingMapper.toScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public DoctorScheduleResponse updateSchedule(String id, UpdateDoctorScheduleRequest request) {
        log.info("Updating schedule id='{}', day='{}', time='{}-{}'",
                id, request.dayOfWeek(), request.startTime(), request.endTime());

        DoctorSchedule schedule = doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new DoctorScheduleNotFoundException(id));

        validateTimes(request.startTime(), request.endTime(), request.breakStartTime(), request.breakEndTime());

        if (Boolean.TRUE.equals(request.isActive())) {
            checkNoScheduleOverlap(schedule.getDoctorId(), request.dayOfWeek(), request.startTime(), request.endTime(), id);
        }

        schedulingMapper.updateScheduleEntity(schedule, request);
        DoctorSchedule updated = doctorScheduleRepository.save(schedule);

        log.info("Successfully updated schedule id='{}'", updated.getId());
        return schedulingMapper.toScheduleResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSchedule(String id) {
        log.info("Deleting schedule id='{}'", id);
        DoctorSchedule schedule = doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new DoctorScheduleNotFoundException(id));
        doctorScheduleRepository.delete(schedule);
        log.info("Deleted schedule id='{}'", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(String doctorId, LocalDate date) {
        log.debug("Calculating available slots for doctor='{}' on date='{}'", doctorId, date);

        validateDoctor(doctorId);

        // Check if doctor is on approved leave for this date (§18)
        List<DoctorLeave> approvedLeaves = doctorLeaveRepository.findApprovedLeavesCoveringDate(doctorId, date);
        if (!approvedLeaves.isEmpty()) {
            log.debug("Doctor='{}' has approved leave on date='{}'. No slots available.", doctorId, date);
            return Collections.emptyList();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorSchedule> activeSchedules = doctorScheduleRepository.findActiveSchedulesByDoctorAndDay(doctorId, dayOfWeek);
        if (activeSchedules.isEmpty()) {
            log.debug("Doctor='{}' has no active schedule on day='{}'.", doctorId, dayOfWeek);
            return Collections.emptyList();
        }

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        for (DoctorSchedule schedule : activeSchedules) {
            LocalTime current = schedule.getStartTime();
            int durationMinutes = schedule.getSlotDurationMinutes();

            while (!current.plusMinutes(durationMinutes).isAfter(schedule.getEndTime())) {
                LocalTime next = current.plusMinutes(durationMinutes);

                boolean overlapsBreak = false;
                if (schedule.hasBreak()) {
                    if (current.isBefore(schedule.getBreakEndTime()) && next.isAfter(schedule.getBreakStartTime())) {
                        overlapsBreak = true;
                    }
                }

                if (!overlapsBreak) {
                    availableSlots.add(new AvailableSlotResponse(doctorId, date, current, next, "AVAILABLE"));
                }

                current = next;
            }
        }

        return availableSlots;
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

    private void validateTimes(LocalTime startTime, LocalTime endTime, LocalTime breakStart, LocalTime breakEnd) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidScheduleTimeException("Start time (" + startTime + ") must be before end time (" + endTime + ")");
        }

        if ((breakStart == null && breakEnd != null) || (breakStart != null && breakEnd == null)) {
            throw new InvalidScheduleTimeException("Both break start and break end times must be provided together");
        }

        if (breakStart != null && breakEnd != null) {
            if (!breakStart.isBefore(breakEnd)) {
                throw new InvalidScheduleTimeException("Break start time (" + breakStart + ") must be before break end time (" + breakEnd + ")");
            }
            if (breakStart.isBefore(startTime) || breakEnd.isAfter(endTime)) {
                throw new InvalidScheduleTimeException(
                        String.format("Break time (%s - %s) must be strictly within working hours (%s - %s)",
                                breakStart, breakEnd, startTime, endTime)
                );
            }
        }
    }

    private void checkNoScheduleOverlap(String doctorId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end, String excludeScheduleId) {
        List<DoctorSchedule> existingSchedules = doctorScheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        for (DoctorSchedule existing : existingSchedules) {
            if (excludeScheduleId != null && existing.getId().equals(excludeScheduleId)) {
                continue;
            }
            if (existing.isActive() && existing.overlapsWith(start, end)) {
                throw new ScheduleOverlapException(
                        String.format("Doctor already has an active schedule on %s from %s to %s overlapping with requested %s to %s",
                                dayOfWeek, existing.getStartTime(), existing.getEndTime(), start, end)
                );
            }
        }
    }
}
