package com.app.core.exception;

import java.util.Map;

/**
 * Thrown when request input validation fails.
 * Equivalent to ValidationException.php
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Validation Failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
