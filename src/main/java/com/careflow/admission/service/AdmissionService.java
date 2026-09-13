package com.careflow.admission.service;

import com.careflow.admission.domain.AdmissionStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service contract for inpatient hospital operations, ward layouts, bed allocation,
 * patient admissions, transfers, and discharges (§28, §57 Lab 4, §103 Phase 10).
 */
public interface AdmissionService {

    // Wards, Rooms & Beds
    WardResponse createWard(CreateWardRequest request);

    WardResponse getWardById(String wardId);

    Page<WardResponse> getWards(Pageable pageable);

    RoomResponse createRoom(CreateRoomRequest request);

    RoomResponse getRoomById(String roomId);

    List<RoomResponse> getRoomsByWard(String wardId);

    BedResponse createBed(CreateBedRequest request);

    BedResponse getBedById(String bedId);

    List<BedResponse> getAvailableBeds(String wardId);

    List<BedResponse> getBedsByRoom(String roomId);

    // Admissions & Bed Concurrency
    AdmissionResponse admitPatient(AdmitPatientRequest request);

    AdmissionResponse transferBed(String admissionId, TransferBedRequest request);

    AdmissionResponse dischargePatient(String admissionId, DischargePatientRequest request);

    AdmissionResponse cancelAdmission(String admissionId, String reason);

    AdmissionResponse getAdmissionById(String admissionId);

    AdmissionResponse getAdmissionByNumber(String admissionNumber);

    Page<AdmissionSummaryResponse> getAdmissionsByPatient(String patientId, Pageable pageable);

    Page<AdmissionSummaryResponse> getAdmissionsByStatus(AdmissionStatus status, Pageable pageable);

    Page<AdmissionSummaryResponse> getAllAdmissions(Pageable pageable);
}
