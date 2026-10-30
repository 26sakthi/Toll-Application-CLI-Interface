package com.toll.model.pass;

import com.toll.model.enums.PassStatus;

import java.time.LocalDate;

/**
 * Entity representing an automated Monthly Toll Pass for a designated Toll Plaza.
 * Enables unlimited free crossings at the designated plaza during its validity period.
 */
public class MonthlyPass {
    private final String passId;
    private final String vehicleRegNo;
    private final int plazaId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double feePaid;
    private PassStatus status;

    public MonthlyPass(String passId, String vehicleRegNo, int plazaId, LocalDate startDate, LocalDate endDate, double feePaid) {
        if (passId == null || passId.trim().isEmpty()) {
            throw new IllegalArgumentException("Pass ID cannot be empty.");
        }
        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }
        if (plazaId <= 0) {
            throw new IllegalArgumentException("Plaza ID must be positive.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and End dates cannot be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }
        if (feePaid < 0) {
            throw new IllegalArgumentException("Fee paid cannot be negative.");
        }

        this.passId = passId.trim();
        this.vehicleRegNo = vehicleRegNo.trim().toUpperCase();
        this.plazaId = plazaId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.feePaid = feePaid;
        this.status = PassStatus.ACTIVE;
    }

    public String getPassId() {
        return passId;
    }

    public String getVehicleRegNo() {
        return vehicleRegNo;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getFeePaid() {
        return feePaid;
    }

    public synchronized PassStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(PassStatus status) {
        this.status = status;
    }

    /**
     * Checks if this pass is active and currently valid on the query date.
     */
    public synchronized boolean isValidOn(LocalDate queryDate) {
        if (queryDate == null) {
            return false;
        }
        if (this.status != PassStatus.ACTIVE) {
            return false;
        }
        return !queryDate.isBefore(startDate) && !queryDate.isAfter(endDate);
    }

    @Override
    public String toString() {
        return String.format("MonthlyPass[ID: %s | Vehicle: %s | Plaza: %d | Validity: %s to %s | Fee: Rs. %.2f | Status: %s]",
                passId, vehicleRegNo, plazaId, startDate, endDate, feePaid, status);
    }
}
