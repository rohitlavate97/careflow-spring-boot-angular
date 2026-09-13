package com.careflow.staff.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.department.service.DepartmentService;
import com.careflow.identity.repository.UserRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.dto.CreateStaffRequest;
import com.careflow.staff.dto.DoctorSummaryResponse;
import com.careflow.staff.dto.StaffResponse;
import com.careflow.staff.dto.StaffSummaryResponse;
import com.careflow.staff.dto.UpdateStaffRequest;
import com.careflow.staff.dto.UpdateStaffStatusRequest;
import com.careflow.staff.exception.DuplicateDoctorLicenseException;
import com.careflow.staff.exception.DuplicateStaffException;
import com.careflow.staff.exception.InvalidStaffStatusTransitionException;
import com.careflow.staff.exception.StaffNotFoundException;
import com.careflow.staff.mapper.StaffMapper;
import com.careflow.staff.repository.DoctorProfileRepository;
import com.careflow.staff.repository.StaffMemberRepository;
import com.careflow.staff.repository.StaffMemberSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of StaffService managing staff lifecycle, doctor specializations, and departmental assignments (§13, §17).
 */
@Service
public class StaffServiceImpl implements StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffServiceImpl.class);

    private final StaffMemberRepository staffMemberRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentService departmentService;
    private final UserRepository userRepository;
    private final StaffMapper staffMapper;

    public StaffServiceImpl(StaffMemberRepository staffMemberRepository,
                            DoctorProfileRepository doctorProfileRepository,
                            DepartmentService departmentService,
                            UserRepository userRepository,
                            StaffMapper staffMapper) {
        this.staffMemberRepository = staffMemberRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.departmentService = departmentService;
        this.userRepository = userRepository;
        this.staffMapper = staffMapper;
    }

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        String staffCode = request.staffCode().trim().toUpperCase();
        String email = request.email().trim().toLowerCase();
        log.info("Creating staff member staffCode='{}', type='{}', email='{}'", staffCode, request.staffType(), email);

        if (staffMemberRepository.existsByStaffCodeIgnoreCase(staffCode)) {
            log.warn("Conflict: Staff code '{}' already exists", staffCode);
            throw DuplicateStaffException.forStaffCode(staffCode);
        }

        if (staffMemberRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Conflict: Staff email '{}' already exists", email);
            throw DuplicateStaffException.forEmail(email);
        }

        if (request.userId() != null && !request.userId().isBlank()) {
            String userId = request.userId().trim();
            if (staffMemberRepository.existsByUserId(userId)) {
                log.warn("Conflict: User ID '{}' already linked to another staff profile", userId);
                throw DuplicateStaffException.forUserId(userId);
            }
            if (!userRepository.existsById(userId)) {
                log.warn("Not found: User ID '{}' does not exist", userId);
                throw new ResourceNotFoundException("User", userId);
            }
        }

        // Validate department exists
        departmentService.getDepartmentById(request.departmentId().trim());

        // Validate doctor profile requirements
        if (request.staffType() == StaffType.DOCTOR) {
            if (request.doctorProfile() == null) {
                log.warn("Validation error: Doctor profile is required for staff type DOCTOR");
                throw new BusinessRuleException("DOCTOR_PROFILE_REQUIRED",
                        "Doctor profile credentials are required when staff type is DOCTOR",
                        HttpStatus.BAD_REQUEST);
            }
            String licenseNumber = request.doctorProfile().medicalLicenseNumber().trim().toUpperCase();
            if (doctorProfileRepository.existsByMedicalLicenseNumberIgnoreCase(licenseNumber)) {
                log.warn("Conflict: Medical license number '{}' already exists", licenseNumber);
                throw new DuplicateDoctorLicenseException(licenseNumber);
            }
        }

        String staffId = UUID.randomUUID().toString();
        StaffMember staffMember = staffMapper.toEntity(staffId, request);
        staffMember.setStaffCode(staffCode);
        staffMember.setEmail(email);

        StaffMember saved = staffMemberRepository.save(staffMember);
        log.info("Successfully created staff member id='{}', staffCode='{}'", saved.getId(), saved.getStaffCode());
        return staffMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(String id) {
        log.debug("Fetching staff member by id='{}'", id);
        StaffMember staff = staffMemberRepository.findByIdWithDoctorProfile(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
        return staffMapper.toResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffByCode(String staffCode) {
        String normalizedCode = staffCode.trim().toUpperCase();
        log.debug("Fetching staff member by code='{}'", normalizedCode);
        StaffMember staff = staffMemberRepository.findByStaffCodeWithDoctorProfile(normalizedCode)
                .orElseThrow(() -> StaffNotFoundException.forCode(normalizedCode));
        return staffMapper.toResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffByUserId(String userId) {
        log.debug("Fetching staff member by userId='{}'", userId);
        StaffMember staff = staffMemberRepository.findByUserIdWithDoctorProfile(userId)
                .orElseThrow(() -> StaffNotFoundException.forUserId(userId));
        return staffMapper.toResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffSummaryResponse> searchStaff(
            String query,
            String departmentId,
            StaffType staffType,
            StaffStatus status,
            Pageable pageable
    ) {
        log.debug("Searching staff query='{}', departmentId='{}', staffType='{}', status='{}'",
                query, departmentId, staffType, status);
        Specification<StaffMember> spec = StaffMemberSpecification.withFilters(query, departmentId, staffType, status);
        Page<StaffMember> page = staffMemberRepository.findAll(spec, pageable);
        return PageResponse.from(page, staffMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(String id, UpdateStaffRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Updating staff member id='{}', email='{}'", id, email);

        StaffMember staff = staffMemberRepository.findByIdWithDoctorProfile(id)
                .orElseThrow(() -> new StaffNotFoundException(id));

        if (staffMemberRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            log.warn("Conflict: Staff email '{}' already taken by another staff member", email);
            throw DuplicateStaffException.forEmail(email);
        }

        if (request.userId() != null && !request.userId().isBlank()) {
            String userId = request.userId().trim();
            if (staffMemberRepository.existsByUserIdAndIdNot(userId, id)) {
                log.warn("Conflict: User ID '{}' already linked to another staff member", userId);
                throw DuplicateStaffException.forUserId(userId);
            }
            if (!userRepository.existsById(userId)) {
                log.warn("Not found: User ID '{}' does not exist", userId);
                throw new ResourceNotFoundException("User", userId);
            }
        }

        // Validate department
        departmentService.getDepartmentById(request.departmentId().trim());

        // Validate doctor profile if applicable
        if (staff.getStaffType() == StaffType.DOCTOR && request.doctorProfile() != null) {
            String licenseNumber = request.doctorProfile().medicalLicenseNumber().trim().toUpperCase();
            String profileId = staff.getDoctorProfile() != null ? staff.getDoctorProfile().getId() : null;
            if (doctorProfileRepository.existsByMedicalLicenseNumberIgnoreCaseAndIdNot(licenseNumber, profileId)) {
                log.warn("Conflict: Medical license number '{}' already exists on another profile", licenseNumber);
                throw new DuplicateDoctorLicenseException(licenseNumber);
            }
        }

        staffMapper.updateEntity(staff, request);
        StaffMember updated = staffMemberRepository.save(staff);
        log.info("Successfully updated staff member id='{}'", updated.getId());
        return staffMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public StaffResponse updateStaffStatus(String id, UpdateStaffStatusRequest request) {
        log.info("Transitioning status of staff member id='{}' to '{}'", id, request.status());

        StaffMember staff = staffMemberRepository.findByIdWithDoctorProfile(id)
                .orElseThrow(() -> new StaffNotFoundException(id));

        if (!staff.getStatus().canTransitionTo(request.status())) {
            log.warn("Invalid transition: Staff member id='{}' cannot transition from '{}' to '{}'",
                    id, staff.getStatus(), request.status());
            throw new InvalidStaffStatusTransitionException(staff.getStatus(), request.status());
        }

        staff.transitionStatus(request.status());
        StaffMember updated = staffMemberRepository.save(staff);
        log.info("Staff member id='{}' status transitioned to '{}'", updated.getId(), updated.getStatus());
        return staffMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorSummaryResponse> searchDoctors(
            String specialization,
            String departmentId,
            Pageable pageable
    ) {
        log.debug("Searching active doctors specialization='{}', departmentId='{}'", specialization, departmentId);
        Page<DoctorProfile> page = doctorProfileRepository.findActiveDoctorsPaged(specialization, departmentId, pageable);
        return PageResponse.from(page, staffMapper::toDoctorSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorSummaryResponse> getActiveDoctors(String specialization, String departmentId) {
        log.debug("Fetching active doctors specialization='{}', departmentId='{}'", specialization, departmentId);
        List<DoctorProfile> list = doctorProfileRepository.findActiveDoctors(specialization, departmentId);
        return staffMapper.toDoctorSummaryResponseList(list);
    }
}
