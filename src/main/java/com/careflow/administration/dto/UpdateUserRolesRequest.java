package com.careflow.administration.dto;

import com.careflow.identity.domain.RoleType;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Request payload for updating the security roles granted to a user account (§38).
 */
public record UpdateUserRolesRequest(
        @NotEmpty(message = "At least one role must be assigned")
        Set<RoleType> roles
) {}
