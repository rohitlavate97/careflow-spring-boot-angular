package com.careflow.insurance.controller;

import com.careflow.insurance.domain.ClaimStatus;
import com.careflow.insurance.domain.InsuranceClaim;
import com.careflow.insurance.domain.InsurancePolicy;
import com.careflow.insurance.domain.InsuranceProvider;
import com.careflow.insurance.domain.PolicyRelationship;
import com.careflow.insurance.dto.AdjudicateClaimRequest;
import com.careflow.insurance.dto.CreateClaimItemRequest;
import com.careflow.insurance.dto.CreateClaimRequest;
import com.careflow.insurance.dto.CreateInsurancePolicyRequest;
import com.careflow.insurance.dto.CreateInsuranceProviderRequest;
import com.careflow.insurance.repository.InsuranceClaimRepository;
import com.careflow.insurance.repository.InsurancePolicyRepository;
import com.careflow.insurance.repository.InsuranceProviderRepository;
import com.careflow.patient.domain.Gender;
import com.careflow.patient.domain.Patient;
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
class InsuranceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InsuranceProviderRepository providerRepository;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private InsuranceClaimRepository claimRepository;

    private Patient patient;
    private InsuranceProvider provider;
    private InsurancePolicy policy;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(new Patient(
                UUID.randomUUID().toString(), "MRN-INS-CTRL-" + UUID.randomUUID().toString().substring(0, 4),
                "Grace", "Hopper", LocalDate.of(1980, 1, 1), Gender.FEMALE, "+1-555-0955"
        ));

        provider = providerRepository.save(new InsuranceProvider(
                UUID.randomUUID().toString(), "PROV-CTRL-" + UUID.randomUUID().toString().substring(0, 4),
                "Cigna Health", "PAYER-CIGNA", "claims@cigna.demo", "+1-800-555-0956", "Bloomfield, CT", true
        ));

        policy = policyRepository.save(new InsurancePolicy(
                UUID.randomUUID().toString(), "POL-CTRL-999", "GRP-CTRL", patient.getId(), provider,
                "Grace Hopper", PolicyRelationship.SELF, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10),
                BigDecimal.valueOf(20.00), BigDecimal.valueOf(80.00), BigDecimal.valueOf(500.00), true
        ));
    }

    @Test
    @DisplayName("BILLING_OFFICER can register insurance provider (201 Created)")
    @WithMockUser(username = "billing@careflow.local", roles = {"BILLING_OFFICER"})
    void createProvider_billingOfficer_success() throws Exception {
        CreateInsuranceProviderRequest request = new CreateInsuranceProviderRequest(
                "PAYER-UHC-" + UUID.randomUUID().toString().substring(0, 4),
                "UnitedHealthcare", "PAYER-87726", "claims@uhc.demo", "+1-800-555-0999", "Minnetonka, MN"
        );

        mockMvc.perform(post("/api/v1/insurance/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/insurance/providers/")))
                .andExpect(jsonPath("$.name").value("UnitedHealthcare"));
    }

    @Test
    @DisplayName("PATIENT cannot create insurance provider (403 Forbidden)")
    @WithMockUser(username = "patient@careflow.local", roles = {"PATIENT"})
    void createProvider_patient_forbidden() throws Exception {
        CreateInsuranceProviderRequest request = new CreateInsuranceProviderRequest(
                "PAYER-BAD", "Unauthorized Payer", "PAYER-999", null, null, null
        );

        mockMvc.perform(post("/api/v1/insurance/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BILLING_OFFICER can register patient policy (201 Created)")
    @WithMockUser(username = "billing@careflow.local", roles = {"BILLING_OFFICER"})
    void createPolicy_billingOfficer_success() throws Exception {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest(
                "POL-NEW-888", "GRP-NEW", patient.getId(), provider.getId(), "Grace Hopper",
                PolicyRelationship.SELF, LocalDate.now(), LocalDate.now().plusYears(1),
                BigDecimal.valueOf(30.00), BigDecimal.valueOf(80.00), BigDecimal.valueOf(750.00)
        );

        mockMvc.perform(post("/api/v1/insurance/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/insurance/policies/")))
                .andExpect(jsonPath("$.policyNumber").value("POL-NEW-888"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()));
    }

    @Test
    @DisplayName("Full Claim Lifecycle: Create -> Submit -> Review -> Adjudicate -> Settle")
    @WithMockUser(username = "billing@careflow.local", roles = {"BILLING_OFFICER"})
    void fullClaimLifecycle_success() throws Exception {
        // 1. Create claim
        CreateClaimRequest createReq = new CreateClaimRequest(
                policy.getId(), patient.getId(), null,
                List.of(new CreateClaimItemRequest(null, "CPT-99214", "Detailed Consultation", BigDecimal.valueOf(180.00)))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/insurance/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalClaimedAmount").value(180.00))
                .andReturn();

        String claimId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Submit claim
        mockMvc.perform(post("/api/v1/insurance/claims/" + claimId + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        // 3. Start review
        mockMvc.perform(post("/api/v1/insurance/claims/" + claimId + "/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        // 4. Adjudicate claim (PARTIALLY_APPROVED: 144.00 approved, 36.00 patient resp)
        AdjudicateClaimRequest adjReq = new AdjudicateClaimRequest(
                ClaimStatus.PARTIALLY_APPROVED, BigDecimal.valueOf(144.00), "80% coverage under policy terms", null
        );
        mockMvc.perform(post("/api/v1/insurance/claims/" + claimId + "/adjudicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(144.00))
                .andExpect(jsonPath("$.patientResponsibility").value(36.00));

        // 5. Settle claim
        mockMvc.perform(post("/api/v1/insurance/claims/" + claimId + "/settle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.settledAt").isNotEmpty());
    }

    @Test
    @DisplayName("PATIENT can view own policies and claims (200 OK)")
    @WithMockUser(username = "patient@careflow.local", roles = {"PATIENT"})
    void getPoliciesAndClaims_patient_success() throws Exception {
        mockMvc.perform(get("/api/v1/insurance/policies/patient/" + patient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/insurance/claims/patient/" + patient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/insurance/claims/patient/" + patient.getId()))
                .andExpect(status().isUnauthorized());
    }
}
