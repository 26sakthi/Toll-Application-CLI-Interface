package com.toll.model.billing;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity capturing a Low Balance Alert.
 * Triggered automatically when wallet balance drops below Rs. 200 after deduction.
 */
public class LowBalanceAlert {
    private final String alertId;
    private final String tagId;
    private final String vehicleRegNo;
    private final double remainingBalance;
    private final LocalDateTime alertTime;

    public LowBalanceAlert(String tagId, String vehicleRegNo, double remainingBalance) {
        this(UUID.randomUUID().toString(), tagId, vehicleRegNo, remainingBalance, LocalDateTime.now());
    }

    public LowBalanceAlert(String alertId, String tagId, String vehicleRegNo, double remainingBalance, LocalDateTime alertTime) {
        this.alertId = alertId != null ? alertId : UUID.randomUUID().toString();
        this.tagId = tagId;
        this.vehicleRegNo = vehicleRegNo;
        this.remainingBalance = remainingBalance;
        this.alertTime = alertTime != null ? alertTime : LocalDateTime.now();
    }

    public String getAlertId() {
        return alertId;
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public double getRemainingBalance() {
        return remainingBalance;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    @Override
    public String toString() {
        return String.format("LowBalanceAlert[Tag: %s | Vehicle: %s | Remaining Bal: Rs. %.2f (< Rs. 200) | Time: %s]",
                tagId, vehicleRegNo, remainingBalance, alertTime);
    }
}
