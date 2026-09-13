package com.careflow.admission.controller;

import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.RoomType;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.domain.WardType;
import com.careflow.admission.dto.AdmitPatientRequest;
import com.careflow.admission.dto.DischargePatientRequest;
import com.careflow.admission.dto.TransferBedRequest;
import com.careflow.admission.repository.AdmissionRepository;
import com.careflow.admission.repository.BedRepository;
import com.careflow.admission.repository.RoomRepository;
import com.careflow.admission.repository.WardRepository;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.StaffMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AdmissionRepository admissionRepository;

    private Patient patient;
    private StaffMember doctor;
    private Ward ward;
    private Room room;
    private Bed bed1;
    private Bed bed2;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department(
                UUID.randomUUID().toString(), "ADM-DEPT-" + UUID.randomUUID().toString().substring(0, 4),
                "Admission Dept", "Inpatient Care", "Floor 4"
        ));

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-ADM-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "John", "Watson", "watson@careflow.local", "+1-555-0401",
                StaffType.DOCTOR, LocalDate.now()
        );
        DoctorProfile profile = new DoctorProfile(
                UUID.randomUUID().toString(), "Internal Medicine", "MD", "LIC-ADM-" + UUID.randomUUID().toString().substring(0, 4),
                BigDecimal.valueOf(150.00), "Room 401", "Inpatient Care"
        );
        doctor.setDoctorProfile(profile);
        doctor = staffMemberRepository.save(doctor);

        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-ADM-" + UUID.randomUUID().toString().substring(0, 4),
                "Oliver", "Twist", LocalDate.of(1995, 8, 12), Gender.MALE, "+1-555-0402"
        ));

        ward = wardRepository.save(new Ward(
                UUID.randomUUID().toString(), "WARD-MED-" + UUID.randomUUID().toString().substring(0, 4),
                "Medical Ward", dept.getId(), WardType.GENERAL, "Floor 4", 2, true
        ));

        room = roomRepository.save(new Room(
                UUID.randomUUID().toString(), "ROOM-401", ward, RoomType.SEMI_PRIVATE, true
        ));

        bed1 = bedRepository.save(new Bed(
                UUID.randomUUID().toString(), "BED-401-A", room, BedStatus.AVAILABLE, BigDecimal.valueOf(150.00), true
        ));

        bed2 = bedRepository.save(new Bed(
                UUID.randomUUID().toString(), "BED-401-B", room, BedStatus.AVAILABLE, BigDecimal.valueOf(150.00), true
        ));
    }

    @Test
    @DisplayName("DOCTOR can admit patient to available bed (201 Created)")
    @WithMockUser(username = "doctor@careflow.local", roles = {"DOCTOR"})
    void admitPatient_doctor_success() throws Exception {
        AdmitPatientRequest request = new AdmitPatientRequest(
                patient.getId(), doctor.getId(), bed1.getId(), null,
                "Severe dehydration", "Gastroenteritis"
        );

        mockMvc.perform(post("/api/v1/admissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/admissions/")))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.status").value("ADMITTED"))
                .andExpect(jsonPath("$.currentBedId").value(bed1.getId()));
    }

    @Test
    @DisplayName("RECEPTIONIST cannot admit patient (403 Forbidden)")
    @WithMockUser(username = "receptionist@careflow.local", roles = {"RECEPTIONIST"})
    void admitPatient_receptionist_forbidden() throws Exception {
        AdmitPatientRequest request = new AdmitPatientRequest(
                patient.getId(), doctor.getId(), bed1.getId(), null,
                "Admission by receptionist", null
        );

        mockMvc.perform(post("/api/v1/admissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Full Inpatient lifecycle: Admit -> Transfer -> Discharge")
    @WithMockUser(username = "doctor@careflow.local", roles = {"DOCTOR"})
    void fullInpatientJourney() throws Exception {
        // 1. Admit
        AdmitPatientRequest admitReq = new AdmitPatientRequest(
                patient.getId(), doctor.getId(), bed1.getId(), null,
                "Post-operative observation", "Appendectomy"
        );

        MvcResult admitResult = mockMvc.perform(post("/api/v1/admissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String admissionId = objectMapper.readTree(admitResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Transfer to Bed 2
        TransferBedRequest transferReq = new TransferBedRequest(bed2.getId(), "Patient requested window view", doctor.getId());
        mockMvc.perform(post("/api/v1/admissions/" + admissionId + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRANSFERRED"))
                .andExpect(jsonPath("$.currentBedId").value(bed2.getId()));

        // 3. Discharge
        DischargePatientRequest dischargeReq = new DischargePatientRequest("Patient discharged in stable condition");
        mockMvc.perform(post("/api/v1/admissions/" + admissionId + "/discharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dischargeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCHARGED"))
                .andExpect(jsonPath("$.currentBedId").isEmpty());
    }

    @Test
    @DisplayName("Authenticated user can browse available beds")
    @WithMockUser(username = "nurse@careflow.local", roles = {"NURSE"})
    void getAvailableBeds_success() throws Exception {
        mockMvc.perform(get("/api/v1/admissions/beds/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
