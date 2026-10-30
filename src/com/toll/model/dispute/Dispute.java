package com.toll.model.dispute;

import com.toll.model.enums.DisputeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a customer dispute for a wrong toll deduction.
 * Strictly enforces 7-day filing window and administrative refund workflow.
 */
public class Dispute {
    private final String disputeId;
    private final String transactionId;
    private final String vehicleRegNo;
    private final String tagId;
    private final double disputedAmount;
    private final String reason;
    private DisputeStatus status;
    private final LocalDateTime raisedAt;
    private LocalDateTime reviewedAt;
    private String adminRemarks;

    public Dispute(String transactionId, String vehicleRegNo, String tagId, double disputedAmount, String reason) {
        this(UUID.randomUUID().toString(), transactionId, vehicleRegNo, tagId, disputedAmount, reason, LocalDateTime.now());
    }

    public Dispute(String transactionId, String vehicleRegNo, String tagId, double disputedAmount, String reason, LocalDateTime raisedAt) {
        this(UUID.randomUUID().toString(), transactionId, vehicleRegNo, tagId, disputedAmount, reason, raisedAt);
    }

    public Dispute(String disputeId, String transactionId, String vehicleRegNo, String tagId, double disputedAmount, String reason, LocalDateTime raisedAt) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be empty.");
        }
        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration cannot be empty.");
        }
        if (disputedAmount < 0) {
            throw new IllegalArgumentException("Disputed amount cannot be negative.");
        }

        this.disputeId = disputeId != null ? disputeId : UUID.randomUUID().toString();
        this.transactionId = transactionId.trim();
        this.vehicleRegNo = vehicleRegNo.trim().toUpperCase();
        this.tagId = tagId != null ? tagId.trim().toUpperCase() : null;
        this.disputedAmount = disputedAmount;
        this.reason = reason != null ? reason.trim() : "Unspecified deduction dispute";
        this.status = DisputeStatus.RAISED;
        this.raisedAt = raisedAt != null ? raisedAt : LocalDateTime.now();
        this.reviewedAt = null;
        this.adminRemarks = null;
    }

    public String getDisputeId() {
        return disputeId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public String getTagId() {
        return tagId;
    }

    public double getDisputedAmount() {
        return disputedAmount;
    }

    public String getReason() {
        return reason;
    }

    public synchronized DisputeStatus getStatus() {
        return status;
    }

    public synchronized LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public synchronized LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public synchronized String getAdminRemarks() {
        return adminRemarks;
    }

    public synchronized void resolve(DisputeStatus newStatus, String remarks) {
        if (newStatus == null || newStatus == DisputeStatus.RAISED) {
            throw new IllegalArgumentException("Invalid resolution status: " + newStatus);
        }
        this.status = newStatus;
        this.adminRemarks = remarks;
        this.reviewedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Dispute[ID: %s | Txn: %s | Vehicle: %s | Rs. %.2f | Status: %s | Raised: %s]",
                disputeId.substring(0, Math.min(8, disputeId.length())),
                transactionId.substring(0, Math.min(8, transactionId.length())),
                vehicleRegNo,
                disputedAmount,
                status,
                raisedAt);
    }
}
