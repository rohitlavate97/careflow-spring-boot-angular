package com.careflow.patient.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an active allergy record already exists for the specified allergen (§16, §92).
 */
public class DuplicateAllergyException extends BusinessRuleException {

    public DuplicateAllergyException(String allergen) {
        super("DUPLICATE_ALLERGY", "Patient already has an active allergy record for allergen: '" + allergen + "'", HttpStatus.CONFLICT);
    }
}
