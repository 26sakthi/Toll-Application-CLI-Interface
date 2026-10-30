package com.toll.service.pricing;

import com.toll.model.admin.Admin;
import com.toll.model.enums.VehicleType;
import com.toll.model.pass.MonthlyPass;
import com.toll.model.plaza.TollPlaza;
import com.toll.model.transaction.TollTransaction;
import com.toll.model.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service contract for Rates, Discounts and Passes (Module 4).
 * Demonstrates Abstraction and Pricing Algorithm Segregation.
 */
public interface IPricingService {

    /**
     * Issues a monthly pass for a vehicle at a designated plaza.
     * Business Rules:
     * - Only an Admin can issue/configure passes.
     * - A vehicle can have only one valid pass per plaza at a time.
     * - Pass fee is set per vehicle type.
     */
    MonthlyPass issueMonthlyPass(Admin admin, String vehicleRegNo, int plazaId, LocalDate startDate, int durationDays, double fee);

    /**
     * Retrieves any currently active monthly pass for a vehicle at a plaza.
     */
    MonthlyPass getValidMonthlyPass(String vehicleRegNo, int plazaId, LocalDate queryDate);

    /**
     * Calculates the applicable toll fare for a crossing.
     * Business Rules:
     * - Valid Monthly Pass => Rs. 0.00
     * - 24-Hour Return Trip => 50% of Single trip rate automatically.
     * - Vehicle Type Mismatch => Charges higher toll rate and flags for review.
     * - All final toll amounts rounded to nearest Rs. 5.
     */
    TollCalculationResult calculateToll(TollPlaza plaza,
                                        Vehicle vehicle,
                                        VehicleType observedType,
                                        LocalDateTime crossingTime,
                                        List<TollTransaction> recentTransactions);

    /**
     * Mathematically rounds a monetary value to the nearest Rs. 5.
     * Formula: Math.round(amount / 5.0) * 5.0
     */
    double roundToNearest5(double amount);

    /**
     * Configures the monthly pass fee for a vehicle type at a plaza.
     */
    void setPassFee(Admin admin, int plazaId, VehicleType vehicleType, double fee);

    /**
     * Gets configured pass fee for a vehicle type at a plaza.
     */
    double getPassFee(int plazaId, VehicleType vehicleType);

    /**
     * Returns an unmodifiable list of all issued monthly passes.
     */
    List<MonthlyPass> getAllPasses();
}
