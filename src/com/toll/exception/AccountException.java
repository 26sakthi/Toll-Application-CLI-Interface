package com.toll.exception;

/**
 * Exception thrown when an invalid vehicle or FASTag account operation occurs.
 */
public class AccountException extends TollManagementException {
    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
