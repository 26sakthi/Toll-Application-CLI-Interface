package com.toll.model.enums;

/**
 * Status of a toll crossing transaction.
 */
public enum TransactionStatus {
    SUCCESS("Transaction Completed Successfully"),
    FAILED("Transaction Failed / Rejected"),
    PENDING("Transaction Processing Pending");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
