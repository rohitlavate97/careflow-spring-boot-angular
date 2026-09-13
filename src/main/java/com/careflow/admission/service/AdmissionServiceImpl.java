package com.careflow.admission.service;

import com.careflow.admission.domain.Admission;
import com.careflow.admission.domain.AdmissionStatus;
import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.domain.BedTransferRecord;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.dto.AdmissionResponse;
import com.careflow.admission.dto.AdmissionSummaryResponse;
import com.careflow.admission.dto.AdmitPatientRequest;
import com.careflow.admission.dto.BedResponse;
import com.careflow.admission.dto.CreateBedRequest;
import com.careflow.admission.dto.CreateRoomRequest;
import com.careflow.admission.dto.CreateWardRequest;
import com.careflow.admission.dto.DischargePatientRequest;
import com.careflow.admission.dto.RoomResponse;
import com.careflow.admission.dto.TransferBedRequest;
import com.careflow.admission.dto.WardResponse;
import com.careflow.admission.exception.AdmissionNotFoundException;
import com.careflow.admission.exception.BedNotFoundException;
import com.careflow.admission.exception.InvalidAdmissionStatusTransitionException;
import com.careflow.admission.exception.PatientAlreadyAdmittedException;
import com.careflow.admission.exception.RoomNotFoundException;
import com.careflow.admission.exception.WardNotFoundException;
import com.careflow.admission.mapper.AdmissionMapper;
import com.careflow.admission.repository.AdmissionRepository;
import com.careflow.admission.repository.BedRepository;
import com.careflow.admission.repository.BedTransferRecordRepository;
import com.careflow.admission.repository.RoomRepository;
import com.careflow.admission.repository.WardRepository;
import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.consultation.repository.ConsultationRepository;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.StaffMember;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of inpatient hospital admission workflows and concurrency-safe bed allocation (§28, §57 Lab 4).
 */
