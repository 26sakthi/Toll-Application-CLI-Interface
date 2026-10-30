package com.toll.model.enums;

/**
 * Status lifecycle of a customer dispute.
 */
public enum DisputeStatus {
    RAISED("Dispute Raised by Customer"),
    APPROVED("Dispute Approved by Admin - Refund Issued"),
    REJECTED("Dispute Rejected by Admin");

    private final String description;

    DisputeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
