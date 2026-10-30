package com.toll.model.enums;

/**
 * Status of a Toll Plaza Monthly Pass.
 */
public enum PassStatus {
    ACTIVE("Active & Valid"),
    EXPIRED("Expired");

    private final String description;

    PassStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
