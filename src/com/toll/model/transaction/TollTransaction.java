package com.toll.model.transaction;

import com.toll.model.enums.LaneType;
import com.toll.model.enums.PaymentType;
import com.toll.model.enums.TransactionStatus;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable entity capturing a toll plaza vehicle crossing record.
 * Fulfills NHAI / MoRTH audit trail and dispute resolution requirements.
 */
public class TollTransaction {
    private final String transactionId;
    private final String vehicleRegNo;
    private final int plazaId;
    private final String plazaName;
    private final int laneNumber;
    private final LaneType laneType;
    private final String tagId;
    private final LocalDateTime timestamp;
    private final double tollAmount;
    private final VehicleType registeredType;
    private final VehicleType observedType;
    private final VehicleType billedType;
    private final TripType tripType;
    private final PaymentType paymentType;
    private final TransactionStatus status;
    private final boolean isFlaggedForReview;
    private final String operatorName;
    private final String failureReason;

    public TollTransaction(Builder builder) {
        this.transactionId = builder.transactionId != null ? builder.transactionId : UUID.randomUUID().toString();
        this.vehicleRegNo = builder.vehicleRegNo;
        this.plazaId = builder.plazaId;
        this.plazaName = builder.plazaName;
        this.laneNumber = builder.laneNumber;
        this.laneType = builder.laneType;
        this.tagId = builder.tagId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.tollAmount = builder.tollAmount;
        this.registeredType = builder.registeredType;
        this.observedType = builder.observedType;
        this.billedType = builder.billedType;
        this.tripType = builder.tripType;
        this.paymentType = builder.paymentType;
        this.status = builder.status;
        this.isFlaggedForReview = builder.isFlaggedForReview;
        this.operatorName = builder.operatorName;
        this.failureReason = builder.failureReason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public String getPlazaName() {
        return plazaName;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public LaneType getLaneType() {
        return laneType;
    }

    public String getTagId() {
        return tagId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getTollAmount() {
        return tollAmount;
    }

    public VehicleType getRegisteredType() {
        return registeredType;
    }

    public VehicleType getObservedType() {
        return observedType;
    }

    public VehicleType getBilledType() {
        return billedType;
    }

    public TripType getTripType() {
        return tripType;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public boolean isFlaggedForReview() {
        return isFlaggedForReview;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionId;
        private String vehicleRegNo;
        private int plazaId;
        private String plazaName;
        private int laneNumber;
        private LaneType laneType;
        private String tagId;
        private LocalDateTime timestamp;
        private double tollAmount;
        private VehicleType registeredType;
        private VehicleType observedType;
        private VehicleType billedType;
        private TripType tripType;
        private PaymentType paymentType;
        private TransactionStatus status;
        private boolean isFlaggedForReview;
        private String operatorName;
        private String failureReason;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder vehicleRegNo(String vehicleRegNo) {
            this.vehicleRegNo = vehicleRegNo;
            return this;
        }

        public Builder plazaId(int plazaId) {
            this.plazaId = plazaId;
            return this;
        }

        public Builder plazaName(String plazaName) {
            this.plazaName = plazaName;
            return this;
        }

        public Builder laneNumber(int laneNumber) {
            this.laneNumber = laneNumber;
            return this;
        }

        public Builder laneType(LaneType laneType) {
            this.laneType = laneType;
            return this;
        }

        public Builder tagId(String tagId) {
            this.tagId = tagId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder tollAmount(double tollAmount) {
            this.tollAmount = tollAmount;
            return this;
        }

        public Builder registeredType(VehicleType registeredType) {
            this.registeredType = registeredType;
            return this;
        }

        public Builder observedType(VehicleType observedType) {
            this.observedType = observedType;
            return this;
        }

        public Builder billedType(VehicleType billedType) {
            this.billedType = billedType;
            return this;
        }

        public Builder tripType(TripType tripType) {
            this.tripType = tripType;
            return this;
        }

        public Builder paymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder isFlaggedForReview(boolean isFlaggedForReview) {
            this.isFlaggedForReview = isFlaggedForReview;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public TollTransaction build() {
            return new TollTransaction(this);
        }
    }

    @Override
    public String toString() {
        return String.format("Txn[%s | %s | Plaza #%d | L#%d | Rs. %.2f | %s | %s%s]",
                transactionId.substring(0, Math.min(8, transactionId.length())),
                vehicleRegNo,
                plazaId,
                laneNumber,
                tollAmount,
                paymentType,
                status,
                isFlaggedForReview ? " (FLAGGED)" : "");
    }
}
