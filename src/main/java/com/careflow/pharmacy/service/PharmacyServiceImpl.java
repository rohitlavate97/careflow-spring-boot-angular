package com.careflow.pharmacy.service;

import com.careflow.common.exception.BusinessRuleException;
import com.careflow.common.exception.ResourceNotFoundException;
import com.careflow.pharmacy.domain.DispenseRecord;
import com.careflow.pharmacy.domain.Medication;
import com.careflow.pharmacy.domain.MedicationStatus;
import com.careflow.pharmacy.domain.PharmacyInventoryBatch;
import com.careflow.pharmacy.dto.AddInventoryBatchRequest;
import com.careflow.pharmacy.dto.CreateMedicationRequest;
import com.careflow.pharmacy.dto.DispenseMedicationRequest;
import com.careflow.pharmacy.dto.DispenseRecordResponse;
import com.careflow.pharmacy.dto.MedicationResponse;
import com.careflow.pharmacy.dto.PharmacyInventoryBatchResponse;
import com.careflow.pharmacy.dto.UpdateMedicationRequest;
import com.careflow.pharmacy.exception.InsufficientInventoryException;
import com.careflow.pharmacy.exception.MedicationNotFoundException;
import com.careflow.pharmacy.mapper.PharmacyMapper;
import com.careflow.pharmacy.repository.DispenseRecordRepository;
import com.careflow.pharmacy.repository.MedicationRepository;
import com.careflow.pharmacy.repository.PharmacyInventoryBatchRepository;
import com.careflow.prescription.domain.Prescription;
import com.careflow.prescription.domain.PrescriptionItem;
import com.careflow.prescription.domain.PrescriptionItemStatus;
import com.careflow.prescription.domain.PrescriptionStatus;
import com.careflow.prescription.exception.PrescriptionNotFoundException;
import com.careflow.prescription.repository.PrescriptionItemRepository;
import com.careflow.prescription.repository.PrescriptionRepository;
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
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of PharmacyService providing formulary management,
 * FEFO batch allocation, and pessimistic row-locking stock decrements (§25, §26, §92, §103 Phase 8).
 */
