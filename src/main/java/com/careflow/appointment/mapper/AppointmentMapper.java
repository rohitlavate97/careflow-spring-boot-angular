package com.careflow.appointment.mapper;

import com.careflow.appointment.domain.Appointment;
import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.AppointmentSummaryResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure Java mapper for Appointment domain entities and DTOs (§89, §90).
 */
@Component
public class AppointmentMapper {

    public Appointment toEntity(String id, BookAppointmentRequest request) {
        if (request == null) {
            return null;
        }

        return new Appointment(
                id,
                request.patientId().trim(),
                request.doctorId().trim(),
                request.departmentId().trim(),
                request.appointmentDateTime(),
                request.durationMinutes(),
                request.reasonForVisit()
        );
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getDepartmentId(),
                appointment.getAppointmentDateTime(),
                appointment.getDurationMinutes(),
                appointment.getStatus(),
                appointment.getReasonForVisit(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getVersion()
        );
    }

    public List<AppointmentResponse> toResponseList(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyList();
        }
        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentSummaryResponse toSummaryResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return new AppointmentSummaryResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getDepartmentId(),
                appointment.getAppointmentDateTime(),
                appointment.getDurationMinutes(),
                appointment.getStatus()
        );
    }

    public List<AppointmentSummaryResponse> toSummaryResponseList(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyList();
        }
        return appointments.stream()
                .map(this::toSummaryResponse)
                .toList();
    }
}
