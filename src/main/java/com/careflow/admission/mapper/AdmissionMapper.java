package com.careflow.admission.mapper;

import com.careflow.admission.domain.Admission;
import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedTransferRecord;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.dto.AdmissionResponse;
import com.careflow.admission.dto.AdmissionSummaryResponse;
import com.careflow.admission.dto.BedResponse;
import com.careflow.admission.dto.BedTransferResponse;
import com.careflow.admission.dto.RoomResponse;
import com.careflow.admission.dto.WardResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Inpatient Admission and Bed Management entities and DTOs (§28).
 */
@Component
public class AdmissionMapper {

    public WardResponse toWardResponse(Ward ward, int availableBeds, int occupiedBeds) {
        if (ward == null) {
            return null;
        }
        return new WardResponse(
                ward.getId(),
                ward.getWardCode(),
                ward.getName(),
                ward.getDepartmentId(),
                ward.getWardType(),
                ward.getFloor(),
                ward.getTotalBeds(),
                ward.isActive(),
                availableBeds,
                occupiedBeds,
                ward.getCreatedAt()
        );
    }

    public RoomResponse toRoomResponse(Room room) {
        if (room == null) {
            return null;
        }
        List<BedResponse> beds = room.getBeds() != null
                ? room.getBeds().stream().map(this::toBedResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getWard() != null ? room.getWard().getId() : null,
                room.getRoomType(),
                room.isActive(),
                beds
        );
    }

    public BedResponse toBedResponse(Bed bed) {
        if (bed == null) {
            return null;
        }
        Room room = bed.getRoom();
        Ward ward = room != null ? room.getWard() : null;

        return new BedResponse(
                bed.getId(),
                bed.getBedNumber(),
                room != null ? room.getId() : null,
                room != null ? room.getRoomNumber() : null,
                ward != null ? ward.getId() : null,
                ward != null ? ward.getName() : null,
                bed.getStatus(),
                bed.getDailyRate(),
                bed.isActive()
        );
    }

    public BedTransferResponse toBedTransferResponse(BedTransferRecord record) {
        if (record == null) {
            return null;
        }
        return new BedTransferResponse(
                record.getId(),
                record.getAdmission() != null ? record.getAdmission().getId() : null,
                record.getFromBed() != null ? record.getFromBed().getId() : null,
                record.getFromBed() != null ? record.getFromBed().getBedNumber() : null,
                record.getToBed() != null ? record.getToBed().getId() : null,
                record.getToBed() != null ? record.getToBed().getBedNumber() : null,
                record.getTransferredAt(),
                record.getTransferReason(),
                record.getTransferredById()
        );
    }

    public AdmissionResponse toAdmissionResponse(Admission admission) {
        if (admission == null) {
            return null;
        }
        Bed bed = admission.getCurrentBed();
        Room room = bed != null ? bed.getRoom() : null;
        Ward ward = room != null ? room.getWard() : null;

        List<BedTransferResponse> transfers = admission.getTransfers() != null
                ? admission.getTransfers().stream().map(this::toBedTransferResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new AdmissionResponse(
                admission.getId(),
                admission.getAdmissionNumber(),
                admission.getPatientId(),
                admission.getAdmittingDoctorId(),
                bed != null ? bed.getId() : null,
                bed != null ? bed.getBedNumber() : null,
                room != null ? room.getRoomNumber() : null,
                ward != null ? ward.getName() : null,
                admission.getEncounterId(),
                admission.getStatus(),
                admission.getAdmissionReason(),
                admission.getAdmittingDiagnosis(),
                admission.getAdmittedAt(),
                admission.getDischargedAt(),
                admission.getDischargeSummary(),
                transfers
        );
    }

    public AdmissionSummaryResponse toAdmissionSummaryResponse(Admission admission) {
        if (admission == null) {
            return null;
        }
        Bed bed = admission.getCurrentBed();
        Room room = bed != null ? bed.getRoom() : null;
        Ward ward = room != null ? room.getWard() : null;

        return new AdmissionSummaryResponse(
                admission.getId(),
                admission.getAdmissionNumber(),
                admission.getPatientId(),
                admission.getAdmittingDoctorId(),
                bed != null ? bed.getId() : null,
                bed != null ? bed.getBedNumber() : null,
                ward != null ? ward.getName() : null,
                admission.getStatus(),
                admission.getAdmittedAt(),
                admission.getDischargedAt()
        );
    }
}
