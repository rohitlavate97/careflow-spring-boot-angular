package com.careflow.staff.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a staff member cannot be found by their identifier or staff code (§17).
 */
public class StaffNotFoundException extends ResourceNotFoundException {

    public StaffNotFoundException(String id) {
        super("StaffMember", id);
    }

    public static StaffNotFoundException forCode(String staffCode) {
        return new StaffNotFoundException("Staff member with code '" + staffCode + "' was not found.", true);
    }

    public static StaffNotFoundException forUserId(String userId) {
        return new StaffNotFoundException("Staff member linked to user ID '" + userId + "' was not found.", true);
    }

    private StaffNotFoundException(String message, boolean directMessage) {
        super(message);
    }
}
