package com.toll.model.enums;

/**
 * Status of a recharge transaction.
 */
public enum RechargeStatus {
    PAID("Payment Received & Credited"),
    FAILED("Payment Failed"),
    REFUNDED("Recharge Amount Refunded");

    private final String description;

    RechargeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
