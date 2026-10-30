package com.toll.model.billing;

import com.toll.model.enums.VehicleType;

/**
 * Analytical DTO representing Query 1 Result:
 * Vehicle type revenue summary for categories with > 2 successful transactions, sorted by toll DESC.
 */
public class VehicleTypeRevenueReport {
    private final VehicleType vehicleType;
    private final long totalTransactions;
    private final double totalTollCollected;
    private final double averageToll;

    public VehicleTypeRevenueReport(VehicleType vehicleType, long totalTransactions, double totalTollCollected, double averageToll) {
        this.vehicleType = vehicleType;
        this.totalTransactions = totalTransactions;
        this.totalTollCollected = totalTollCollected;
        this.averageToll = averageToll;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public double getTotalTollCollected() {
        return totalTollCollected;
    }

    public double getAverageToll() {
        return averageToll;
    }

    @Override
    public String toString() {
        return String.format("| %-15s | %-12d | Rs. %-15.2f | Rs. %-15.2f |",
                vehicleType.name(), totalTransactions, totalTollCollected, averageToll);
    }
}
