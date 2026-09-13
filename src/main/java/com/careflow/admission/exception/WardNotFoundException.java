package com.careflow.admission.exception;

import com.careflow.common.exception.ResourceNotFoundException;

/**
 * Thrown when a hospital ward is not found.
 */
public class WardNotFoundException extends ResourceNotFoundException {

    public WardNotFoundException(String id) {
        super("Ward", id);
    }
}
