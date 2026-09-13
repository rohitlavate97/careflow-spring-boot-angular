package com.careflow.document.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.document.domain.DocumentStatus;
import com.careflow.document.domain.DocumentType;
import com.careflow.document.domain.MedicalDocument;
import com.careflow.document.dto.DocumentResponse;
import com.careflow.document.dto.UploadDocumentRequest;
import com.careflow.document.dto.UploadDocumentVersionRequest;
import com.careflow.document.exception.DocumentNotFoundException;
import com.careflow.document.exception.InvalidDocumentStateException;
import com.careflow.document.mapper.DocumentMapper;
import com.careflow.document.repository.MedicalDocumentRepository;
import com.careflow.document.storage.StorageService;
import com.careflow.document.storage.StoredFile;
import com.careflow.patient.exception.PatientNotFoundException;
import com.careflow.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of DocumentService managing metadata persistence,
 * storage coordination, version lineage, and access audit (§34, §103 Phase 13).
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final MedicalDocumentRepository medicalDocumentRepository;
    private final PatientRepository patientRepository;
    private final StorageService storageService;
    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(MedicalDocumentRepository medicalDocumentRepository,
                               PatientRepository patientRepository,
                               StorageService storageService,
                               DocumentMapper documentMapper) {
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.patientRepository = patientRepository;
        this.storageService = storageService;
        this.documentMapper = documentMapper;
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, UploadDocumentRequest request, String uploadedById) {
        String patientId = request.patientId().trim();
        if (!patientRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        String subDirectory = "patients/" + patientId + "/" + LocalDate.now().getYear();
        StoredFile stored = storageService.store(file, subDirectory);

        String documentNumber = generateDocumentNumber();
        String documentId = UUID.randomUUID().toString();

        MedicalDocument document = new MedicalDocument(
                documentId,
                documentNumber,
                request.title().trim(),
                request.documentType(),
                patientId,
                uploadedById,
                stored.originalFileName(),
                stored.mimeType(),
                stored.fileSize(),
                stored.checksumSha256(),
                stored.storagePath(),
                1,
                null,
                DocumentStatus.ACTIVE,
                request.description(),
                request.referenceId(),
                request.referenceType()
        );

        MedicalDocument saved = medicalDocumentRepository.save(document);
        log.info("Medical document uploaded: docId={}, docNumber={}, patientId={}, uploader={}, checksum={}",
                saved.getId(), saved.getDocumentNumber(), saved.getPatientId(), uploadedById, saved.getChecksumSha256());

        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DocumentResponse uploadNewVersion(String documentId, MultipartFile file, UploadDocumentVersionRequest request, String uploadedById) {
        MedicalDocument existingDoc = medicalDocumentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (existingDoc.isDeleted()) {
            throw new InvalidDocumentStateException("Cannot create a new revision of a deleted document [" + documentId + "].");
        }

        String rootId = (existingDoc.getParentDocumentId() != null) ? existingDoc.getParentDocumentId() : existingDoc.getId();
        List<MedicalDocument> allVersions = medicalDocumentRepository.findAllVersionsForRoot(rootId);

        int nextVersion = allVersions.stream()
                .mapToInt(MedicalDocument::getDocumentVersion)
                .max()
                .orElse(1) + 1;

        // Archive all active versions in this lineage
        for (MedicalDocument doc : allVersions) {
            if (doc.isActive()) {
                doc.archive();
                medicalDocumentRepository.save(doc);
            }
        }

        String subDirectory = "patients/" + existingDoc.getPatientId() + "/" + LocalDate.now().getYear();
        StoredFile stored = storageService.store(file, subDirectory);

        String newDocumentNumber = generateDocumentNumber();
        String newId = UUID.randomUUID().toString();

        String newTitle = (request != null && request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : existingDoc.getTitle();

        String newDescription = (request != null && request.description() != null)
                ? request.description().trim()
                : existingDoc.getDescription();

        MedicalDocument newRevision = new MedicalDocument(
                newId,
                newDocumentNumber,
                newTitle,
                existingDoc.getDocumentType(),
                existingDoc.getPatientId(),
                uploadedById,
                stored.originalFileName(),
                stored.mimeType(),
                stored.fileSize(),
                stored.checksumSha256(),
                stored.storagePath(),
                nextVersion,
                rootId,
                DocumentStatus.ACTIVE,
                newDescription,
                existingDoc.getReferenceId(),
                existingDoc.getReferenceType()
        );

        MedicalDocument saved = medicalDocumentRepository.save(newRevision);
        log.info("New document revision uploaded: docId={}, docNumber={}, version={}, rootId={}, uploader={}",
                saved.getId(), saved.getDocumentNumber(), nextVersion, rootId, uploadedById);

        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(String id) {
        MedicalDocument document = medicalDocumentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(String id) {
        MedicalDocument document = medicalDocumentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        if (document.isDeleted()) {
            throw new InvalidDocumentStateException("Cannot download deleted document [" + id + "].");
        }

        Resource resource = storageService.loadAsResource(document.getStoragePath());
        log.info("Medical document downloaded: docId={}, docNumber={}, patientId={}, size={}",
                document.getId(), document.getDocumentNumber(), document.getPatientId(), document.getFileSize());

        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> getPatientDocuments(String patientId, DocumentType documentType, DocumentStatus status, Pageable pageable) {
        String cleanPatientId = patientId.trim();
        if (!patientRepository.existsById(cleanPatientId)) {
            throw new PatientNotFoundException(cleanPatientId);
        }

        DocumentStatus targetStatus = (status != null) ? status : DocumentStatus.ACTIVE;

        Page<MedicalDocument> page;
        if (documentType != null) {
            page = medicalDocumentRepository.findByPatientIdAndDocumentTypeAndStatus(
                    cleanPatientId, documentType, targetStatus, pageable);
        } else {
            page = medicalDocumentRepository.findByPatientIdAndStatus(
                    cleanPatientId, targetStatus, pageable);
        }

        return PageResponse.from(page, documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentHistory(String documentId) {
        MedicalDocument document = medicalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        String rootId = (document.getParentDocumentId() != null) ? document.getParentDocumentId() : document.getId();
        List<MedicalDocument> lineage = medicalDocumentRepository.findVersionHistory(rootId);

        return lineage.stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponse archiveDocument(String id) {
        MedicalDocument document = medicalDocumentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        document.archive();
        MedicalDocument saved = medicalDocumentRepository.save(document);
        log.info("Medical document archived: docId={}, docNumber={}", saved.getId(), saved.getDocumentNumber());

        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDocument(String id) {
        MedicalDocument document = medicalDocumentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        document.delete();
        medicalDocumentRepository.save(document);
        log.info("Medical document soft-deleted: docId={}, docNumber={}", document.getId(), document.getDocumentNumber());
    }

    private String generateDocumentNumber() {
        String documentNumber;
        do {
            documentNumber = "DOC-" + LocalDate.now().format(DATE_FORMATTER) + "-" +
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (medicalDocumentRepository.existsByDocumentNumber(documentNumber));
        return documentNumber;
    }
}
