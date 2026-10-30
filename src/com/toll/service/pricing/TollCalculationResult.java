package com.toll.service.pricing;

import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;

/**
 * Result object produced by the Pricing Engine.
 */
public class TollCalculationResult {
    private final double applicableAmount;
    private final VehicleType billedVehicleType;
    private final TripType tripType;
    private final boolean isReturnTripDiscountApplied;
    private final boolean isPassApplied;
    private final boolean isFlaggedForReview;
    private final String explanation;

    public TollCalculationResult(double applicableAmount,
                                 VehicleType billedVehicleType,
                                 TripType tripType,
                                 boolean isReturnTripDiscountApplied,
                                 boolean isPassApplied,
                                 boolean isFlaggedForReview,
                                 String explanation) {
        this.applicableAmount = applicableAmount;
        this.billedVehicleType = billedVehicleType;
        this.tripType = tripType;
        this.isReturnTripDiscountApplied = isReturnTripDiscountApplied;
        this.isPassApplied = isPassApplied;
        this.isFlaggedForReview = isFlaggedForReview;
        this.explanation = explanation;
    }

    public double getApplicableAmount() {
        return applicableAmount;
    }

    public VehicleType getBilledVehicleType() {
        return billedVehicleType;
    }

    public TripType getTripType() {
        return tripType;
    }

    public boolean isReturnTripDiscountApplied() {
        return isReturnTripDiscountApplied;
    }

    public boolean isPassApplied() {
        return isPassApplied;
    }

    public boolean isFlaggedForReview() {
        return isFlaggedForReview;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return String.format("TollResult[Amount: Rs. %.2f | Type: %s | Trip: %s | Pass: %s | ReturnDiscount: %s | Flagged: %s]",
                applicableAmount, billedVehicleType, tripType, isPassApplied, isReturnTripDiscountApplied, isFlaggedForReview);
    }
}
