package com.careflow.department.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a department cannot be found by its identifier or unique code (§17).
 */
public class DepartmentNotFoundException extends ResourceNotFoundException {

    public DepartmentNotFoundException(String id) {
        super("Department", id);
    }

    public static DepartmentNotFoundException forCode(String code) {
        return new DepartmentNotFoundException("Department with code '" + code + "' was not found.", true);
    }

    private DepartmentNotFoundException(String message, boolean directMessage) {
        super(message);
    }
}
