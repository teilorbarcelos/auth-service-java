package com.app.core.exception;

/**
 * Thrown for invalid request data.
 * Equivalent to BadRequestException.php
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
