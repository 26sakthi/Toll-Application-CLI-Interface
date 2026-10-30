package com.toll.service.tollengine;

import com.toll.model.enums.VehicleType;

import java.time.LocalDateTime;

/**
 * Request payload capturing a vehicle approaching a toll lane sensor / RFID reader.
 */
public class CrossingRequest {
    private final int plazaId;
    private final int laneNumber;
    private final String tagId;
    private final String vehicleRegNo;
    private final VehicleType observedVehicleType; // Automated Vehicle Classifier (AVC) optical sensor reading
    private final LocalDateTime crossingTime;
    private final String cashOperatorName;

    public CrossingRequest(Builder builder) {
        this.plazaId = builder.plazaId;
        this.laneNumber = builder.laneNumber;
        this.tagId = builder.tagId;
        this.vehicleRegNo = builder.vehicleRegNo;
        this.observedVehicleType = builder.observedVehicleType;
        this.crossingTime = builder.crossingTime != null ? builder.crossingTime : LocalDateTime.now();
        this.cashOperatorName = builder.cashOperatorName;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public VehicleType getObservedVehicleType() {
        return observedVehicleType;
    }

    public LocalDateTime getCrossingTime() {
        return crossingTime;
    }

    public String getCashOperatorName() {
        return cashOperatorName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int plazaId;
        private int laneNumber;
        private String tagId;
        private String vehicleRegNo;
        private VehicleType observedVehicleType;
        private LocalDateTime crossingTime;
        private String cashOperatorName;

        public Builder plazaId(int plazaId) {
            this.plazaId = plazaId;
            return this;
        }

        public Builder laneNumber(int laneNumber) {
            this.laneNumber = laneNumber;
            return this;
        }

        public Builder tagId(String tagId) {
            this.tagId = tagId;
            return this;
        }

        public Builder vehicleRegNo(String vehicleRegNo) {
            this.vehicleRegNo = vehicleRegNo;
            return this;
        }

        public Builder observedVehicleType(VehicleType observedVehicleType) {
            this.observedVehicleType = observedVehicleType;
            return this;
        }

        public Builder crossingTime(LocalDateTime crossingTime) {
            this.crossingTime = crossingTime;
            return this;
        }

        public Builder cashOperatorName(String cashOperatorName) {
            this.cashOperatorName = cashOperatorName;
            return this;
        }

        public CrossingRequest build() {
            return new CrossingRequest(this);
        }
    }
}
