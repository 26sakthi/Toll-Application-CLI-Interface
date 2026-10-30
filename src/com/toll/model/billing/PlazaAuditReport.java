package com.toll.model.billing;

/**
 * Analytical DTO representing Query 2 Result:
 * Plaza crossings and revenue audit for plazas with at least 3 crossings.
 */
public class PlazaAuditReport {
    private final int plazaId;
    private final String plazaName;
    private final long totalCrossings;
    private final double totalRevenueCollected;
    private final long failedTransactions;

    public PlazaAuditReport(int plazaId, String plazaName, long totalCrossings, double totalRevenueCollected, long failedTransactions) {
        this.plazaId = plazaId;
        this.plazaName = plazaName;
        this.totalCrossings = totalCrossings;
        this.totalRevenueCollected = totalRevenueCollected;
        this.failedTransactions = failedTransactions;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public String getPlazaName() {
        return plazaName;
    }

    public long getTotalCrossings() {
        return totalCrossings;
    }

    public double getTotalRevenueCollected() {
        return totalRevenueCollected;
    }

    public long getFailedTransactions() {
        return failedTransactions;
    }

    @Override
    public String toString() {
        return String.format("| Plaza #%-4d | %-25s | Total: %-5d | Rev: Rs. %-10.2f | Failed: %-4d |",
                plazaId, plazaName, totalCrossings, totalRevenueCollected, failedTransactions);
    }
}
