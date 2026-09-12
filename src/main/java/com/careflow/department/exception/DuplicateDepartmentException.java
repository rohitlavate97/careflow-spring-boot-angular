package com.careflow.department.exception;

import com.careflow.common.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a department code or name violates uniqueness constraints (§17, §92).
 */
public class DuplicateDepartmentException extends BusinessRuleException {

    public static DuplicateDepartmentException forCode(String code) {
        return new DuplicateDepartmentException("DUPLICATE_DEPARTMENT_CODE",
                "Department with code '" + code + "' already exists.");
    }

    public static DuplicateDepartmentException forName(String name) {
        return new DuplicateDepartmentException("DUPLICATE_DEPARTMENT_NAME",
                "Department with name '" + name + "' already exists.");
    }

    private DuplicateDepartmentException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
