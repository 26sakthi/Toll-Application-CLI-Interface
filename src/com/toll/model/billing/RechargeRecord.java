package com.toll.model.billing;

import com.toll.model.enums.RechargeMethod;
import com.toll.model.enums.RechargeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity capturing a FASTag wallet recharge record.
 * Stores amount, date, method, and status.
 */
public class RechargeRecord {
    private final String rechargeId;
    private final String tagId;
    private final double amount;
    private final RechargeMethod method;
    private RechargeStatus status;
    private final LocalDateTime rechargeTime;

    public RechargeRecord(String tagId, double amount, RechargeMethod method, RechargeStatus status) {
        this(UUID.randomUUID().toString(), tagId, amount, method, status, LocalDateTime.now());
    }

    public RechargeRecord(String rechargeId, String tagId, double amount, RechargeMethod method, RechargeStatus status, LocalDateTime rechargeTime) {
        if (tagId == null || tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be empty.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        if (method == null) {
            throw new IllegalArgumentException("Recharge method cannot be null.");
        }

        this.rechargeId = rechargeId != null ? rechargeId : UUID.randomUUID().toString();
        this.tagId = tagId.trim().toUpperCase();
        this.amount = amount;
        this.method = method;
        this.status = status != null ? status : RechargeStatus.PAID;
        this.rechargeTime = rechargeTime != null ? rechargeTime : LocalDateTime.now();
    }

    public String getRechargeId() {
        return rechargeId;
    }

    public String getTagId() {
        return tagId;
    }

    public double getAmount() {
        return amount;
    }

    public RechargeMethod getMethod() {
        return method;
    }

    public synchronized RechargeStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(RechargeStatus status) {
        this.status = status;
    }

    public LocalDateTime getRechargeTime() {
        return rechargeTime;
    }

    @Override
    public String toString() {
        return String.format("RechargeRecord[ID: %s | Tag: %s | Rs. %.2f via %s | Status: %s | Time: %s]",
                rechargeId.substring(0, Math.min(8, rechargeId.length())),
                tagId, amount, method, status, rechargeTime);
    }
}
