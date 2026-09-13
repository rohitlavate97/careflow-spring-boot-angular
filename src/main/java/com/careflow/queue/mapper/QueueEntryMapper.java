package com.careflow.queue.mapper;

import com.careflow.queue.domain.QueueEntry;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;
import com.careflow.queue.dto.QueueSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mapper for QueueEntry entity and associated DTOs (§21).
 */
@Component
public class QueueEntryMapper {

    public QueueEntry toEntity(String id,
                              EnqueuePatientRequest request,
                              LocalDate queueDate,
                              Integer tokenNumber,
                              String tokenDisplay) {
        return new QueueEntry(
                id,
                request.departmentId().trim(),
                request.doctorId() != null && !request.doctorId().isBlank() ? request.doctorId().trim() : null,
                request.patientId().trim(),
                request.appointmentId() != null && !request.appointmentId().isBlank() ? request.appointmentId().trim() : null,
                queueDate,
                tokenNumber,
                tokenDisplay,
                request.resolvedPriority(),
                Instant.now(),
                request.notes()
        );
    }

    public QueueEntryResponse toResponse(QueueEntry entity, long patientsAhead) {
        return new QueueEntryResponse(
                entity.getId(),
                entity.getDepartmentId(),
                entity.getDoctorId(),
                entity.getPatientId(),
                entity.getAppointmentId(),
                entity.getQueueDate(),
                entity.getTokenNumber(),
                entity.getTokenDisplay(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getEntryTime(),
                entity.getCalledTime(),
                entity.getConsultationStartTime(),
                entity.getConsultationEndTime(),
                entity.getNotes(),
                patientsAhead,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public QueueSummaryResponse toSummary(QueueEntry entity) {
        return new QueueSummaryResponse(
                entity.getId(),
                entity.getTokenDisplay(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getEntryTime(),
                entity.getCalledTime()
        );
    }

    public List<QueueSummaryResponse> toSummaries(List<QueueEntry> entries) {
        return entries.stream().map(this::toSummary).toList();
    }
}
