package com.toll.model.enums;

/**
 * Payment method utilized for toll collection.
 */
public enum PaymentType {
    FASTAG("Electronic FASTag RFID Deduction"),
    CASH("Manual Cash Payment at Booth"),
    EXEMPT("Toll Exempt (Emergency / Authorized)");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
