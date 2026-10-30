package com.toll.model.billing;

/**
 * Analytical DTO representing Query 3 Result:
 * FASTag accounts that crossed at more than 2 distinct plazas, ordered by total amount deducted DESC.
 */
public class MultiPlazaAccountReport {
    private final String tagId;
    private final String vehicleRegNo;
    private final long totalCrossings;
    private final double totalAmountDeducted;
    private final long distinctPlazasUsed;

    public MultiPlazaAccountReport(String tagId, String vehicleRegNo, long totalCrossings, double totalAmountDeducted, long distinctPlazasUsed) {
        this.tagId = tagId;
        this.vehicleRegNo = vehicleRegNo;
        this.totalCrossings = totalCrossings;
        this.totalAmountDeducted = totalAmountDeducted;
        this.distinctPlazasUsed = distinctPlazasUsed;
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public long getTotalCrossings() {
        return totalCrossings;
    }

    public double getTotalAmountDeducted() {
        return totalAmountDeducted;
    }

    public long getDistinctPlazasUsed() {
        return distinctPlazasUsed;
    }

    @Override
    public String toString() {
        return String.format("| %-15s | %-15s | Crossings: %-4d | Distinct Plazas: %-3d | Total Deducted: Rs. %-10.2f |",
                tagId, vehicleRegNo, totalCrossings, distinctPlazasUsed, totalAmountDeducted);
    }
}
