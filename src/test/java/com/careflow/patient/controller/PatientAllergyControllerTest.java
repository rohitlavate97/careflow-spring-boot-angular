package com.careflow.patient.controller;

import com.careflow.patient.domain.AllergenCategory;
import com.careflow.patient.domain.AllergySeverity;
import com.careflow.patient.domain.AllergyStatus;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
import com.careflow.patient.dto.CreatePatientAllergyRequest;
import com.careflow.patient.dto.UpdateAllergyStatusRequest;
import com.careflow.patient.dto.UpdatePatientAllergyRequest;
import com.careflow.patient.repository.PatientRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class PatientAllergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testPatient = new Patient(
                UUID.randomUUID().toString(),
                "PAT-2026-88001",
                "Edward",
                "Jenner",
                LocalDate.of(1749, 5, 17),
                Gender.MALE,
                "+1-555-0876"
        );
        patientRepository.save(testPatient);
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/allergies should return 201 Created with Location header (§16, §89)")
    @WithMockUser(roles = "DOCTOR")
    void recordAllergy_shouldReturn201_whenValid() throws Exception {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Penicillin G",
                AllergenCategory.DRUG,
                AllergySeverity.LIFE_THREATENING,
                "Severe respiratory distress and urticaria",
                "Verified prior reaction at external clinic",
                LocalDate.of(2018, 6, 12)
        );

        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/patients/" + testPatient.getId() + "/allergies/")))
                .andExpect(jsonPath("$.allergen").value("Penicillin G"))
                .andExpect(jsonPath("$.category").value("DRUG"))
                .andExpect(jsonPath("$.severity").value("LIFE_THREATENING"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isHighRisk").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/allergies should return 400 Bad Request when validation fails (§41)")
    @WithMockUser(roles = "DOCTOR")
    void recordAllergy_shouldReturn400_whenPayloadInvalid() throws Exception {
        CreatePatientAllergyRequest invalidRequest = new CreatePatientAllergyRequest(
                "", // Blank allergen
                null, // Missing category
                null, // Missing severity
                "Reaction",
                null,
                LocalDate.now().plusDays(5) // Future date invalid
        );

        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/allergies should return 409 Conflict on duplicate active allergen (§16, §92)")
    @WithMockUser(roles = "NURSE")
    void recordAllergy_shouldReturn409_whenDuplicateActiveAllergen() throws Exception {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Ibuprofen",
                AllergenCategory.DRUG,
                AllergySeverity.MODERATE,
                "Facial swelling",
                null,
                null
        );

        // First creation succeeds
        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate active creation returns 409 Conflict
        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ALLERGY"))
                .andExpect(jsonPath("$.message", containsString("Ibuprofen")));
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/allergies should return 401 Unauthorized when unauthenticated (§44, §45)")
    void recordAllergy_shouldReturn401_whenUnauthenticated() throws Exception {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Latex",
                AllergenCategory.OTHER,
                AllergySeverity.MILD,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/allergies should return 403 Forbidden for unauthorized role (§44, §45)")
    @WithMockUser(roles = "BILLING_OFFICER")
    void recordAllergy_shouldReturn403_whenForbiddenRole() throws Exception {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Latex",
                AllergenCategory.OTHER,
                AllergySeverity.MILD,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/patients/{patientId}/allergies should return 200 OK for RECEPTIONIST (§44)")
    @WithMockUser(roles = "RECEPTIONIST")
    void getPatientAllergies_shouldReturn200_forReceptionist() throws Exception {
        mockMvc.perform(get("/api/v1/patients/" + testPatient.getId() + "/allergies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PUT and PATCH on allergy should update details and status (§16, §69)")
    @WithMockUser(roles = "DOCTOR")
    void updateAndTransitionAllergy_shouldSucceed() throws Exception {
        CreatePatientAllergyRequest createReq = new CreatePatientAllergyRequest(
                "Peanuts",
                AllergenCategory.FOOD,
                AllergySeverity.SEVERE,
                "Hives and lip edema",
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String allergyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Update details
        UpdatePatientAllergyRequest updateReq = new UpdatePatientAllergyRequest(
                AllergySeverity.LIFE_THREATENING,
                "Anaphylaxis confirmed via prick test",
                "Escalated classification",
                LocalDate.of(2022, 4, 1)
        );

        mockMvc.perform(put("/api/v1/patients/" + testPatient.getId() + "/allergies/" + allergyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("LIFE_THREATENING"))
                .andExpect(jsonPath("$.reaction").value("Anaphylaxis confirmed via prick test"));

        // Transition status to RESOLVED
        UpdateAllergyStatusRequest statusReq = new UpdateAllergyStatusRequest(
                AllergyStatus.RESOLVED,
                "Tolerance therapy complete"
        );

        mockMvc.perform(patch("/api/v1/patients/" + testPatient.getId() + "/allergies/" + allergyId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.isHighRisk").value(false));

        // Soft deactivation DELETE returns 204
        mockMvc.perform(delete("/api/v1/patients/" + testPatient.getId() + "/allergies/" + allergyId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/patients/{patientId}/allergies/alerts/high-risk should indicate high-risk allergy presence (§16)")
    @WithMockUser(roles = "DOCTOR")
    void getHighRiskAllergyStatus_shouldReturnBooleanFlag() throws Exception {
        CreatePatientAllergyRequest request = new CreatePatientAllergyRequest(
                "Sulfa Drugs",
                AllergenCategory.DRUG,
                AllergySeverity.SEVERE,
                "Stevens-Johnson syndrome risk",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/patients/" + testPatient.getId() + "/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients/" + testPatient.getId() + "/allergies/alerts/high-risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(testPatient.getId()))
                .andExpect(jsonPath("$.hasHighRiskAllergies").value(true));
    }
}
