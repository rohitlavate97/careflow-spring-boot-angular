package com.careflow.scheduling.service;

import com.careflow.scheduling.dto.AvailableSlotResponse;
import com.careflow.scheduling.dto.CreateDoctorScheduleRequest;
import com.careflow.scheduling.dto.DoctorScheduleResponse;
import com.careflow.scheduling.dto.UpdateDoctorScheduleRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Service boundary managing doctor weekly schedule templates and appointment slot calculation (§13, §18).
 */
public interface DoctorScheduleService {

    DoctorScheduleResponse createSchedule(String doctorId, CreateDoctorScheduleRequest request);

    List<DoctorScheduleResponse> getDoctorSchedules(String doctorId);

    DoctorScheduleResponse getScheduleById(String id);

    DoctorScheduleResponse updateSchedule(String id, UpdateDoctorScheduleRequest request);

    void deleteSchedule(String id);

    List<AvailableSlotResponse> getAvailableSlots(String doctorId, LocalDate date);
}
