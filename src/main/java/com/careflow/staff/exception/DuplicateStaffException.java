package com.careflow.staff.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a staff member violates unique constraints (staff code, user ID, email) (§17, §92).
 */
public class DuplicateStaffException extends BusinessRuleException {

    public static DuplicateStaffException forStaffCode(String staffCode) {
        return new DuplicateStaffException("DUPLICATE_STAFF_CODE",
                "Staff member with code '" + staffCode + "' already exists.");
    }

    public static DuplicateStaffException forEmail(String email) {
        return new DuplicateStaffException("DUPLICATE_STAFF_EMAIL",
                "Staff member with email '" + email + "' already exists.");
    }

    public static DuplicateStaffException forUserId(String userId) {
        return new DuplicateStaffException("DUPLICATE_STAFF_USER",
                "A staff profile is already linked to user ID '" + userId + "'.");
    }

    private DuplicateStaffException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
