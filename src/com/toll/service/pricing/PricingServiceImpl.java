package com.toll.service.pricing;

import com.toll.exception.PassException;
import com.toll.exception.UnauthorizedException;
import com.toll.model.admin.Admin;
import com.toll.model.enums.PassStatus;
import com.toll.model.enums.TransactionStatus;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;
import com.toll.model.pass.MonthlyPass;
import com.toll.model.plaza.TollPlaza;
import com.toll.model.plaza.TollRate;
import com.toll.model.transaction.TollTransaction;
import com.toll.model.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete implementation of IPricingService.
 * Implements 24-Hour Return Discounts, Monthly Pass Validations, Vehicle Mismatch Overrides, and Nearest Rs. 5 Rounding.
 */
public class PricingServiceImpl implements IPricingService {

    private final List<MonthlyPass> passes = new CopyOnWriteArrayList<>();
    private final Map<String, Double> passFeeConfig = new ConcurrentHashMap<>();
    private final AtomicInteger passIdGen = new AtomicInteger(5000);

    public PricingServiceImpl() {
        // Initialize default monthly pass fee table (approximating 30x single trip discount per MoRTH)
        for (VehicleType vt : VehicleType.values()) {
            double defaultFee = switch (vt) {
                case CAR -> 3300.0;
                case VAN -> 5300.0;
                case BUS -> 11000.0;
                case TRUCK -> 11000.0;
                case MULTI_AXLE -> 17500.0;
            };
            passFeeConfig.put("ALL_" + vt.name(), defaultFee);
        }
    }

    private void validateAdmin(Admin admin, String action) {
        if (admin == null || !admin.isActive()) {
            throw new UnauthorizedException("Unauthorized: Only an authorized Admin can " + action + ".");
        }
    }

    @Override
    public MonthlyPass issueMonthlyPass(Admin admin, String vehicleRegNo, int plazaId, LocalDate startDate, int durationDays, double fee) {
        validateAdmin(admin, "issue monthly toll passes");

        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (durationDays <= 0) {
            durationDays = 30; // standard calendar month
        }
        LocalDate endDate = startDate.plusDays(durationDays);
        String regNo = vehicleRegNo.trim().toUpperCase();

        // Business Rule: A vehicle can have only one valid pass per plaza at a time.
        for (MonthlyPass existingPass : passes) {
            if (existingPass.getVehicleRegNo().equalsIgnoreCase(regNo) &&
                    existingPass.getPlazaId() == plazaId &&
                    existingPass.getStatus() == PassStatus.ACTIVE) {
                // Check date overlap
                boolean overlaps = !startDate.isAfter(existingPass.getEndDate()) && !endDate.isBefore(existingPass.getStartDate());
                if (overlaps) {
                    throw new PassException(String.format(
                            "Pass Conflict: Vehicle %s already holds an active pass (%s) at Plaza #%d valid until %s.",
                            regNo, existingPass.getPassId(), plazaId, existingPass.getEndDate()));
                }
            }
        }

        String passId = "PASS-PLZ" + plazaId + "-" + passIdGen.incrementAndGet();
        MonthlyPass pass = new MonthlyPass(passId, regNo, plazaId, startDate, endDate, fee);
        passes.add(pass);
        return pass;
    }

