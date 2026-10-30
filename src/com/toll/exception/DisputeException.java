package com.toll.exception;

/**
 * Exception thrown when dispute raising or processing fails.
 */
public class DisputeException extends TollManagementException {
    public DisputeException(String message) {
        super(message);
    }

    public DisputeException(String message, Throwable cause) {
        super(message, cause);
    }
}