@Service
public class PharmacyServiceImpl implements PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyServiceImpl.class);

    private final MedicationRepository medicationRepository;
    private final PharmacyInventoryBatchRepository batchRepository;
    private final DispenseRecordRepository dispenseRecordRepository;
    private final PharmacyMapper pharmacyMapper;
    private final StaffMemberRepository staffMemberRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;

    public PharmacyServiceImpl(MedicationRepository medicationRepository,
                               PharmacyInventoryBatchRepository batchRepository,
                               DispenseRecordRepository dispenseRecordRepository,
                               PharmacyMapper pharmacyMapper,
                               StaffMemberRepository staffMemberRepository,
                               PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository prescriptionItemRepository) {
        this.medicationRepository = medicationRepository;
        this.batchRepository = batchRepository;
        this.dispenseRecordRepository = dispenseRecordRepository;
        this.pharmacyMapper = pharmacyMapper;
        this.staffMemberRepository = staffMemberRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
    }

    @Override
    @Transactional
    public MedicationResponse registerMedication(CreateMedicationRequest request) {
        log.info("Registering medication code='{}', name='{}'", request.code(), request.name());

        if (medicationRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new BusinessRuleException(
                    "DUPLICATE_MEDICATION_CODE",
                    String.format("Medication with code '%s' already exists in the formulary.", request.code()),
                    HttpStatus.CONFLICT
            );
        }

        String id = UUID.randomUUID().toString();
        Medication medication = pharmacyMapper.toEntity(id, request);
        Medication saved = medicationRepository.save(medication);

        log.info("Medication registered successfully id='{}', code='{}'", saved.getId(), saved.getCode());
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MedicationResponse updateMedication(String id, UpdateMedicationRequest request) {
        log.info("Updating medication id='{}'", id);
        Medication medication = medicationRepository.findById(id.trim())
                .orElseThrow(() -> new MedicationNotFoundException(id.trim()));

        if (request.name() != null && !request.name().isBlank()) {
            medication.setName(request.name().trim());
        }
        if (request.genericName() != null && !request.genericName().isBlank()) {
            medication.setGenericName(request.genericName().trim());
        }
        if (request.form() != null) {
            medication.setForm(request.form());
        }
        if (request.strength() != null && !request.strength().isBlank()) {
            medication.setStrength(request.strength().trim());
        }
        if (request.unitPrice() != null) {
            medication.setUnitPrice(request.unitPrice());
        }
        if (request.reorderThreshold() != null) {
            medication.setReorderThreshold(request.reorderThreshold());
        }
        if (request.status() != null) {
            medication.setStatus(request.status());
        }

        Medication updated = medicationRepository.save(medication);
        return pharmacyMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicationResponse getMedicationById(String id) {
        log.debug("Fetching medication id='{}'", id);
        Medication medication = medicationRepository.findById(id.trim())
                .orElseThrow(() -> new MedicationNotFoundException(id.trim()));
        return pharmacyMapper.toResponse(medication);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicationResponse> searchMedications(String query, MedicationStatus status, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return medicationRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
                    query.trim(), query.trim(), pageable
            ).map(pharmacyMapper::toResponse);
        }
        if (status != null) {
            return medicationRepository.findByStatus(status, pageable)
                    .map(pharmacyMapper::toResponse);
        }
        return medicationRepository.findAll(pageable)
                .map(pharmacyMapper::toResponse);
    }

    @Override
    @Transactional
    public PharmacyInventoryBatchResponse addInventoryBatch(AddInventoryBatchRequest request) {
        log.info("Adding inventory batch number='{}' for medication id='{}'",
                request.batchNumber(), request.medicationId());

        if (!medicationRepository.existsById(request.medicationId().trim())) {
            throw new MedicationNotFoundException(request.medicationId().trim());
        }

        if (batchRepository.findByMedicationIdAndBatchNumber(request.medicationId().trim(), request.batchNumber().trim()).isPresent()) {
            throw new BusinessRuleException(
                    "DUPLICATE_BATCH_NUMBER",
                    String.format("Batch '%s' already exists for medication '%s'.", request.batchNumber(), request.medicationId()),
                    HttpStatus.CONFLICT
            );
        }

        String id = UUID.randomUUID().toString();
        PharmacyInventoryBatch batch = new PharmacyInventoryBatch(
                id,
                request.medicationId().trim(),
                request.batchNumber().trim(),
                request.expiryDate(),
                request.quantityAvailable(),
                request.reorderThreshold() != null ? request.reorderThreshold() : 10
        );

        PharmacyInventoryBatch saved = batchRepository.save(batch);
        log.info("Inventory batch id='{}' created with quantity={}", saved.getId(), saved.getQuantityAvailable());
        return pharmacyMapper.toBatchResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyInventoryBatchResponse> getBatchesForMedication(String medicationId) {
        if (!medicationRepository.existsById(medicationId.trim())) {
            throw new MedicationNotFoundException(medicationId.trim());
        }
        return pharmacyMapper.toBatchResponseList(
                batchRepository.findByMedicationIdOrderByExpiryDateAsc(medicationId.trim())
        );
    }

    @Override
    @Transactional
    public DispenseRecordResponse dispenseMedication(DispenseMedicationRequest request) {
        log.info("Processing dispense request: prescriptionId='{}', itemId='{}', pharmacistId='{}', qty={}",
                request.prescriptionId(), request.prescriptionItemId(), request.pharmacistId(), request.quantityToDispense());

        validatePharmacist(request.pharmacistId());

        Prescription prescription = prescriptionRepository.findWithItemsById(request.prescriptionId().trim())
                .orElseThrow(() -> new PrescriptionNotFoundException(request.prescriptionId().trim()));

        if (prescription.getStatus() == PrescriptionStatus.CANCELLED || prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new BusinessRuleException(
                    "PRESCRIPTION_NOT_DISPENSABLE",
                    String.format("Prescription '%s' is in status '%s' and cannot be dispensed.",
                            prescription.getId(), prescription.getStatus()),
                    HttpStatus.CONFLICT
            );
        }

        PrescriptionItem item = prescriptionItemRepository.findById(request.prescriptionItemId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("PrescriptionItem", request.prescriptionItemId()));

        if (item.getStatus() == PrescriptionItemStatus.DISPENSED) {
            throw new BusinessRuleException(
                    "ITEM_ALREADY_DISPENSED",
                    String.format("Prescription item '%s' has already been fully dispensed.", item.getId()),
                    HttpStatus.CONFLICT
            );
        }

        // Concurrency-critical: Resolve batch and lock with PESSIMISTIC_WRITE lock (§26, §92)
        PharmacyInventoryBatch batch;
        if (request.inventoryBatchId() != null && !request.inventoryBatchId().isBlank()) {
            batch = batchRepository.findByIdForUpdate(request.inventoryBatchId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("PharmacyInventoryBatch", request.inventoryBatchId()));
        } else {
            // First-Expiring, First-Out (FEFO) candidate query
            List<PharmacyInventoryBatch> candidates = batchRepository
                    .findByMedicationIdAndExpiryDateAfterAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(
                            item.getMedicationId(), LocalDate.now(), 0
                    );
            if (candidates.isEmpty()) {
                throw new InsufficientInventoryException(
                        String.format("No active non-expired stock available for medication '%s'.", item.getMedicationId())
                );
            }
            // Lock the earliest-expiring batch
            batch = batchRepository.findByIdForUpdate(candidates.get(0).getId())
                    .orElseThrow(() -> new ResourceNotFoundException("PharmacyInventoryBatch", candidates.get(0).getId()));
        }

        // Atomic check and decrement under lock
        batch.decrementStock(request.quantityToDispense());
        batchRepository.saveAndFlush(batch);

        if (batch.isLowStock()) {
            log.warn("Inventory batch '{}' for medication '{}' is running LOW (remaining: {}, threshold: {})",
                    batch.getBatchNumber(), batch.getMedicationId(), batch.getQuantityAvailable(), batch.getReorderThreshold());
        }

        // Update prescription item and aggregate status
        item.recordDispense(request.quantityToDispense());
        prescriptionItemRepository.saveAndFlush(item);

        prescription.checkAndUpdateDispenseStatus();
        prescriptionRepository.save(prescription);

        // Record immutable dispense audit entry
        DispenseRecord record = new DispenseRecord(
                UUID.randomUUID().toString(),
                prescription.getId(),
                item.getId(),
                batch.getId(),
                request.pharmacistId().trim(),
                request.quantityToDispense(),
                Instant.now(),
                request.notes()
        );

        DispenseRecord savedRecord = dispenseRecordRepository.save(record);
        log.info("Successfully dispensed {} units of batch '{}' (remaining stock: {})",
                request.quantityToDispense(), batch.getBatchNumber(), batch.getQuantityAvailable());

        return pharmacyMapper.toDispenseResponse(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispenseRecordResponse> getDispensesForPrescription(String prescriptionId) {
        if (!prescriptionRepository.existsById(prescriptionId.trim())) {
            throw new PrescriptionNotFoundException(prescriptionId.trim());
        }
        return pharmacyMapper.toDispenseResponseList(
                dispenseRecordRepository.findByPrescriptionIdOrderByDispensedAtAsc(prescriptionId.trim())
        );
    }

    private void validatePharmacist(String pharmacistId) {
        StaffMember staff = staffMemberRepository.findById(pharmacistId.trim())
                .orElseThrow(() -> new StaffNotFoundException(pharmacistId));

        if (staff.getStaffType() != StaffType.PHARMACIST && staff.getStaffType() != StaffType.ADMIN) {
            throw new BusinessRuleException(
                    "INVALID_STAFF_TYPE",
                    String.format("Staff member '%s' is of type '%s', but only PHARMACIST or ADMIN can dispense medications.",
                            pharmacistId, staff.getStaffType()),
                    HttpStatus.FORBIDDEN
            );
        }
    }
}
