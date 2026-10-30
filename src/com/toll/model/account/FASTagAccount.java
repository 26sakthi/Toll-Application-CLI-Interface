package com.toll.model.account;

import com.toll.exception.AccountException;
import com.toll.model.enums.AccountStatus;

import java.time.LocalDateTime;

/**
 * Entity representing an RFID FASTag electronic toll account.
 * Demonstrates strict Encapsulation, Business Rule Invariants, and Automated State Transitions.
 */
public class FASTagAccount {
    public static final double MIN_BALANCE_THRESHOLD = 100.0;
    public static final double MAX_ACCOUNT_BALANCE = 50000.0;
    public static final double MIN_RECHARGE_AMOUNT = 100.0;
    public static final double MAX_RECHARGE_AMOUNT = 10000.0;

    private final String tagId;
    private final String vehicleRegNo;
    private double balance;
    private AccountStatus status;
    private boolean isBlacklisted;
    private String blacklistReason;
    private LocalDateTime blockedAt;

    public FASTagAccount(String tagId, String vehicleRegNo, double initialBalance) {
        if (tagId == null || tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be empty.");
        }
        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }
        if (initialBalance < 0) {
            throw new AccountException("Balance can never be negative. Provided: " + initialBalance);
        }
        if (initialBalance > MAX_ACCOUNT_BALANCE) {
            throw new AccountException("Initial balance cannot exceed Rs. " + MAX_ACCOUNT_BALANCE);
        }

        this.tagId = tagId.trim().toUpperCase();
        this.vehicleRegNo = vehicleRegNo.trim().toUpperCase();
        this.balance = initialBalance;
        this.isBlacklisted = false;
        this.blacklistReason = null;

        // Auto-block if initial balance < 100, otherwise ACTIVE
        if (this.balance < MIN_BALANCE_THRESHOLD) {
            this.status = AccountStatus.BLOCKED;
            this.blockedAt = LocalDateTime.now();
        } else {
            this.status = AccountStatus.ACTIVE;
            this.blockedAt = null;
        }
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized boolean isBlacklisted() {
        return isBlacklisted;
    }

    public synchronized String getBlacklistReason() {
        return blacklistReason;
    }

    public synchronized LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public synchronized boolean isOperational() {
        return status == AccountStatus.ACTIVE && !isBlacklisted;
    }

    /**
     * Debits toll amount from account balance.
     * Business Rules:
     * - Balance can never be negative.
     * - Auto-blocks automatically when balance falls below Rs. 100.
     */
    public synchronized void debit(double amount) {
        if (isBlacklisted) {
            throw new AccountException("Tag " + tagId + " is BLACKLISTED (" + blacklistReason + ") and cannot be debited.");
        }
        if (status == AccountStatus.CLOSED) {
            throw new AccountException("Tag " + tagId + " is CLOSED.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Debit amount must be non-negative: " + amount);
        }
        if (amount == 0.0) {
            return;
        }
        if (this.balance < amount) {
            throw new AccountException(String.format(
                    "Insufficient balance on Tag %s: Current Balance = Rs. %.2f, Required = Rs. %.2f",
                    tagId, balance, amount));
        }

        this.balance -= amount;

        // Auto-block check
        if (this.balance < MIN_BALANCE_THRESHOLD && this.status == AccountStatus.ACTIVE) {
            this.status = AccountStatus.BLOCKED;
            this.blockedAt = LocalDateTime.now();
        }
    }

    /**
     * Recharges the FASTag wallet.
     * Business Rules:
     * - Recharge must be between Rs. 100 and Rs. 10,000.
     * - Balance cannot go above Rs. 50,000.
     * - A blocked account becomes Active again automatically when a recharge takes balance to Rs. 100 or more.
     */
    public synchronized void recharge(double amount) {
        if (status == AccountStatus.CLOSED) {
            throw new AccountException("Cannot recharge permanently CLOSED account: " + tagId);
        }
        if (amount < MIN_RECHARGE_AMOUNT || amount > MAX_RECHARGE_AMOUNT) {
            throw new AccountException(String.format(
                    "Recharge amount must be between Rs. %.2f and Rs. %.2f. Attempted: Rs. %.2f",
                    MIN_RECHARGE_AMOUNT, MAX_RECHARGE_AMOUNT, amount));
        }
        if (this.balance + amount > MAX_ACCOUNT_BALANCE) {
            throw new AccountException(String.format(
                    "Recharge rejected: Wallet balance cannot exceed Rs. %.2f. Current: Rs. %.2f, Attempted: Rs. %.2f (Total = Rs. %.2f)",
                    MAX_ACCOUNT_BALANCE, balance, amount, (balance + amount)));
        }

        this.balance += amount;

        // Auto-unblock check
        if (this.status == AccountStatus.BLOCKED && this.balance >= MIN_BALANCE_THRESHOLD && !isBlacklisted) {
            this.status = AccountStatus.ACTIVE;
            this.blockedAt = null;
        }
    }

    /**
     * Refunds money back to tag balance (e.g. from an approved dispute).
     */
    public synchronized void refund(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive.");
        }
        this.balance = Math.min(this.balance + amount, MAX_ACCOUNT_BALANCE);

        // Auto-unblock check
        if (this.status == AccountStatus.BLOCKED && this.balance >= MIN_BALANCE_THRESHOLD && !isBlacklisted) {
            this.status = AccountStatus.ACTIVE;
            this.blockedAt = null;
        }
    }

    /**
     * Flags tag as blacklisted (stolen / fraudulent).
     * Refused at every plaza.
     */
    public synchronized void blacklist(String reason) {
        this.isBlacklisted = true;
        this.blacklistReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Flagged by Admin";
        this.status = AccountStatus.BLOCKED;
        this.blockedAt = LocalDateTime.now();
    }

    /**
     * Removes blacklist status from tag.
     */
    public synchronized void unblacklist() {
        this.isBlacklisted = false;
        this.blacklistReason = null;
        if (this.balance >= MIN_BALANCE_THRESHOLD) {
            this.status = AccountStatus.ACTIVE;
            this.blockedAt = null;
        } else {
            this.status = AccountStatus.BLOCKED;
        }
    }

    /**
     * Permanently closes the account.
     */
    public synchronized void closeAccount() {
        this.status = AccountStatus.CLOSED;
    }

    @Override
    public String toString() {
        return String.format("FASTagAccount[Tag: %s | Vehicle: %s | Bal: Rs. %.2f | Status: %s%s]",
                tagId,
                vehicleRegNo,
                balance,
                status,
                isBlacklisted ? " (BLACKLISTED)" : "");
    }
}
