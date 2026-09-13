package com.careflow.staff.mapper;

import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorProfileDto;
import com.careflow.staff.dto.DoctorProfileResponse;
import com.careflow.staff.dto.DoctorSummaryResponse;
import com.careflow.staff.dto.StaffResponse;
import com.careflow.staff.dto.StaffSummaryResponse;
import com.careflow.staff.dto.UpdateStaffRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pure Java mapper for Staff and Doctor domain entities and DTOs (§89, §90).
 */
@Component
public class StaffMapper {

    public StaffMember toEntity(String id, CreateStaffRequest request) {
        if (request == null) {
            return null;
        }

        StaffMember staff = new StaffMember(
                id,
                request.staffCode(),
                request.departmentId() != null ? request.departmentId().trim() : null,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.staffType(),
                request.dateOfJoining()
        );

        if (request.userId() != null && !request.userId().isBlank()) {
            staff.setUserId(request.userId().trim());
        }

        if (request.staffType() == StaffType.DOCTOR && request.doctorProfile() != null) {
            DoctorProfile doctorProfile = toDoctorProfileEntity(UUID.randomUUID().toString(), request.doctorProfile());
            staff.setDoctorProfile(doctorProfile);
        }

        return staff;
    }

    public void updateEntity(StaffMember staffMember, UpdateStaffRequest request) {
        if (staffMember == null || request == null) {
            return;
        }

        staffMember.setFirstName(request.firstName().trim());
        staffMember.setLastName(request.lastName().trim());
        staffMember.setEmail(request.email().trim().toLowerCase());
        staffMember.setPhone(request.phone().trim());
        staffMember.setDepartmentId(request.departmentId().trim());

        if (request.userId() != null && !request.userId().isBlank()) {
            staffMember.setUserId(request.userId().trim());
        } else {
            staffMember.setUserId(null);
        }

        if (staffMember.getStaffType() == StaffType.DOCTOR && request.doctorProfile() != null) {
            if (staffMember.getDoctorProfile() != null) {
                updateDoctorProfileEntity(staffMember.getDoctorProfile(), request.doctorProfile());
            } else {
                DoctorProfile newProfile = toDoctorProfileEntity(UUID.randomUUID().toString(), request.doctorProfile());
                staffMember.setDoctorProfile(newProfile);
            }
        }
    }

    public DoctorProfile toDoctorProfileEntity(String id, DoctorProfileDto dto) {
        if (dto == null) {
            return null;
        }

        return new DoctorProfile(
                id,
                dto.specialization(),
                dto.qualifications(),
                dto.medicalLicenseNumber(),
                dto.consultationFee(),
                dto.consultationRoom(),
                dto.bio()
        );
    }

    public void updateDoctorProfileEntity(DoctorProfile profile, DoctorProfileDto dto) {
        if (profile == null || dto == null) {
            return;
        }

        profile.setSpecialization(dto.specialization().trim());
        profile.setQualifications(dto.qualifications().trim());
        profile.setMedicalLicenseNumber(dto.medicalLicenseNumber().trim().toUpperCase());
        profile.setConsultationFee(dto.consultationFee());
        profile.setConsultationRoom(dto.consultationRoom() != null ? dto.consultationRoom().trim() : null);
        profile.setBio(dto.bio() != null ? dto.bio().trim() : null);
    }

    public StaffResponse toResponse(StaffMember staffMember) {
        if (staffMember == null) {
            return null;
        }

        DoctorProfileResponse doctorResponse = toDoctorProfileResponse(staffMember.getDoctorProfile());
        String fullName = (staffMember.getFirstName() + " " + staffMember.getLastName()).trim();

        return new StaffResponse(
                staffMember.getId(),
                staffMember.getStaffCode(),
                staffMember.getUserId(),
                staffMember.getDepartmentId(),
                staffMember.getFirstName(),
                staffMember.getLastName(),
                fullName,
                staffMember.getEmail(),
                staffMember.getPhone(),
                staffMember.getStaffType(),
                staffMember.getStatus(),
                staffMember.getDateOfJoining(),
                doctorResponse,
                staffMember.getCreatedAt(),
                staffMember.getUpdatedAt(),
                staffMember.getVersion()
        );
    }

    public List<StaffResponse> toResponseList(List<StaffMember> staffMembers) {
        if (staffMembers == null || staffMembers.isEmpty()) {
            return Collections.emptyList();
        }
        return staffMembers.stream()
                .map(this::toResponse)
                .toList();
    }

    public StaffSummaryResponse toSummaryResponse(StaffMember staffMember) {
        if (staffMember == null) {
            return null;
        }

        String fullName = (staffMember.getFirstName() + " " + staffMember.getLastName()).trim();
        return new StaffSummaryResponse(
                staffMember.getId(),
                staffMember.getStaffCode(),
                fullName,
                staffMember.getDepartmentId(),
                staffMember.getStaffType(),
                staffMember.getStatus(),
                staffMember.getEmail(),
                staffMember.getPhone()
        );
    }

    public List<StaffSummaryResponse> toSummaryResponseList(List<StaffMember> staffMembers) {
        if (staffMembers == null || staffMembers.isEmpty()) {
            return Collections.emptyList();
        }
        return staffMembers.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public DoctorProfileResponse toDoctorProfileResponse(DoctorProfile profile) {
        if (profile == null) {
            return null;
        }

        return new DoctorProfileResponse(
                profile.getId(),
                profile.getSpecialization(),
                profile.getQualifications(),
                profile.getMedicalLicenseNumber(),
                profile.getConsultationFee(),
                profile.getConsultationRoom(),
                profile.getBio(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion()
        );
    }

    public DoctorSummaryResponse toDoctorSummaryResponse(DoctorProfile profile) {
        if (profile == null) {
            return null;
        }

        StaffMember staff = profile.getStaffMember();
        String doctorName = staff != null ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "Unknown";
        String staffId = staff != null ? staff.getId() : null;
        String staffCode = staff != null ? staff.getStaffCode() : null;
        String departmentId = staff != null ? staff.getDepartmentId() : null;

        return new DoctorSummaryResponse(
                staffId,
                staffCode,
                doctorName,
                departmentId,
                profile.getSpecialization(),
                profile.getMedicalLicenseNumber(),
                profile.getConsultationFee(),
                profile.getConsultationRoom()
        );
    }

    public List<DoctorSummaryResponse> toDoctorSummaryResponseList(List<DoctorProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Collections.emptyList();
        }
        return profiles.stream()
                .map(this::toDoctorSummaryResponse)
                .toList();
    }
}
