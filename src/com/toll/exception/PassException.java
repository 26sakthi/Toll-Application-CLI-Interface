package com.toll.exception;

/**
 * Exception thrown when monthly pass creation, renewal or validation fails.
 */
public class PassException extends TollManagementException {
    public PassException(String message) {
        super(message);
    }

    public PassException(String message, Throwable cause) {
        super(message, cause);
    }
}
