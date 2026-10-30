package com.toll.service.tollengine;

import com.toll.model.enums.TransactionStatus;
import com.toll.model.transaction.TollTransaction;

/**
 * Actuator and system response for a lane crossing request.
 * Controls barrier opening, diversion routing, and transaction receipts.
 */
public class CrossingResponse {
    private final boolean barrierOpened;
    private final TollTransaction transaction;
    private final TransactionStatus status;
    private final String message;
    private final boolean divertedToCash;
    private final boolean penaltyApplied;

    public CrossingResponse(boolean barrierOpened,
                            TollTransaction transaction,
                            TransactionStatus status,
                            String message,
                            boolean divertedToCash,
                            boolean penaltyApplied) {
        this.barrierOpened = barrierOpened;
        this.transaction = transaction;
        this.status = status;
        this.message = message;
        this.divertedToCash = divertedToCash;
        this.penaltyApplied = penaltyApplied;
    }

    public boolean isBarrierOpened() {
        return barrierOpened;
    }

    public TollTransaction getTransaction() {
        return transaction;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isDivertedToCash() {
        return divertedToCash;
    }

    public boolean isPenaltyApplied() {
        return penaltyApplied;
    }

    @Override
    public String toString() {
        return String.format("CrossingResponse[Barrier: %s | Status: %s | Diverted: %s | Msg: %s]",
                barrierOpened ? "OPENED (GREEN)" : "CLOSED (RED)",
                status,
                divertedToCash ? "YES (TO CASH LANE)" : "NO",
                message);
    }
}
