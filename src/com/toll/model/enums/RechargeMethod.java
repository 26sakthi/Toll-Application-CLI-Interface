package com.toll.model.enums;

/**
 * Payment instruments accepted for FASTag wallet recharge.
 */
public enum RechargeMethod {
    UPI("Unified Payments Interface (UPI)"),
    CARD("Debit / Credit Card"),
    NET_BANKING("Internet Banking");

    private final String description;

    RechargeMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
