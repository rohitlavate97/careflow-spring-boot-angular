package com.careflow.laboratory.controller;

import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import com.careflow.laboratory.domain.AbnormalityFlag;
import com.careflow.laboratory.domain.LabOrderPriority;
import com.careflow.laboratory.domain.LabReviewStatus;
import com.careflow.laboratory.domain.LabTest;
import com.careflow.laboratory.domain.LabTestCategory;
import com.careflow.laboratory.domain.SpecimenType;
import com.careflow.laboratory.dto.CollectSampleRequest;
import com.careflow.laboratory.dto.CreateLabOrderItemRequest;
import com.careflow.laboratory.dto.CreateLabOrderRequest;
import com.careflow.laboratory.dto.EnterLabResultRequest;
import com.careflow.laboratory.dto.ReviewLabOrderRequest;
import com.careflow.laboratory.repository.LabOrderRepository;
import com.careflow.laboratory.repository.LabTestRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.repository.PatientRepository;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffType;
import com.careflow.staff.repository.DoctorProfileRepository;
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
import java.util.List;
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
class LaboratoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    private Patient patient;
    private StaffMember doctor;
    private StaffMember labTech;
    private LabTest labTest;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department(
                UUID.randomUUID().toString(), "PATH-TEST-" + UUID.randomUUID().toString().substring(0, 4),
                "Pathology Test", "Lab Testing", "Floor 1"
        ));

        doctor = new StaffMember(
                UUID.randomUUID().toString(), "DOC-TEST-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "Arthur", "Conan", "conan@careflow.local", "+1-555-0391",
                StaffType.DOCTOR, LocalDate.now()
        );

        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(), "General Diagnostics",
                "MBBS", "LIC-DIAG-" + UUID.randomUUID().toString().substring(0, 4),
                BigDecimal.valueOf(100.00), "Room 101", "Diagnostics"
        );
        doctor.setDoctorProfile(docProfile);
        doctor = staffMemberRepository.save(doctor);

        labTech = staffMemberRepository.save(new StaffMember(
                UUID.randomUUID().toString(), "TECH-TEST-" + UUID.randomUUID().toString().substring(0, 4),
                dept.getId(), "Rosalind", "Franklin", "rosalind@careflow.local", "+1-555-0392",
                StaffType.LAB_TECHNICIAN, LocalDate.now()
        ));

        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-LAB-" + UUID.randomUUID().toString().substring(0, 4),
                "Jane", "Doe", LocalDate.of(1990, 5, 15), Gender.FEMALE,
                "+1-555-0393"
        ));

        labTest = labTestRepository.save(new LabTest(
                UUID.randomUUID().toString(), "TEST-CBC-" + UUID.randomUUID().toString().substring(0, 4),
                "Complete Blood Count", LabTestCategory.HEMATOLOGY, SpecimenType.BLOOD,
                "4.5-11.0", "10^3/uL", 4, BigDecimal.valueOf(18.00), true
        ));
    }

    @Test
    @DisplayName("DOCTOR can create a diagnostic lab order")
    @WithMockUser(username = "doctor@careflow.local", roles = {"DOCTOR"})
    void createLabOrder_doctor_success() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest(
                patient.getId(), doctor.getId(), null, LabOrderPriority.ROUTINE, "Diagnostic check",
                List.of(new CreateLabOrderItemRequest(labTest.getId(), "Fasting"))
        );

        mockMvc.perform(post("/api/v1/laboratory/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/laboratory/orders/")))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("RECEPTIONIST cannot create a lab order (403 Forbidden)")
    @WithMockUser(username = "receptionist@careflow.local", roles = {"RECEPTIONIST"})
    void createLabOrder_receptionist_forbidden() throws Exception {
        CreateLabOrderRequest request = new CreateLabOrderRequest(
                patient.getId(), doctor.getId(), null, LabOrderPriority.ROUTINE, "Diagnostic check",
                List.of(new CreateLabOrderItemRequest(labTest.getId(), null))
        );

        mockMvc.perform(post("/api/v1/laboratory/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LAB_TECHNICIAN can collect sample, enter results, and auto-complete order")
    @WithMockUser(username = "lab@careflow.local", roles = {"LAB_TECHNICIAN", "DOCTOR"})
    void fullLabWorkflow_accessioningToResults() throws Exception {
        // 1. Doctor creates order
        CreateLabOrderRequest orderReq = new CreateLabOrderRequest(
                patient.getId(), doctor.getId(), null, LabOrderPriority.URGENT, "Urgent CBC",
                List.of(new CreateLabOrderItemRequest(labTest.getId(), null))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/laboratory/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderJson = createResult.getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderJson).get("id").asText();
        String orderItemId = objectMapper.readTree(orderJson).get("items").get(0).get("id").asText();

        // 2. Lab Tech collects specimen
        CollectSampleRequest sampleReq = new CollectSampleRequest(SpecimenType.BLOOD, "Vacutainer whole blood", labTech.getId());
        MvcResult sampleResult = mockMvc.perform(post("/api/v1/laboratory/orders/" + orderId + "/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COLLECTED"))
                .andReturn();

        String sampleId = objectMapper.readTree(sampleResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Lab Tech enters analytical result -> should complete the order
        EnterLabResultRequest resultReq = new EnterLabResultRequest(
                orderItemId, sampleId, "WBC", "6.5", 6.5, "10^3/uL", "4.5-11.0",
                AbnormalityFlag.NORMAL, "Normal white blood cell count", labTech.getId()
        );

        mockMvc.perform(post("/api/v1/laboratory/orders/" + orderId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resultReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testParameter").value("WBC"))
                .andExpect(jsonPath("$.abnormalityFlag").value("NORMAL"));

        // 4. Doctor reviews completed order
        ReviewLabOrderRequest reviewReq = new ReviewLabOrderRequest(LabReviewStatus.REVIEWED, "WBC within normal limits", doctor.getId());
        mockMvc.perform(post("/api/v1/laboratory/orders/" + orderId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reviewStatus").value("REVIEWED"))
                .andExpect(jsonPath("$.reviewedById").value(doctor.getId()));
    }

    @Test
    @DisplayName("Authenticated user can browse the lab test catalog")
    @WithMockUser(username = "doctor@careflow.local", roles = {"DOCTOR"})
    void getLabTests_authenticated_success() throws Exception {
        mockMvc.perform(get("/api/v1/laboratory/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
