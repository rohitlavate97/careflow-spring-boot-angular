package com.careflow.appointment.service;

import com.careflow.appointment.domain.AppointmentStatus;
import com.careflow.appointment.dto.AppointmentResponse;
import com.careflow.appointment.dto.AppointmentSummaryResponse;
import com.careflow.appointment.dto.BookAppointmentRequest;
import com.careflow.appointment.dto.CancelAppointmentRequest;
import com.careflow.appointment.dto.RescheduleAppointmentRequest;
import com.careflow.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Service boundary managing appointment lifecycle, double-booking prevention, and state transitions (§13, §19, §20).
 */
public interface AppointmentService {

    AppointmentResponse bookAppointment(BookAppointmentRequest request);

    AppointmentResponse getAppointmentById(String id);

    PageResponse<AppointmentSummaryResponse> searchAppointments(
            String patientId,
            String doctorId,
            String departmentId,
            AppointmentStatus status,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            Pageable pageable
    );

    AppointmentResponse confirmAppointment(String id);

    AppointmentResponse checkInAppointment(String id);

    AppointmentResponse startConsultation(String id);

    AppointmentResponse completeConsultation(String id);

    AppointmentResponse cancelAppointment(String id, CancelAppointmentRequest request);

    AppointmentResponse markNoShow(String id);

    AppointmentResponse rescheduleAppointment(String id, RescheduleAppointmentRequest request);
}
