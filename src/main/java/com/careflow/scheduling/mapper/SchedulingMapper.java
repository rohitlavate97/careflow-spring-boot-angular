package com.careflow.scheduling.mapper;

import com.careflow.scheduling.domain.DoctorLeave;
import com.careflow.scheduling.domain.DoctorSchedule;
import com.careflow.scheduling.dto.CreateDoctorLeaveRequest;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.DoctorLeaveResponse;
import com.careflow.scheduling.dto.DoctorScheduleResponse;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Doctor Schedules, Leaves, and Slot DTOs (§89, §90).
 */
@Component
public class SchedulingMapper {

    public DoctorSchedule toScheduleEntity(String id, String doctorId, CreateDoctorScheduleRequest request) {
        if (request == null) {
            return null;
        }

        return new DoctorSchedule(
                id,
                doctorId,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.breakStartTime(),
                request.breakEndTime(),
                request.slotDurationMinutes()
        );
    }

    public void updateScheduleEntity(DoctorSchedule schedule, UpdateDoctorScheduleRequest request) {
        if (schedule == null || request == null) {
            return;
        }

        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setBreakStartTime(request.breakStartTime());
        schedule.setBreakEndTime(request.breakEndTime());
        schedule.setSlotDurationMinutes(request.slotDurationMinutes());
        if (request.isActive() != null) {
            schedule.setActive(request.isActive());
        }
    }

    public DoctorScheduleResponse toScheduleResponse(DoctorSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        return new DoctorScheduleResponse(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getBreakStartTime(),
                schedule.getBreakEndTime(),
                schedule.getSlotDurationMinutes(),
                schedule.isActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                schedule.getVersion()
        );
    }

    public List<DoctorScheduleResponse> toScheduleResponseList(List<DoctorSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return Collections.emptyList();
        }
        return schedules.stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    public DoctorLeave toLeaveEntity(String id, String doctorId, CreateDoctorLeaveRequest request) {
        if (request == null) {
            return null;
        }

        return new DoctorLeave(
                id,
                doctorId,
                request.startDate(),
                request.endDate(),
                request.reason()
        );
    }

    public DoctorLeaveResponse toLeaveResponse(DoctorLeave leave) {
        if (leave == null) {
            return null;
        }

        return new DoctorLeaveResponse(
                leave.getId(),
                leave.getDoctorId(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getReason(),
                leave.getStatus(),
                leave.getCreatedAt(),
                leave.getUpdatedAt(),
                leave.getVersion()
        );
    }

    public List<DoctorLeaveResponse> toLeaveResponseList(List<DoctorLeave> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return Collections.emptyList();
        }
        return leaves.stream()
                .map(this::toLeaveResponse)
                .toList();
    }
}
