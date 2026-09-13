package com.careflow.insurance.mapper;

import com.careflow.insurance.domain.ClaimItem;
import com.careflow.insurance.domain.InsuranceClaim;
import com.careflow.insurance.domain.InsurancePolicy;
import com.careflow.insurance.domain.InsuranceProvider;
import com.careflow.insurance.dto.ClaimItemResponse;
import com.careflow.insurance.dto.ClaimSummaryResponse;
import com.careflow.insurance.dto.InsuranceClaimResponse;
import com.careflow.insurance.dto.InsurancePolicyResponse;
import com.careflow.insurance.dto.InsuranceProviderResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component mapping Insurance entities to response DTOs (§33).
 */
@Component
public class InsuranceMapper {

    public InsuranceProviderResponse toProviderResponse(InsuranceProvider provider) {
        if (provider == null) {
            return null;
        }
        return new InsuranceProviderResponse(
                provider.getId(),
                provider.getProviderCode(),
                provider.getName(),
                provider.getPayerId(),
                provider.getContactEmail(),
                provider.getContactPhone(),
                provider.getAddress(),
                provider.isActive(),
                provider.getCreatedAt()
        );
    }

    public InsurancePolicyResponse toPolicyResponse(InsurancePolicy policy) {
        if (policy == null) {
            return null;
        }
        InsuranceProvider provider = policy.getProvider();
        return new InsurancePolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getGroupNumber(),
                policy.getPatientId(),
                provider != null ? provider.getId() : null,
                provider != null ? provider.getName() : null,
                provider != null ? provider.getPayerId() : null,
                policy.getPolicyHolderName(),
                policy.getRelationship(),
                policy.getCoverageStartDate(),
                policy.getCoverageEndDate(),
                policy.getCoPayAmount(),
                policy.getCoveragePercentage(),
                policy.getDeductible(),
                policy.isActive(),
                policy.getCreatedAt()
        );
    }

    public ClaimItemResponse toClaimItemResponse(ClaimItem item) {
        if (item == null) {
            return null;
        }
        return new ClaimItemResponse(
                item.getId(),
                item.getClaim() != null ? item.getClaim().getId() : null,
                item.getInvoiceItemId(),
                item.getServiceCode(),
                item.getDescription(),
                item.getClaimedAmount(),
                item.getApprovedAmount(),
                item.getRejectionReason()
        );
    }

    public ClaimSummaryResponse toClaimSummaryResponse(InsuranceClaim claim) {
        if (claim == null) {
            return null;
        }
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicy() != null ? claim.getPolicy().getId() : null,
                claim.getPatientId(),
                claim.getInvoiceId(),
                claim.getStatus(),
                claim.getTotalClaimedAmount(),
                claim.getApprovedAmount(),
                claim.getPatientResponsibility(),
                claim.getSubmittedAt(),
                claim.getAdjudicatedAt(),
                claim.getSettledAt(),
                claim.getCreatedAt()
        );
    }

    public InsuranceClaimResponse toClaimResponse(InsuranceClaim claim) {
        if (claim == null) {
            return null;
        }
        InsurancePolicy policy = claim.getPolicy();
        InsuranceProvider provider = policy != null ? policy.getProvider() : null;

        List<ClaimItemResponse> items = claim.getItems() != null
                ? claim.getItems().stream().map(this::toClaimItemResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new InsuranceClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                policy != null ? policy.getId() : null,
                policy != null ? policy.getPolicyNumber() : null,
                provider != null ? provider.getName() : null,
                provider != null ? provider.getPayerId() : null,
                claim.getPatientId(),
                claim.getInvoiceId(),
                claim.getStatus(),
                claim.getTotalClaimedAmount(),
                claim.getApprovedAmount(),
                claim.getPatientResponsibility(),
                claim.getDenialReason(),
                claim.getAdjudicationNotes(),
                claim.getSubmittedAt(),
                claim.getAdjudicatedAt(),
                claim.getSettledAt(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                items
        );
    }
}
