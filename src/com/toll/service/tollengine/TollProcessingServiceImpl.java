package com.toll.service.tollengine;

import com.toll.exception.TollProcessingException;
import com.toll.model.account.FASTagAccount;
import com.toll.model.billing.LowBalanceAlert;
import com.toll.model.billing.PaymentContext;
import com.toll.model.billing.PaymentResult;
import com.toll.model.enums.*;
import com.toll.model.plaza.Lane;
import com.toll.model.plaza.TollPlaza;
import com.toll.model.transaction.TollTransaction;
import com.toll.model.vehicle.Vehicle;
import com.toll.service.account.IAccountService;
import com.toll.service.billing.IBillingReportingService;
import com.toll.service.billing.payment.CashPaymentMethod;
import com.toll.service.billing.payment.FASTagPaymentMethod;
import com.toll.service.billing.payment.PaymentMethod;
import com.toll.service.plaza.IPlazaService;
import com.toll.service.pricing.IPricingService;
import com.toll.service.pricing.TollCalculationResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Concrete implementation of ITollProcessingService.
 * Executes the full real-time crossing pipeline with barrier controls,
 * 2-minute cooldowns, AVC optical mismatch handling, and cash fallback routing.
 */
public class TollProcessingServiceImpl implements ITollProcessingService {

    private final IPlazaService plazaService;
    private final IAccountService accountService;
    private final IPricingService pricingService;
    private final IBillingReportingService billingService;

    private final PaymentMethod fastagPaymentMethod = new FASTagPaymentMethod();
    private final PaymentMethod cashPaymentMethod = new CashPaymentMethod();

    public TollProcessingServiceImpl(IPlazaService plazaService,
                                      IAccountService accountService,
                                      IPricingService pricingService,
                                      IBillingReportingService billingService) {
        this.plazaService = plazaService;
        this.accountService = accountService;
        this.pricingService = pricingService;
        this.billingService = billingService;
    }

    @Override
    public CrossingResponse processCrossing(CrossingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Crossing request cannot be null.");
        }

        TollPlaza plaza = plazaService.getPlaza(request.getPlazaId());
        if (!plaza.isOpen()) {
            throw new TollProcessingException("Toll Plaza '" + plaza.getName() + "' (ID: " + plaza.getPlazaId() + ") is currently CLOSED.");
        }

        Lane lane = plaza.getLane(request.getLaneNumber());
        if (lane.getStatus() == LaneStatus.CLOSED) {
            throw new TollProcessingException("Lane #" + lane.getLaneNumber() + " at Plaza '" + plaza.getName() + "' is currently CLOSED.");
        }