@Service
public class AdmissionServiceImpl implements AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionServiceImpl.class);

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final BedTransferRecordRepository bedTransferRecordRepository;
    private final PatientRepository patientRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionMapper admissionMapper;

    public AdmissionServiceImpl(WardRepository wardRepository,
                                RoomRepository roomRepository,
                                BedRepository bedRepository,
                                AdmissionRepository admissionRepository,
                                BedTransferRecordRepository bedTransferRecordRepository,
                                PatientRepository patientRepository,
                                StaffMemberRepository staffMemberRepository,
                                DepartmentRepository departmentRepository,
                                ConsultationRepository consultationRepository,
                                AdmissionMapper admissionMapper) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.admissionRepository = admissionRepository;
        this.bedTransferRecordRepository = bedTransferRecordRepository;
        this.patientRepository = patientRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.departmentRepository = departmentRepository;
        this.consultationRepository = consultationRepository;
        this.admissionMapper = admissionMapper;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Wards, Rooms & Beds
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public WardResponse createWard(CreateWardRequest request) {
        log.info("Creating ward code='{}', name='{}'", request.wardCode(), request.name());

        if (wardRepository.existsByWardCode(request.wardCode().trim())) {
            throw new BusinessRuleException(
                    "DUPLICATE_WARD_CODE",
                    "Ward with code '" + request.wardCode().trim() + "' already exists.",
                    HttpStatus.CONFLICT
            );
        }

        if (!departmentRepository.existsById(request.departmentId().trim())) {
            throw new ResourceNotFoundException("Department", request.departmentId());
        }

        String wardId = UUID.randomUUID().toString();
        Ward ward = new Ward(
                wardId,
                request.wardCode().trim().toUpperCase(),
                request.name().trim(),
                request.departmentId().trim(),
                request.wardType(),
                request.floor() != null ? request.floor().trim() : null,
                request.totalBeds(),
                true
        );

        Ward saved = wardRepository.save(ward);
        return admissionMapper.toWardResponse(saved, 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public WardResponse getWardById(String wardId) {
        Ward ward = wardRepository.findById(wardId.trim())
                .orElseThrow(() -> new WardNotFoundException(wardId));

        List<Bed> beds = bedRepository.findByWardId(wardId.trim());
        int available = (int) beds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
        int occupied = (int) beds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();

        return admissionMapper.toWardResponse(ward, available, occupied);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WardResponse> getWards(Pageable pageable) {
        return wardRepository.findByActiveTrue(pageable).map(ward -> {
            List<Bed> beds = bedRepository.findByWardId(ward.getId());
            int available = (int) beds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
            int occupied = (int) beds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
            return admissionMapper.toWardResponse(ward, available, occupied);
        });
    }

    @Override
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        log.info("Creating room number='{}' in ward id='{}'", request.roomNumber(), request.wardId());
        Ward ward = wardRepository.findById(request.wardId().trim())
                .orElseThrow(() -> new WardNotFoundException(request.wardId()));

        if (roomRepository.findByWardIdAndRoomNumber(ward.getId(), request.roomNumber().trim()).isPresent()) {
            throw new BusinessRuleException(
                    "DUPLICATE_ROOM_NUMBER",
                    String.format("Room '%s' already exists in ward '%s'.", request.roomNumber(), ward.getWardCode()),
                    HttpStatus.CONFLICT
            );
        }

        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, request.roomNumber().trim(), ward, request.roomType(), true);
        Room saved = roomRepository.save(room);
        return admissionMapper.toRoomResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(String roomId) {
        Room room = roomRepository.findById(roomId.trim())
                .orElseThrow(() -> new RoomNotFoundException(roomId));
        return admissionMapper.toRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByWard(String wardId) {
        return roomRepository.findByWardIdAndActiveTrue(wardId.trim()).stream()
                .map(admissionMapper::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BedResponse createBed(CreateBedRequest request) {
        log.info("Creating bed number='{}' in room id='{}'", request.bedNumber(), request.roomId());
        Room room = roomRepository.findById(request.roomId().trim())
                .orElseThrow(() -> new RoomNotFoundException(request.roomId()));

        String bedId = UUID.randomUUID().toString();
        Bed bed = new Bed(bedId, request.bedNumber().trim(), room, BedStatus.AVAILABLE, request.dailyRate(), true);
        Bed saved = bedRepository.save(bed);

        // Increment total beds in ward
        Ward ward = room.getWard();
        ward.setTotalBeds(ward.getTotalBeds() + 1);
        wardRepository.save(ward);

        return admissionMapper.toBedResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BedResponse getBedById(String bedId) {
        Bed bed = bedRepository.findById(bedId.trim())
                .orElseThrow(() -> new BedNotFoundException(bedId));
        return admissionMapper.toBedResponse(bed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedResponse> getAvailableBeds(String wardId) {
        if (wardId != null && !wardId.isBlank()) {
            return bedRepository.findAvailableBedsByWardId(wardId.trim()).stream()
                    .map(admissionMapper::toBedResponse)
                    .collect(Collectors.toList());
        }
        return bedRepository.findByStatusAndActiveTrue(BedStatus.AVAILABLE).stream()
                .map(admissionMapper::toBedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedResponse> getBedsByRoom(String roomId) {
        return bedRepository.findByRoomIdAndActiveTrue(roomId.trim()).stream()
                .map(admissionMapper::toBedResponse)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Inpatient Admissions & Bed Concurrency
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public AdmissionResponse admitPatient(AdmitPatientRequest request) {
        log.info("Admitting patient id='{}', doctor id='{}', bed id='{}'",
                request.patientId(), request.admittingDoctorId(), request.bedId());

        validatePatient(request.patientId());
        validateDoctor(request.admittingDoctorId());

        // Check if patient already has an active admission
        List<AdmissionStatus> activeStatuses = List.of(AdmissionStatus.ADMITTED, AdmissionStatus.TRANSFERRED);
        if (admissionRepository.existsByPatientIdAndStatusIn(request.patientId().trim(), activeStatuses)) {
            throw new PatientAlreadyAdmittedException(request.patientId(), "Active inpatient admission already exists");
        }

        // Concurrency Lab 4: Pessimistic Write Lock on target bed (SELECT ... FOR UPDATE)
        Bed bed = bedRepository.findByIdForUpdate(request.bedId().trim())
                .orElseThrow(() -> new BedNotFoundException(request.bedId()));

        if (!bed.isActive()) {
            throw new BusinessRuleException("INACTIVE_BED", "Cannot admit patient to inactive bed.", HttpStatus.BAD_REQUEST);
        }

        // Evaluate state & allocate (throws BedNotAvailableException if not AVAILABLE)
        bed.allocate();
        bedRepository.save(bed);

        String encounterId = null;
        if (request.encounterId() != null && !request.encounterId().isBlank()) {
            encounterId = request.encounterId().trim();
            if (!consultationRepository.existsById(encounterId)) {
                throw new ResourceNotFoundException("Consultation", encounterId);
            }
        }

        String admissionId = UUID.randomUUID().toString();
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String admissionNumber = String.format("ADM-%s-%s", datePart, uniqueSuffix);

        Admission admission = new Admission(
                admissionId,
                admissionNumber,
                request.patientId().trim(),
                request.admittingDoctorId().trim(),
                bed,
                encounterId,
                request.admissionReason().trim(),
                request.admittingDiagnosis() != null ? request.admittingDiagnosis().trim() : null,
                Instant.now()
        );

        Admission saved = admissionRepository.save(admission);

        // Initial bed assignment record
        BedTransferRecord initialRecord = new BedTransferRecord(
                UUID.randomUUID().toString(),
                saved,
                null,
                bed,
                Instant.now(),
                "Initial admission bed allocation",
                request.admittingDoctorId().trim()
        );
        bedTransferRecordRepository.save(initialRecord);

        return admissionMapper.toAdmissionResponse(saved);
    }

    @Override
    @Transactional
    public AdmissionResponse transferBed(String admissionId, TransferBedRequest request) {
        log.info("Transferring admission id='{}' to bed id='{}'", admissionId, request.targetBedId());
        Admission admission = admissionRepository.findById(admissionId.trim())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));

        if (admission.getStatus() != AdmissionStatus.ADMITTED && admission.getStatus() != AdmissionStatus.TRANSFERRED) {
            throw new InvalidAdmissionStatusTransitionException(
                    "Cannot transfer patient from admission in status: " + admission.getStatus());
        }

        // Acquire lock on target bed
        Bed targetBed = bedRepository.findByIdForUpdate(request.targetBedId().trim())
                .orElseThrow(() -> new BedNotFoundException(request.targetBedId()));

        if (!targetBed.isActive()) {
            throw new BusinessRuleException("INACTIVE_BED", "Cannot transfer to inactive bed.", HttpStatus.BAD_REQUEST);
        }

        // Allocate target bed
        targetBed.allocate();
        bedRepository.save(targetBed);

        // Release previous bed
        Bed previousBed = admission.getCurrentBed();
        if (previousBed != null) {
            Bed lockedPrevious = bedRepository.findByIdForUpdate(previousBed.getId()).orElse(previousBed);
            lockedPrevious.release();
            bedRepository.save(lockedPrevious);
        }

        String staffId = request.transferredById() != null && !request.transferredById().isBlank()
                ? request.transferredById().trim()
                : admission.getAdmittingDoctorId();

        admission.transferTo(targetBed, request.transferReason().trim(), staffId);
        Admission saved = admissionRepository.save(admission);

        return admissionMapper.toAdmissionResponse(saved);
    }

    @Override
    @Transactional
    public AdmissionResponse dischargePatient(String admissionId, DischargePatientRequest request) {
        log.info("Discharging admission id='{}'", admissionId);
        Admission admission = admissionRepository.findById(admissionId.trim())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));

        Bed currentBed = admission.getCurrentBed();
        if (currentBed != null) {
            Bed lockedBed = bedRepository.findByIdForUpdate(currentBed.getId()).orElse(currentBed);
            lockedBed.release();
            bedRepository.save(lockedBed);
        }

        admission.discharge(request.dischargeSummary().trim());
        Admission saved = admissionRepository.save(admission);

        return admissionMapper.toAdmissionResponse(saved);
    }

    @Override
    @Transactional
    public AdmissionResponse cancelAdmission(String admissionId, String reason) {
        log.info("Cancelling admission id='{}', reason='{}'", admissionId, reason);
        Admission admission = admissionRepository.findById(admissionId.trim())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));

        Bed currentBed = admission.getCurrentBed();
        if (currentBed != null) {
            Bed lockedBed = bedRepository.findByIdForUpdate(currentBed.getId()).orElse(currentBed);
            lockedBed.release();
            bedRepository.save(lockedBed);
        }

        admission.cancel(reason != null ? reason.trim() : "Admission cancelled");
        Admission saved = admissionRepository.save(admission);

        return admissionMapper.toAdmissionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionResponse getAdmissionById(String admissionId) {
        Admission admission = admissionRepository.findByIdWithBedDetails(admissionId.trim())
                .or(() -> admissionRepository.findById(admissionId.trim()))
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        return admissionMapper.toAdmissionResponse(admission);
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionResponse getAdmissionByNumber(String admissionNumber) {
        Admission admission = admissionRepository.findByAdmissionNumber(admissionNumber.trim())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionNumber));
        return admissionMapper.toAdmissionResponse(admission);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdmissionSummaryResponse> getAdmissionsByPatient(String patientId, Pageable pageable) {
        validatePatient(patientId);
        return admissionRepository.findByPatientIdOrderByAdmittedAtDesc(patientId.trim(), pageable)
                .map(admissionMapper::toAdmissionSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdmissionSummaryResponse> getAdmissionsByStatus(AdmissionStatus status, Pageable pageable) {
        return admissionRepository.findByStatusOrderByAdmittedAtDesc(status, pageable)
                .map(admissionMapper::toAdmissionSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdmissionSummaryResponse> getAllAdmissions(Pageable pageable) {
        return admissionRepository.findAll(pageable)
                .map(admissionMapper::toAdmissionSummaryResponse);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Validation Helpers
    // -----------------------------------------------------------------------------------------------------------------

    private void validatePatient(String patientId) {
        if (!patientRepository.existsById(patientId.trim())) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }

    private void validateDoctor(String doctorId) {
        StaffMember staff = staffMemberRepository.findById(doctorId.trim())
                .orElseThrow(() -> new StaffNotFoundException(doctorId));

        if (staff.getStaffType() != StaffType.DOCTOR) {
            throw new BusinessRuleException(
                    "INVALID_STAFF_TYPE",
                    String.format("Staff member '%s' is of type '%s', but only DOCTOR can perform this action.",
                            doctorId, staff.getStaffType()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
