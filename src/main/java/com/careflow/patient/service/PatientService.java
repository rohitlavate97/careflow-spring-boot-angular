package com.careflow.patient.service;

import com.careflow.common.dto.PageResponse;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.PatientResponse;
import com.careflow.patient.dto.PatientSummaryResponse;
import com.careflow.patient.dto.UpdatePatientRequest;
import com.careflow.patient.dto.UpdatePatientStatusRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface governing Patient aggregate lifecycle and operations (§16, §88).
 */
public interface PatientService {

    /**
     * Registers a new patient with unique MRN generation and initial ACTIVE status.
     *
     * @param request Validated patient creation payload
     * @return Created patient response
     */
    PatientResponse registerPatient(CreatePatientRequest request);

    /**
     * Retrieves a patient by their unique internal ID.
     *
     * @param id Patient primary key
     * @return Patient response
     */
    PatientResponse getPatientById(String id);

    /**
     * Retrieves a patient by their unique Medical Record Number.
     *
     * @param mrn Medical Record Number
     * @return Patient response
     */
    PatientResponse getPatientByMrn(String mrn);

    /**
     * Updates an existing patient's demographic and contact information.
     *
     * @param id Patient primary key
     * @param request Validated patient update payload
     * @return Updated patient response
     */
    PatientResponse updatePatient(String id, UpdatePatientRequest request);

    /**
     * Transitions a patient's operational status according to clinical state rules.
     *
     * @param id Patient primary key
     * @param request Status update payload
     * @return Updated patient response
     */
    PatientResponse updatePatientStatus(String id, UpdatePatientStatusRequest request);

    /**
     * Searches and paginates patients using composite dynamic filters.
     *
     * @param query Optional free-text search (name, MRN, phone)
     * @param gender Optional gender filter
     * @param status Optional status filter
     * @param dateOfBirth Optional date of birth filter
     * @param pageable Pagination and sorting criteria
     * @return Paginated patient responses
     */
    PageResponse<PatientResponse> searchPatients(
            String query,
            Gender gender,
            PatientStatus status,
            LocalDate dateOfBirth,
            Pageable pageable
    );

    /**
     * Performs a lightweight quick search for dropdown autocompletion.
     *
     * @param query Search query matching name, MRN, or phone
     * @param limit Maximum results to return (default 10)
     * @return List of patient summaries
     */
    List<PatientSummaryResponse> quickSearch(String query, int limit);
}
