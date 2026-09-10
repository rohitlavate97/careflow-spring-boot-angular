package com.careflow.patient.controller;

import com.careflow.patient.domain.BloodGroup;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.PatientStatus;
import com.careflow.patient.dto.AddressDto;
import com.careflow.patient.dto.CreatePatientRequest;
import com.careflow.patient.dto.EmergencyContactDto;
import com.careflow.patient.dto.UpdatePatientRequest;
import com.careflow.patient.dto.UpdatePatientStatusRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("registerPatient should return 201 Created with Location header when payload is valid (§16, §89)")
    @WithMockUser(roles = "RECEPTIONIST")
    void registerPatient_shouldReturn201_whenValid() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest(
                "Albert",
                "A.",
                "Einstein",
                LocalDate.of(1879, 3, 14),
                Gender.MALE,
                BloodGroup.B_POSITIVE,
                "einstein@princeton.edu",
                "+1-555-0189",
                new AddressDto("112 Mercer Street", null, "Princeton", "NJ", "08540", "USA"),
                new EmergencyContactDto("Elsa Einstein", "Spouse", "+1-555-0190")
        );

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.mrn").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Albert A. Einstein"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.bloodGroup").value("B_POSITIVE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.address.city").value("Princeton"))
                .andExpect(jsonPath("$.emergencyContact.name").value("Elsa Einstein"));
    }

    @Test
    @DisplayName("registerPatient should return 400 Bad Request with field errors when payload is invalid (§42)")
    @WithMockUser(roles = "RECEPTIONIST")
    void registerPatient_shouldReturn400_whenValidationFails() throws Exception {
        CreatePatientRequest invalidRequest = new CreatePatientRequest(
                "", // Blank first name
                null,
                "", // Blank last name
                LocalDate.now().plusDays(1), // Future date of birth
                null, // Missing gender
                null,
                "not-an-email", // Malformed email
                "invalid-phone", // Invalid phone pattern
                null,
                null
        );

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("getPatientById should return 200 OK when patient exists")
    @WithMockUser(roles = "DOCTOR")
    void getPatientById_shouldReturn200_whenExists() throws Exception {
        String patientId = createSamplePatient("Isaac", "Newton", "+1-555-0199");

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId))
                .andExpect(jsonPath("$.fullName").value("Isaac Newton"));
    }

    @Test
    @DisplayName("getPatientById should return 404 Not Found when ID does not exist (§42)")
    @WithMockUser(roles = "DOCTOR")
    void getPatientById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{id}", "unknown-id-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("getPatientByMrn should return 200 OK when MRN matches (§16)")
    @WithMockUser(roles = "NURSE")
    void getPatientByMrn_shouldReturn200_whenMatches() throws Exception {
        String patientId = createSamplePatient("Galileo", "Galilei", "+1-555-0188");

        MvcResult result = mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String mrn = json.get("mrn").asText();

        mockMvc.perform(get("/api/v1/patients/mrn/{mrn}", mrn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrn").value(mrn))
                .andExpect(jsonPath("$.fullName").value("Galileo Galilei"));
    }

    @Test
    @DisplayName("updatePatient should return 200 OK with updated patient attributes")
    @WithMockUser(roles = "RECEPTIONIST")
    void updatePatient_shouldReturn200_whenUpdated() throws Exception {
        String patientId = createSamplePatient("Rosalind", "Franklin", "+1-555-0177");

        UpdatePatientRequest updateRequest = new UpdatePatientRequest(
                "Rosalind",
                "Elsie",
                "Franklin",
                LocalDate.of(1920, 7, 25),
                Gender.FEMALE,
                BloodGroup.O_NEGATIVE,
                "rosalind.franklin@dna.ac.uk",
                "+1-555-9988",
                new AddressDto("Kings College", null, "London", "Greater London", "WC2R 2LS", "UK"),
                new EmergencyContactDto("Ellis Franklin", "Father", "+1-555-9977")
        );

        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Rosalind Elsie Franklin"))
                .andExpect(jsonPath("$.phone").value("+1-555-9988"))
                .andExpect(jsonPath("$.bloodGroup").value("O_NEGATIVE"))
                .andExpect(jsonPath("$.address.city").value("London"));
    }

    @Test
    @DisplayName("updatePatientStatus to DECEASED should succeed for DOCTOR role (§69)")
    @WithMockUser(roles = "DOCTOR")
    void updatePatientStatus_shouldReturn200_whenDoctorMarksDeceased() throws Exception {
        String patientId = createSamplePatient("Niels", "Bohr", "+1-555-0166");

        UpdatePatientStatusRequest request = new UpdatePatientStatusRequest(
                PatientStatus.DECEASED, "Clinical confirmation of death"
        );

        mockMvc.perform(patch("/api/v1/patients/{id}/status", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEASED"));
    }

    @Test
    @DisplayName("updatePatientStatus should return 403 Forbidden when RECEPTIONIST attempts clinical status change (§91)")
    @WithMockUser(roles = "RECEPTIONIST")
    void updatePatientStatus_shouldReturn403_whenRoleLacksPermission() throws Exception {
        String patientId = createSamplePatient("Erwin", "Schrodinger", "+1-555-0155");

        UpdatePatientStatusRequest request = new UpdatePatientStatusRequest(
                PatientStatus.DECEASED, "Attempted receptionist marking"
        );

        mockMvc.perform(patch("/api/v1/patients/{id}/status", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("updatePatientStatus should return 422 Unprocessable Entity when resurrecting DECEASED patient (§69)")
    @WithMockUser(roles = "DOCTOR")
    void updatePatientStatus_shouldReturn422_whenTransitioningDeceasedToActive() throws Exception {
        String patientId = createSamplePatient("Lise", "Meitner", "+1-555-0144");

        // First mark deceased
        mockMvc.perform(patch("/api/v1/patients/{id}/status", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePatientStatusRequest(PatientStatus.DECEASED, "Death certificate"))))
                .andExpect(status().isOk());

        // Attempt reactivation (forbidden state machine transition)
        mockMvc.perform(patch("/api/v1/patients/{id}/status", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePatientStatusRequest(PatientStatus.ACTIVE, "Revive"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message", containsString("Cannot transition patient status from 'DECEASED' to 'ACTIVE'")));
    }

    @Test
    @DisplayName("searchPatients should return 200 OK with PageResponse structure (§41, §71)")
    @WithMockUser(roles = "RECEPTIONIST")
    void searchPatients_shouldReturnPaginatedResponse() throws Exception {
        createSamplePatient("Max", "Planck", "+1-555-0133");
        createSamplePatient("Werner", "Heisenberg", "+1-555-0122");

        mockMvc.perform(get("/api/v1/patients")
                        .param("query", "Planck")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].fullName").value("Max Planck"));
    }

    @Test
    @DisplayName("quickSearch should return compact summaries for autocompletion (§111)")
    @WithMockUser(roles = "RECEPTIONIST")
    void quickSearch_shouldReturnSummaries() throws Exception {
        createSamplePatient("Enrico", "Fermi", "+1-555-0111");

        mockMvc.perform(get("/api/v1/patients/quick-search")
                        .param("query", "Fermi")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fullName").value("Enrico Fermi"))
                .andExpect(jsonPath("$[0].mrn").isNotEmpty());
    }

    @Test
    @DisplayName("unauthenticated request should return 401 Unauthorized (§44)")
    void unauthenticatedRequest_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private String createSamplePatient(String firstName, String lastName, String phone) throws Exception {
        CreatePatientRequest request = new CreatePatientRequest(
                firstName,
                null,
                lastName,
                LocalDate.of(1980, 1, 1),
                Gender.OTHER,
                BloodGroup.AB_POSITIVE,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com",
                phone,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }
}