        // Engage vehicle processing lock on lane
        lane.startProcessingVehicle();
        try {
            return executeCrossingPipeline(plaza, lane, request);
        } finally {
            // Guarantee vehicle processing lock release when crossing sequence finishes
            lane.finishProcessingVehicle();
        }
    }

    private CrossingResponse executeCrossingPipeline(TollPlaza plaza, Lane lane, CrossingRequest request) {
        LocalDateTime crossingTime = request.getCrossingTime();

        // ----------------------------------------------------------------------------------------
        // BRANCH 1: EMERGENCY LANE
        // Business Rule: Emergency vehicles (ambulance, fire engine) use Emergency lane and pay nothing.
        // ----------------------------------------------------------------------------------------
        if (lane.getLaneType() == LaneType.EMERGENCY) {
            Vehicle vehicle = resolveVehicle(request);
            if (vehicle != null && vehicle.isEmergencyVehicle()) {
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(vehicle.getRegistrationNumber())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(request.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle.getVehicleType())
                        .observedType(request.getObservedVehicleType() != null ? request.getObservedVehicleType() : vehicle.getVehicleType())
                        .billedType(vehicle.getVehicleType())
                        .tripType(TripType.EMERGENCY)
                        .paymentType(PaymentType.EXEMPT)
                        .status(TransactionStatus.SUCCESS)
                        .isFlaggedForReview(false)
                        .operatorName("Emergency Automated Gate Actuator")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                        "EMERGENCY PRIORITY BYPASS: Boom barrier opened (Toll = Rs. 0.00).", false, false);
            } else {
                // Non-emergency vehicle illegally attempting to use Emergency Lane
                String regNo = vehicle != null ? vehicle.getRegistrationNumber() : "UNKNOWN";
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(regNo)
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(request.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .observedType(request.getObservedVehicleType())
                        .billedType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .tripType(TripType.EMERGENCY)
                        .paymentType(PaymentType.EXEMPT)
                        .status(TransactionStatus.FAILED)
                        .isFlaggedForReview(true)
                        .failureReason("UNAUTHORIZED_EMERGENCY_LANE_ACCESS")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(false, txn, TransactionStatus.FAILED,
                        "CROSSING REJECTED: Lane #3 is reserved strictly for Emergency Services.", false, false);
            }
        }

        // ----------------------------------------------------------------------------------------
        // BRANCH 2: FASTAG LANE
        // ----------------------------------------------------------------------------------------
        if (lane.getLaneType() == LaneType.FASTAG) {
            FASTagAccount account = resolveAccount(request);
            Vehicle vehicle = resolveVehicle(request);

            if (vehicle == null && account != null) {
                vehicle = accountService.getVehicle(account.getVehicleRegNo());
            }

            // Case 2A: No valid active FASTag attached
            // Business Rule: A vehicle without a valid Active tag is sent to a Cash lane and pays double the toll.
            if (account == null || account.getStatus() == AccountStatus.CLOSED) {
                VehicleType effectiveType = (request.getObservedVehicleType() != null)
                        ? request.getObservedVehicleType()
                        : (vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR);
                double baseSingleRate = plaza.getEffectiveRate(effectiveType, TripType.SINGLE, crossingTime.toLocalDate()).getAmount();
                double doubleTollPenalty = pricingService.roundToNearest5(baseSingleRate * 2.0);

                String regNo = vehicle != null ? vehicle.getRegistrationNumber() : (request.getVehicleRegNo() != null ? request.getVehicleRegNo() : "UNREGISTERED");
                String operator = request.getCashOperatorName() != null ? request.getCashOperatorName() : "Suresh Yadav (POS Operator)";

                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(regNo)
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(null)
                        .timestamp(crossingTime)
                        .tollAmount(doubleTollPenalty)
                        .registeredType(vehicle != null ? vehicle.getVehicleType() : effectiveType)
                        .observedType(request.getObservedVehicleType())
                        .billedType(effectiveType)
                        .tripType(TripType.SINGLE)
                        .paymentType(PaymentType.CASH)
                        .status(TransactionStatus.SUCCESS)
                        .isFlaggedForReview(true)
                        .operatorName(operator)
                        .failureReason("NO_VALID_FASTAG_DIVERTED_TO_CASH_2X_PENALTY")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                        String.format("FASTag Lane Barrier kept closed. Diverted to Cash Lane: Paid DOUBLE TOLL (Rs. %.2f) in cash to Operator %s.",
                                doubleTollPenalty, operator), true, true);
            }

            // Case 2B: Blacklisted FASTag
            // Business Rule: An Admin can blacklist a tag. A blacklisted tag is refused at every plaza.
            if (account.isBlacklisted()) {
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .observedType(request.getObservedVehicleType())
                        .billedType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .tripType(TripType.SINGLE)
                        .paymentType(PaymentType.FASTAG)
                        .status(TransactionStatus.FAILED)
                        .isFlaggedForReview(true)
                        .failureReason("TAG_BLACKLISTED (" + account.getBlacklistReason() + ")")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(false, txn, TransactionStatus.FAILED,
                        "ALARM TRIGGERED: FASTag " + account.getTagId() + " is BLACKLISTED (" + account.getBlacklistReason() + "). Crossing refused.",
                        false, false);
            }

            // Case 2C: 2-Minute Anti-Double-Billing Cooldown
            // Business Rule: The same tag cannot be charged twice at the same plaza within 2 minutes.
            LocalDateTime cooldownLimit = crossingTime.minusMinutes(2);
            boolean isDuplicateAttempt = billingService.getAllTransactions().stream()
                    .filter(t -> t.getPlazaId() == plaza.getPlazaId())
                    .filter(t -> t.getTagId() != null && t.getTagId().equalsIgnoreCase(account.getTagId()))
                    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                    .anyMatch(t -> !t.getTimestamp().isBefore(cooldownLimit) && !t.getTimestamp().isAfter(crossingTime));

            if (isDuplicateAttempt) {
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .observedType(request.getObservedVehicleType())
                        .billedType(vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR)
                        .tripType(TripType.SINGLE)
                        .paymentType(PaymentType.FASTAG)
                        .status(TransactionStatus.FAILED)
                        .isFlaggedForReview(false)
                        .failureReason("ANTI_DOUBLE_BILLING_2_MIN_COOLDOWN")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(false, txn, TransactionStatus.FAILED,
                        "CROSSING REJECTED: Tag " + account.getTagId() + " was already charged at this plaza within the last 2 minutes.",
                        false, false);
            }

            // Case 2D: Price calculation (Pass, 24h Return Discount, Vehicle Type Mismatch)
            TollCalculationResult calcResult = pricingService.calculateToll(
                    plaza,
                    vehicle,
                    request.getObservedVehicleType(),
                    crossingTime,
                    billingService.getAllTransactions()
            );

            // Case 2E: Monthly Pass Active (Toll = Rs. 0.00)
            if (calcResult.isPassApplied()) {
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle.getVehicleType())
                        .observedType(request.getObservedVehicleType())
                        .billedType(calcResult.getBilledVehicleType())
                        .tripType(TripType.PASS)
                        .paymentType(PaymentType.EXEMPT)
                        .status(TransactionStatus.SUCCESS)
                        .isFlaggedForReview(false)
                        .operatorName("Automated RFID Sensor")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                        "MONTHLY PASS VALID: Barrier opened (Rs. 0.00). " + calcResult.getExplanation(), false, false);
            }

            double requiredToll = calcResult.getApplicableAmount();

            // Case 2F: Insufficient balance or Account Blocked (< Rs. 100)
            // Business Rule: If tag balance is not enough, barrier does not open. Sent to Cash lane and pays normal toll in cash.
            if (account.getStatus() == AccountStatus.BLOCKED || account.getBalance() < requiredToll) {
                String operator = request.getCashOperatorName() != null ? request.getCashOperatorName() : "Suresh Yadav (POS Operator)";

                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(requiredToll)
                        .registeredType(vehicle.getVehicleType())
                        .observedType(request.getObservedVehicleType())
                        .billedType(calcResult.getBilledVehicleType())
                        .tripType(calcResult.getTripType())
                        .paymentType(PaymentType.CASH)
                        .status(TransactionStatus.SUCCESS)
                        .isFlaggedForReview(calcResult.isFlaggedForReview())
                        .operatorName(operator)
                        .failureReason("INSUFFICIENT_FASTAG_BALANCE_DIVERTED_NORMAL_CASH")
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                        String.format("FASTag barrier kept closed (Tag balance Rs. %.2f < required Rs. %.2f). Diverted to Cash Lane: Paid NORMAL TOLL (Rs. %.2f) in cash to Operator %s.",
                                account.getBalance(), requiredToll, requiredToll, operator), true, false);
            }

            // Case 2G: Sufficient Balance & Active Account -> Deduct & Open Barrier
            PaymentContext payContext = new PaymentContext(requiredToll, account, null, calcResult.getExplanation());
            PaymentResult payResult = fastagPaymentMethod.processPayment(payContext);

            if (payResult.isSuccess()) {
                // Check Low Balance Alert trigger (< Rs. 200)
                LowBalanceAlert alert = billingService.checkAndTriggerLowBalanceAlert(
                        account.getTagId(), vehicle.getRegistrationNumber(), account.getBalance()
                );

                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(requiredToll)
                        .registeredType(vehicle.getVehicleType())
                        .observedType(request.getObservedVehicleType())
                        .billedType(calcResult.getBilledVehicleType())
                        .tripType(calcResult.getTripType())
                        .paymentType(PaymentType.FASTAG)
                        .status(TransactionStatus.SUCCESS)
                        .isFlaggedForReview(calcResult.isFlaggedForReview())
                        .operatorName("ETC RFID Transceiver")
                        .build();

                billingService.recordTransaction(txn);
                String alertMsg = (alert != null) ? String.format(" [WARNING: Low Balance Alert generated! Balance is Rs. %.2f < Rs. 200]", account.getBalance()) : "";
                return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                        String.format("FASTag deduction successful (Rs. %.2f debited). Barrier opened.%s", requiredToll, alertMsg),
                        false, false);
            } else {
                TollTransaction txn = TollTransaction.builder()
                        .vehicleRegNo(account.getVehicleRegNo())
                        .plazaId(plaza.getPlazaId())
                        .plazaName(plaza.getName())
                        .laneNumber(lane.getLaneNumber())
                        .laneType(lane.getLaneType())
                        .tagId(account.getTagId())
                        .timestamp(crossingTime)
                        .tollAmount(0.0)
                        .registeredType(vehicle.getVehicleType())
                        .observedType(request.getObservedVehicleType())
                        .billedType(calcResult.getBilledVehicleType())
                        .tripType(calcResult.getTripType())
                        .paymentType(PaymentType.FASTAG)
                        .status(TransactionStatus.FAILED)
                        .isFlaggedForReview(false)
                        .failureReason(payResult.getMessage())
                        .build();

                billingService.recordTransaction(txn);
                return new CrossingResponse(false, txn, TransactionStatus.FAILED,
                        "Deduction failed: " + payResult.getMessage(), false, false);
            }
        }

        // ----------------------------------------------------------------------------------------
        // BRANCH 3: CASH LANE (Manual Toll Booth)
        // ----------------------------------------------------------------------------------------
        Vehicle vehicle = resolveVehicle(request);
        VehicleType effectiveType = request.getObservedVehicleType() != null
                ? request.getObservedVehicleType()
                : (vehicle != null ? vehicle.getVehicleType() : VehicleType.CAR);

        TollCalculationResult calcResult = pricingService.calculateToll(
                plaza,
                vehicle,
                request.getObservedVehicleType(),
                crossingTime,
                billingService.getAllTransactions()
        );

        String operator = request.getCashOperatorName() != null ? request.getCashOperatorName() : "Suresh Yadav (POS Operator)";
        double cashAmount = calcResult.getApplicableAmount();

        PaymentContext payContext = new PaymentContext(cashAmount, null, operator, "Cash Booth Toll Collection");
        PaymentResult payResult = cashPaymentMethod.processPayment(payContext);

        TollTransaction txn = TollTransaction.builder()
                .vehicleRegNo(vehicle != null ? vehicle.getRegistrationNumber() : (request.getVehicleRegNo() != null ? request.getVehicleRegNo() : "CASH-CAR-01"))
                .plazaId(plaza.getPlazaId())
                .plazaName(plaza.getName())
                .laneNumber(lane.getLaneNumber())
                .laneType(lane.getLaneType())
                .tagId(request.getTagId())
                .timestamp(crossingTime)
                .tollAmount(cashAmount)
                .registeredType(vehicle != null ? vehicle.getVehicleType() : effectiveType)
                .observedType(request.getObservedVehicleType())
                .billedType(calcResult.getBilledVehicleType())
                .tripType(calcResult.getTripType())
                .paymentType(PaymentType.CASH)
                .status(TransactionStatus.SUCCESS)
                .isFlaggedForReview(calcResult.isFlaggedForReview())
                .operatorName(operator)
                .build();

        billingService.recordTransaction(txn);
        return new CrossingResponse(true, txn, TransactionStatus.SUCCESS,
                String.format("Cash toll payment of Rs. %.2f collected by Operator '%s'. Boom barrier opened.",
                        cashAmount, operator), false, false);
    }

    private FASTagAccount resolveAccount(CrossingRequest request) {
        if (request.getTagId() != null && !request.getTagId().trim().isEmpty()) {
            try {
                return accountService.getAccountByTagId(request.getTagId());
            } catch (Exception e) {
                // tagId might not exist
            }
        }
        if (request.getVehicleRegNo() != null && !request.getVehicleRegNo().trim().isEmpty()) {
            return accountService.getAccountByVehicleReg(request.getVehicleRegNo());
        }
        return null;
    }

    private Vehicle resolveVehicle(CrossingRequest request) {
        if (request.getVehicleRegNo() != null && !request.getVehicleRegNo().trim().isEmpty()) {
            try {
                return accountService.getVehicle(request.getVehicleRegNo());
            } catch (Exception e) {
                // vehicle may not be found
            }
        }
        if (request.getTagId() != null && !request.getTagId().trim().isEmpty()) {
            try {
                FASTagAccount account = accountService.getAccountByTagId(request.getTagId());
                if (account != null) {
                    return accountService.getVehicle(account.getVehicleRegNo());
                }
            } catch (Exception e) {
                // account not found
            }
        }
        return null;
    }
}
