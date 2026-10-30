package com.toll.exception;

/**
 * Exception thrown when a crossing or toll processing violation occurs.
 */
public class TollProcessingException extends TollManagementException {
    public TollProcessingException(String message) {
        super(message);
    }

    public TollProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