    @Override
    public MonthlyPass getValidMonthlyPass(String vehicleRegNo, int plazaId, LocalDate queryDate) {
        if (vehicleRegNo == null || queryDate == null) {
            return null;
        }
        String normalizedReg = vehicleRegNo.trim().toUpperCase();
        for (MonthlyPass p : passes) {
            if (p.getVehicleRegNo().equalsIgnoreCase(normalizedReg) && p.getPlazaId() == plazaId) {
                if (p.isValidOn(queryDate)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public double roundToNearest5(double amount) {
        // Business Rule: The toll amount is rounded to the nearest Rs. 5.
        return Math.round(amount / 5.0) * 5.0;
    }

    @Override
    public void setPassFee(Admin admin, int plazaId, VehicleType vehicleType, double fee) {
        validateAdmin(admin, "configure monthly pass fees");
        if (fee < 0) {
            throw new IllegalArgumentException("Fee must be non-negative.");
        }
        passFeeConfig.put(plazaId + "_" + vehicleType.name(), fee);
    }

    @Override
    public double getPassFee(int plazaId, VehicleType vehicleType) {
        Double fee = passFeeConfig.get(plazaId + "_" + vehicleType.name());
        if (fee != null) {
            return fee;
        }
        return passFeeConfig.getOrDefault("ALL_" + vehicleType.name(), 3000.0);
    }

    @Override
    public List<MonthlyPass> getAllPasses() {
        return Collections.unmodifiableList(passes);
    }

    @Override
    public TollCalculationResult calculateToll(TollPlaza plaza,
                                               Vehicle vehicle,
                                               VehicleType observedType,
                                               LocalDateTime crossingTime,
                                               List<TollTransaction> recentTransactions) {
        if (plaza == null || vehicle == null || crossingTime == null) {
            throw new IllegalArgumentException("Plaza, vehicle, and crossing time must be non-null.");
        }

        LocalDate crossingDate = crossingTime.toLocalDate();

        // 1. Emergency vehicles: 100% exempt
        if (vehicle.isEmergencyVehicle()) {
            return new TollCalculationResult(
                    0.0,
                    vehicle.getVehicleType(),
                    TripType.EMERGENCY,
                    false,
                    false,
                    false,
                    "Emergency Vehicle Priority Bypass - 100% Toll Exempt"
            );
        }

        // 2. Check Monthly Pass
        MonthlyPass activePass = getValidMonthlyPass(vehicle.getRegistrationNumber(), plaza.getPlazaId(), crossingDate);
        if (activePass != null) {
            return new TollCalculationResult(
                    0.0,
                    vehicle.getVehicleType(),
                    TripType.PASS,
                    false,
                    true,
                    false,
                    "Covered by Active Monthly Pass [" + activePass.getPassId() + "]"
            );
        }

        // 3. Observed vehicle type mismatch detection
        VehicleType registeredType = vehicle.getVehicleType();
        boolean isMismatch = (observedType != null && observedType != registeredType);
        VehicleType billedType = registeredType;
        boolean isFlagged = false;
        String mismatchNote = "";

        if (isMismatch) {
            isFlagged = true;
            TollRate regRate = plaza.getEffectiveRate(registeredType, TripType.SINGLE, crossingDate);
            TollRate obsRate = plaza.getEffectiveRate(observedType, TripType.SINGLE, crossingDate);

            if (obsRate.getAmount() > regRate.getAmount()) {
                billedType = observedType;
                mismatchNote = String.format(" [AVC Mismatch: Registered as %s, Observed as %s. Higher rate applied]",
                        registeredType, observedType);
            } else {
                billedType = registeredType;
                mismatchNote = String.format(" [AVC Mismatch: Registered as %s, Observed as %s. Retained registered rate]",
                        registeredType, observedType);
            }
        }

        // 4. Check 24-Hour Return Trip Discount Engine
        // Business Rule: If a vehicle returns through the same plaza within 24 hours of a Single trip,
        // the Return trip rate (50% of the Single rate) is applied automatically.
        LocalDateTime windowStart = crossingTime.minusHours(24);
        boolean qualifiesForReturn = false;

        if (recentTransactions != null) {
            for (TollTransaction txn : recentTransactions) {
                if (txn.getPlazaId() == plaza.getPlazaId() &&
                        txn.getVehicleRegNo().equalsIgnoreCase(vehicle.getRegistrationNumber()) &&
                        txn.getStatus() == TransactionStatus.SUCCESS &&
                        txn.getTripType() == TripType.SINGLE &&
                        !txn.getTimestamp().isBefore(windowStart) &&
                        !txn.getTimestamp().isAfter(crossingTime)) {
                    qualifiesForReturn = true;
                    break;
                }
            }
        }

        double singleBaseRate = plaza.getEffectiveRate(billedType, TripType.SINGLE, crossingDate).getAmount();

        if (qualifiesForReturn) {
            // Apply return trip rate (50% of single rate)
            double returnFare = singleBaseRate * 0.50;
            double roundedFare = roundToNearest5(returnFare);
            return new TollCalculationResult(
                    roundedFare,
                    billedType,
                    TripType.RETURN,
                    true,
                    false,
                    isFlagged,
                    String.format("24-Hour Return Trip Discount Applied (50%% of Single Base Rs. %.2f -> Rs. %.2f)%s",
                            singleBaseRate, roundedFare, mismatchNote)
            );
        } else {
            // Standard Single Journey
            double roundedFare = roundToNearest5(singleBaseRate);
            return new TollCalculationResult(
                    roundedFare,
                    billedType,
                    TripType.SINGLE,
                    false,
                    false,
                    isFlagged,
                    String.format("Standard Single Trip Fare (Rs. %.2f)%s", roundedFare, mismatchNote)
            );
        }
    }
}
