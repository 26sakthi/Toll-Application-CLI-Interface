package com.toll.model.enums;

/**
 * Status lifecycle of a FASTag account.
 * ACTIVE: Tag is operational and can pay toll.
 * BLOCKED: Auto-blocked when balance falls below Rs. 100, or administratively blocked.
 * CLOSED: Permanently closed tag account.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    BLOCKED("Blocked (Balance < Rs. 100 or Administrative Hold)"),
    CLOSED("Permanently Closed");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
