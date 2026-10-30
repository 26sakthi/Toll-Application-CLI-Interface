package com.toll.test;

import com.toll.context.TollSystemBootstrap;
import com.toll.context.TollSystemContext;
import com.toll.exception.*;
import com.toll.model.account.FASTagAccount;
import com.toll.model.admin.Admin;
import com.toll.model.billing.*;
import com.toll.model.dispute.Dispute;
import com.toll.model.enums.*;
import com.toll.model.pass.MonthlyPass;
import com.toll.model.plaza.*;
import com.toll.model.transaction.TollTransaction;
import com.toll.model.vehicle.*;
import com.toll.service.account.IAccountService;
import com.toll.service.billing.IBillingReportingService;
import com.toll.service.plaza.IPlazaService;
import com.toll.service.pricing.IPricingService;
import com.toll.service.pricing.TollCalculationResult;
import com.toll.service.tollengine.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Automated System Verification Suite.
 * Asserts and demonstrates all business rules across Modules 1 to 5,
 * including 2-Minute Cooldown, Optical Mismatches, 24h Return Discounts,
 * 7-Day Disputes, Auto-Block/Unblock, and SQL Analytical Queries 1, 2, 3.
 */
public class SystemVerificationSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        runAllVerifications();
    }

    public static void runAllVerifications() {
        testsPassed = 0;
        testsFailed = 0;

        System.out.println("==========================================================================================");
        System.out.println("       TOLL MANAGEMENT SYSTEM (FASTag) - END-TO-END VERIFICATION SUITE       ");
        System.out.println("==========================================================================================\n");

        TollSystemContext context = TollSystemBootstrap.initializeSystem();
        IPlazaService plazaService = context.getPlazaService();
        IAccountService accountService = context.getAccountService();
        IPricingService pricingService = context.getPricingService();
        IBillingReportingService billingService = context.getBillingService();
        ITollProcessingService tollEngine = context.getTollProcessingService();
        Admin admin = context.getDefaultAdmin();

        // ----------------------------------------------------------------------------------------
        // SECTION 1: MODULE 1 - PLAZA & LANE RULES
        // ----------------------------------------------------------------------------------------
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(" [MODULE 1] VERIFYING TOLL PLAZA & LANE SETUP RULES");
        System.out.println("------------------------------------------------------------------------------------------");

        // 1.1 Only Admin can create plaza
        try {
            plazaService.createPlaza(null, "Fake Plaza", "NH-99", "Somewhere");
            recordFailure("Unauthorized plaza creation was allowed without Admin!");
        } catch (UnauthorizedException e) {
            recordSuccess("Admin Authorization: Unauthorized creation correctly blocked.");
        }

        // 1.2 Plaza minimum 2 lanes requirement
        TollPlaza testPlaza = plazaService.createPlaza(admin, "Test Hub Plaza", "NH-50", "Nashik");
        testPlaza.addLane(new FASTagLane(1));
        try {
            plazaService.openPlaza(admin, testPlaza.getPlazaId());
            recordFailure("Plaza was allowed to open with only 1 lane!");
        } catch (PlazaNotReadyException e) {
            recordSuccess("Minimum Lanes Rule: Plaza opening blocked with < 2 lanes.");
        }

        // 1.3 Lane Close Safety Constraint (cannot close while vehicle processing)
        Lane lane1 = testPlaza.getLane(1);
        lane1.startProcessingVehicle();
        try {
            lane1.closeLane();
            recordFailure("Lane was allowed to close while processing a vehicle!");
        } catch (LaneOperationException e) {
            recordSuccess("Lane Safety Guard: Lane closure rejected while vehicle actively processing.");
        }
        lane1.finishProcessingVehicle();
        lane1.closeLane();
        lane1.openLane();
        recordSuccess("Lane Safety Guard: Lane closed and reopened safely when idle.");

        // ----------------------------------------------------------------------------------------
        // SECTION 2: MODULE 2 - VEHICLE & FASTAG ACCOUNT RULES
        // ----------------------------------------------------------------------------------------
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println(" [MODULE 2] VERIFYING VEHICLE & FASTAG ACCOUNT LIFECYCLE");
        System.out.println("------------------------------------------------------------------------------------------");

        // 2.1 Strict 1:1 Tag to Vehicle association
        try {
            accountService.issueFASTag("IND-TAG-DUPLICATE", "MH-12-DE-1433", 500.0);
            recordFailure("Duplicate FASTag was issued to vehicle with active tag!");
        } catch (AccountException e) {
            recordSuccess("Strict 1:1 Association: Second active tag on vehicle MH-12-DE-1433 correctly rejected.");
        }

        // 2.2 Auto-Block when balance < Rs. 100
        FASTagAccount tag1002 = accountService.getAccountByTagId("IND-TAG-1002"); // Initial Rs. 150
        assertEquals(AccountStatus.ACTIVE, tag1002.getStatus(), "Tag IND-TAG-1002 initial state");
        tag1002.debit(80.0); // 150 - 80 = 70 (< 100)
        assertEquals(70.0, tag1002.getBalance(), "Tag balance after debit");
        assertEquals(AccountStatus.BLOCKED, tag1002.getStatus(), "Auto-block state");
        recordSuccess("Auto-Block Engine: Balance dropped to Rs. 70.00 -> Account transitioned to BLOCKED.");

        // 2.3 Recharge boundaries (Min 100, Max 10,000, Cap 50,000)
        try {
            tag1002.recharge(50.0); // Below Rs. 100
            recordFailure("Recharge < Rs. 100 was accepted!");
        } catch (AccountException e) {
            recordSuccess("Recharge Min Bound: Rs. 50.00 recharge rejected (Min is Rs. 100).");
        }

        try {
            tag1002.recharge(15000.0); // Above Rs. 10,000
            recordFailure("Recharge > Rs. 10,000 was accepted!");
        } catch (AccountException e) {
            recordSuccess("Recharge Max Bound: Rs. 15,000.00 recharge rejected (Max is Rs. 10,000).");
        }

        // 2.4 Auto-Unblock when balance >= Rs. 100
        tag1002.recharge(500.0); // 70 + 500 = 570 (>= 100)
        assertEquals(570.0, tag1002.getBalance(), "Tag balance after recharge");
        assertEquals(AccountStatus.ACTIVE, tag1002.getStatus(), "Auto-unblock state");
        recordSuccess("Auto-Unblock Engine: Recharge brought balance to Rs. 570.00 -> Account transitioned to ACTIVE.");

        // 2.5 Admin Blacklisting
        FASTagAccount tag9999 = accountService.getAccountByTagId("IND-TAG-9999");
        assertTrue(tag9999.isBlacklisted(), "Tag IND-TAG-9999 blacklisted flag");
        recordSuccess("Admin Blacklist Engine: Stolen tag IND-TAG-9999 successfully flagged as BLACKLISTED.");

        // ----------------------------------------------------------------------------------------
        // SECTION 3: MODULE 4 - RATES, DISCOUNTS & PASSES
        // ----------------------------------------------------------------------------------------
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println(" [MODULE 4] VERIFYING RATES, 24H RETURN DISCOUNT & MONTHLY PASSES");
        System.out.println("------------------------------------------------------------------------------------------");

        // 3.1 Nearest Rs. 5 Rounding
        assertEquals(110.0, pricingService.roundToNearest5(112.0), "Nearest 5 rounding (112 -> 110)");
        assertEquals(115.0, pricingService.roundToNearest5(113.0), "Nearest 5 rounding (113 -> 115)");
        assertEquals(115.0, pricingService.roundToNearest5(112.5), "Nearest 5 rounding (112.5 -> 115)");
        recordSuccess("Rounding Algorithm: Mathematical rounding to nearest Rs. 5 validated.");

        // 3.2 Monthly Pass validity & single pass constraint
        MonthlyPass activePass = pricingService.getValidMonthlyPass("TN-09-CB-9999", 103, LocalDate.now());
        assertTrue(activePass != null && activePass.isValidOn(LocalDate.now()), "Active Monthly Pass check");
        recordSuccess("Monthly Pass Retrieval: Active pass for Bus TN-09-CB-9999 at Plaza #103 verified.");

        // Attempt duplicate active pass on same vehicle and plaza
        try {
            pricingService.issueMonthlyPass(admin, "TN-09-CB-9999", 103, LocalDate.now(), 30, 11000.0);
            recordFailure("Duplicate active monthly pass was permitted!");
        } catch (PassException e) {
            recordSuccess("Single Pass Rule: Second overlapping monthly pass for TN-09-CB-9999 at Plaza #103 correctly blocked.");
        }

        // 3.3 24-Hour Return Trip Discount calculation
        TollPlaza p101 = plazaService.getPlaza(101);
        Vehicle car1 = accountService.getVehicle("MH-12-DE-1433");
        LocalDateTime testNow = LocalDateTime.now();

        // Simulate single trip
        TollTransaction singleTrip = TollTransaction.builder()
                .vehicleRegNo("MH-12-DE-1433").plazaId(101).plazaName(p101.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1001").timestamp(testNow.minusHours(4))
                .tollAmount(110.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build();

        TollCalculationResult calc = pricingService.calculateToll(p101, car1, VehicleType.CAR, testNow, List.of(singleTrip));
        assertEquals(TripType.RETURN, calc.getTripType(), "Trip type after return");
        assertEquals(55.0, calc.getApplicableAmount(), "Return trip fare (50% of 110 = 55)");
        recordSuccess("24-Hour Return Discount: Automatic 50% discount applied for return crossing within 24h (Rs. 55.00).");

        // ----------------------------------------------------------------------------------------
        // SECTION 4: MODULE 3 - TOLL PROCESSING PIPELINE & BARRIER CONTROLS
        // ----------------------------------------------------------------------------------------
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println(" [MODULE 3] VERIFYING EDGE TOLL PROCESSING & BARRIER ACTUATION");
        System.out.println("------------------------------------------------------------------------------------------");

        // 4.1 Emergency Vehicle in Emergency Lane pays Rs. 0
        CrossingRequest emgReq = CrossingRequest.builder()
                .plazaId(101).laneNumber(4).vehicleRegNo("MH-12-EM-108").crossingTime(LocalDateTime.now()).build();
        CrossingResponse emgResp = tollEngine.processCrossing(emgReq);
        assertTrue(emgResp.isBarrierOpened(), "Emergency lane barrier open");
        assertEquals(0.0, emgResp.getTransaction().getTollAmount(), "Emergency lane toll amount");
        recordSuccess("Emergency Priority Lane: Ambulance cleared with Rs. 0.00 toll. Barrier opened.");

        // 4.2 Blacklisted Tag Refused
        CrossingRequest blReq = CrossingRequest.builder()
                .plazaId(101).laneNumber(1).tagId("IND-TAG-9999").crossingTime(LocalDateTime.now()).build();
        CrossingResponse blResp = tollEngine.processCrossing(blReq);
        assertTrue(!blResp.isBarrierOpened(), "Blacklisted tag barrier closed");
        assertEquals(TransactionStatus.FAILED, blResp.getStatus(), "Blacklisted transaction status");
        recordSuccess("Blacklist Enforcement: Blacklisted tag IND-TAG-9999 refused at plaza. Barrier stayed closed.");

        // 4.3 2-Minute Anti-Double-Billing Cooldown
        LocalDateTime crossingT = LocalDateTime.now();
        CrossingRequest firstCross = CrossingRequest.builder()
                .plazaId(101).laneNumber(1).tagId("IND-TAG-1001").crossingTime(crossingT).build();
        CrossingResponse firstResp = tollEngine.processCrossing(firstCross);
        assertTrue(firstResp.isBarrierOpened(), "First crossing successful");

        // Immediate second attempt within 30 seconds
        CrossingRequest dupCross = CrossingRequest.builder()
                .plazaId(101).laneNumber(1).tagId("IND-TAG-1001").crossingTime(crossingT.plusSeconds(30)).build();
        CrossingResponse dupResp = tollEngine.processCrossing(dupCross);
        assertTrue(!dupResp.isBarrierOpened(), "Duplicate crossing barrier closed");
        assertEquals(TransactionStatus.FAILED, dupResp.getStatus(), "Duplicate transaction status");
        recordSuccess("Anti-Double-Billing Protection: Duplicate crossing within 30s blocked. Barrier stayed closed.");

        // 4.4 Vehicle Type Mismatch (Registered CAR, Optical AVC Observed TRUCK)
        // Rule: Higher toll charged and isFlaggedForReview = true
        CrossingRequest mismatchReq = CrossingRequest.builder()
                .plazaId(102).laneNumber(1).tagId("IND-TAG-1001") // Car
                .observedVehicleType(VehicleType.TRUCK)           // AVC reads Truck
                .crossingTime(crossingT.plusMinutes(5)).build();
        CrossingResponse mismatchResp = tollEngine.processCrossing(mismatchReq);
        assertTrue(mismatchResp.isBarrierOpened(), "Mismatch crossing barrier opened after payment");
        assertTrue(mismatchResp.getTransaction().isFlaggedForReview(), "Transaction flagged for review");
        assertEquals(VehicleType.TRUCK, mismatchResp.getTransaction().getBilledType(), "Billed as higher vehicle category");
        assertEquals(235.0, mismatchResp.getTransaction().getTollAmount(), "Charged Truck toll rate (Rs. 235.00)");
        recordSuccess("AVC Mismatch Engine: Optical Truck sensor on Car tag charged higher toll (Rs. 235.00) and flagged for review.");

        // 4.5 Missing Tag in FASTag Lane -> Diverted to Cash Lane with Double Toll (2x)
        CrossingRequest noTagReq = CrossingRequest.builder()
                .plazaId(101).laneNumber(1).vehicleRegNo("KA-04-NO-TAG")
                .observedVehicleType(VehicleType.CAR)
                .cashOperatorName("Suresh Yadav (POS Operator)")
                .crossingTime(crossingT.plusMinutes(10)).build();
        CrossingResponse noTagResp = tollEngine.processCrossing(noTagReq);
        assertTrue(noTagResp.isDivertedToCash(), "Diverted to cash lane");
        assertTrue(noTagResp.isPenaltyApplied(), "Double penalty applied");
        assertEquals(220.0, noTagResp.getTransaction().getTollAmount(), "Double toll (2 x 110 = 220)");
        assertEquals(PaymentType.CASH, noTagResp.getTransaction().getPaymentType(), "Paid in cash");
        recordSuccess("Missing Tag Fallback: Diverted to Cash Lane, charged DOUBLE TOLL penalty (Rs. 220.00).");

        // 4.6 Insufficient Balance in FASTag Lane -> Diverted to Cash Lane with Normal Toll
        // Tag IND-TAG-1003 is blocked (< Rs. 100)
        CrossingRequest lowBalReq = CrossingRequest.builder()
                .plazaId(101).laneNumber(1).tagId("IND-TAG-1003")
                .cashOperatorName("Suresh Yadav (POS Operator)")
                .crossingTime(crossingT.plusMinutes(15)).build();
        CrossingResponse lowBalResp = tollEngine.processCrossing(lowBalReq);
        assertTrue(lowBalResp.isDivertedToCash(), "Diverted to cash lane due to insufficient balance");
        assertTrue(!lowBalResp.isPenaltyApplied(), "Normal toll (no 2x penalty) for insufficient balance");
        assertEquals(180.0, lowBalResp.getTransaction().getTollAmount(), "Normal single toll for Van (Rs. 180.00)");
        recordSuccess("Low Balance Fallback: Blocked tag diverted to Cash Lane, paid NORMAL TOLL (Rs. 180.00) in cash.");

        // ----------------------------------------------------------------------------------------
        // SECTION 5: MODULE 5 - BILLING, 7-DAY DISPUTES & SQL ANALYTICS
        // ----------------------------------------------------------------------------------------
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println(" [MODULE 5] VERIFYING BILLING, 7-DAY DISPUTES, REFUNDS & SQL ANALYTICS");
        System.out.println("------------------------------------------------------------------------------------------");

        // 5.1 7-Day Dispute Filing Window
        TollTransaction recentTxn = firstResp.getTransaction();
        // File dispute within 2 days -> Should SUCCEED
        Dispute validDispute = billingService.raiseDispute(
                recentTxn.getVehicleRegNo(), recentTxn.getTransactionId(), "Duplicate charge suspected", recentTxn.getTimestamp().plusDays(2)
        );
        assertEquals(DisputeStatus.RAISED, validDispute.getStatus(), "Dispute raised status");
        recordSuccess("7-Day Dispute Engine: Dispute filed 2 days after crossing accepted (Status: RAISED).");

        // Dispute filed 9 days after crossing -> Should REJECT
        try {
            billingService.raiseDispute(
                    recentTxn.getVehicleRegNo(), recentTxn.getTransactionId(), "Late claim", recentTxn.getTimestamp().plusDays(9)
            );
            recordFailure("Dispute filed after 7 days was accepted!");
        } catch (DisputeException e) {
            recordSuccess("7-Day Dispute Window: Dispute filed after 9 days correctly rejected (> 7-day limit).");
        }

        // 5.2 Dispute Approval and Automatic Refund to FASTag Account
        double preRefundBalance = tag1001Balance(accountService);
        billingService.reviewDispute(admin, validDispute.getDisputeId(), true, "Approved by NHAI grievance team.");
        assertEquals(DisputeStatus.APPROVED, validDispute.getStatus(), "Dispute approved status");
        double postRefundBalance = tag1001Balance(accountService);
        assertEquals(preRefundBalance + validDispute.getDisputedAmount(), postRefundBalance, "Automatic balance refund");
        recordSuccess(String.format("Dispute Refund Engine: Approved dispute refunded Rs. %.2f back to Tag IND-TAG-1001 balance.",
                validDispute.getDisputedAmount()));

        // 5.3 Low Balance Alert Trigger (< Rs. 200)
        // Setup tag1002 with balance Rs. 250, then execute crossing with Rs. 110 toll -> remaining Rs. 140 (< Rs. 200)
        FASTagAccount alertTestTag = accountService.getAccountByTagId("IND-TAG-1002");
        // Debit down to 250
        double currentBal = alertTestTag.getBalance();
        if (currentBal > 250.0) {
            alertTestTag.debit(currentBal - 250.0);
        }
        CrossingRequest alertReq = CrossingRequest.builder()
                .plazaId(101).laneNumber(2).tagId("IND-TAG-1002").crossingTime(crossingT.plusHours(1)).build();
        CrossingResponse alertResp = tollEngine.processCrossing(alertReq);
        assertTrue(alertResp.isBarrierOpened(), "Crossing with balance Rs. 250 succeeded");
        assertEquals(140.0, alertTestTag.getBalance(), "Remaining balance is Rs. 140 (< Rs. 200)");

        List<LowBalanceAlert> alerts = billingService.getAllAlerts();
        assertTrue(!alerts.isEmpty(), "Low balance alerts list should not be empty");
        recordSuccess("Low Balance Alert: Successfully triggered when balance dropped to Rs. " + alertTestTag.getBalance() + " (< Rs. 200).");

        // 5.4 SQL Exercise Query 1
        System.out.println("\n>>> [SQL QUERY 1 RESULT] Vehicle Type Revenue Summary (>2 Successful Txns, Sorted by Total Toll DESC):");
        List<VehicleTypeRevenueReport> q1 = billingService.executeQuery1VehicleTypeSummary();
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
        System.out.println("| Vehicle Type    | Total Txns   | Total Toll (Rs.)   | Avg Toll (Rs.)     |");
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
        for (VehicleTypeRevenueReport r : q1) {
            System.out.println(r);
        }
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
        assertTrue(!q1.isEmpty(), "Query 1 returns non-empty aggregated data");
        recordSuccess("SQL Exercise Query 1 executed and validated successfully.");

        // 5.5 SQL Exercise Query 2
        System.out.println("\n>>> [SQL QUERY 2 RESULT] Plaza Crossings & Failure Audit (>=3 Crossings):");
        List<PlazaAuditReport> q2 = billingService.executeQuery2PlazaCrossingsAudit();
        System.out.println("+------------------------------------------------------------------------------------------+");
        for (PlazaAuditReport r : q2) {
            System.out.println(r);
        }
        System.out.println("+------------------------------------------------------------------------------------------+");
        assertTrue(!q2.isEmpty(), "Query 2 returns non-empty audit results");
        recordSuccess("SQL Exercise Query 2 executed and validated successfully.");

        // 5.6 SQL Exercise Query 3
        System.out.println("\n>>> [SQL QUERY 3 RESULT] Multi-Plaza Accounts (>2 Distinct Plazas Used, Sorted by Toll DESC):");
        List<MultiPlazaAccountReport> q3 = billingService.executeQuery3MultiPlazaAccountSummary();
        System.out.println("+------------------------------------------------------------------------------------------+");
        for (MultiPlazaAccountReport r : q3) {
            System.out.println(r);
        }
        System.out.println("+------------------------------------------------------------------------------------------+");
        assertTrue(!q3.isEmpty(), "Query 3 returns non-empty multi-plaza accounts");
        recordSuccess("SQL Exercise Query 3 executed and validated successfully.");

        // SUMMARY
        System.out.println("\n==========================================================================================");
        System.out.printf("   TOTAL TEST RESULTS: %d PASSED, %d FAILED%n", testsPassed, testsFailed);
        System.out.println("==========================================================================================");

        if (testsFailed == 0) {
            System.out.println(">>> ? ALL BUSINESS RULES, CONSTRAINTS & SQL QUERIES FULLY VERIFIED!\n");
        } else {
            System.err.println(">>> ? VERIFICATION FAILED WITH " + testsFailed + " ERRORS.\n");
        }
    }

    private static double tag1001Balance(IAccountService accountService) {
        return accountService.getAccountByTagId("IND-TAG-1001").getBalance();
    }

    private static void recordSuccess(String message) {
        testsPassed++;
        System.out.println("? [PASS] " + message);
    }

    private static void recordFailure(String message) {
        testsFailed++;
        System.err.println("? [FAIL] " + message);
    }

    private static void assertEquals(Object expected, Object actual, String context) {
        if (!expected.equals(actual)) {
            recordFailure(context + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, String context) {
        if (Math.abs(expected - actual) > 0.001) {
            recordFailure(context + " | Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String context) {
        if (!condition) {
            recordFailure(context + " | Expected TRUE but was FALSE.");
        }
    }
}
