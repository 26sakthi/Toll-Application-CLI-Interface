package com.toll.app;

import com.toll.context.TollSystemBootstrap;
import com.toll.context.TollSystemContext;
import com.toll.exception.AccountException;
import com.toll.exception.DisputeException;
import com.toll.exception.PassException;
import com.toll.exception.TollManagementException;
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
import com.toll.service.tollengine.CrossingRequest;
import com.toll.service.tollengine.CrossingResponse;
import com.toll.service.tollengine.ITollProcessingService;
import com.toll.test.SystemVerificationSuite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main Interactive Console Application for Toll Management System (FASTag).
 * Provides an Indian Highway Authority themed console interface with interactive
 * operations, real-time crossing simulator, and analytical SQL dashboards.
 */
public class TollManagementApp {

    // ANSI Color Palettes for Terminal
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String ORANGE = "\u001B[38;5;208m"; // NHAI Orange
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";

    private final TollSystemContext context;
    private final Scanner scanner;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TollManagementApp(TollSystemContext context) {
        this.context = context;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        TollSystemContext context = TollSystemBootstrap.initializeSystem();
        TollManagementApp app = new TollManagementApp(context);
        app.start();
    }

    public void start() {
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Enter menu choice (1-8): ");
            System.out.println();

            switch (choice.trim()) {
                case "1" -> handlePlazaLaneMenu();
                case "2" -> handleVehicleAccountMenu();
                case "3" -> handleCrossingSimulatorMenu();
                case "4" -> handleMonthlyPassMenu();
                case "5" -> handleDisputeBillingMenu();
                case "6" -> handleSqlAnalyticsMenu();
                case "7" -> handleRunAutomatedTestSuite();
                case "8" -> {
                    System.out.println(GREEN + "Thank you for using the National Highway FASTag Toll Management System. Safe Journey!" + RESET);
                    running = false;
                }
                default -> System.out.println(RED + "Invalid option. Please choose between 1 and 8." + RESET);
            }
            if (running) {
                prompt("\nPress ENTER to return to Main Menu...");
            }
        }
    }

    private void printBanner() {
        System.out.println(ORANGE + BOLD + "==========================================================================================" + RESET);
        System.out.println(ORANGE + BOLD + "    NATIONAL HIGHWAY AUTHORITY OF INDIA (NHAI) - ELECTRONIC TOLL COLLECTION (FASTag)   " + RESET);
        System.out.println(ORANGE + BOLD + "            Ministry of Road Transport & Highways (MoRTH) Standard System                " + RESET);
        System.out.println(ORANGE + BOLD + "==========================================================================================" + RESET);
        System.out.println(CYAN + " Logged in Authority: " + context.getDefaultAdmin().getName() + " (" + context.getDefaultAdmin().getAdminId() + ")" + RESET);
        System.out.println(CYAN + " System Status: ACTIVE | In-Memory Thread-Safe State Engine Initialized" + RESET);
        System.out.println(ORANGE + "==========================================================================================\n" + RESET);
    }

    private void printMainMenu() {
        System.out.println(BOLD + "\n==================== MAIN CONTROL CENTER ====================" + RESET);
        System.out.println("  1. " + CYAN + "Toll Plaza & Lane Administration" + RESET + " (Setup, Lanes, Rates, Locks)");
        System.out.println("  2. " + CYAN + "Vehicle & FASTag Registry" + RESET + " (Register, Issue Tag, Recharge, Blacklist)");
        System.out.println("  3. " + YELLOW + BOLD + "Toll Plaza Crossing Simulator" + RESET + " (FASTag, Cash Fallback, AVC Mismatch, Emergency)");
        System.out.println("  4. " + PURPLE + "Monthly Passes & Pricing" + RESET + " (Pass Issuance, 24h Return Discounts)");
        System.out.println("  5. " + BLUE + "Disputes, Billing & Reports" + RESET + " (7-Day Claims, Refunds, Low Bal Alerts)");
        System.out.println("  6. " + GREEN + BOLD + "SQL Analytical Queries Dashboard" + RESET + " (Queries 1, 2, and 3 Aggregations)");
        System.out.println("  7. " + ORANGE + BOLD + "Automated System Verification Test Suite" + RESET + " (Run 27+ Unit & Integration Tests)");
        System.out.println("  8. " + RED + "Exit Application" + RESET);
        System.out.println("=============================================================");
    }

    // ============================================================================================
    // MENU 1: PLAZA & LANE MANAGEMENT
    // ============================================================================================
    private void handlePlazaLaneMenu() {
        System.out.println(BOLD + "\n--- [MODULE 1] TOLL PLAZA & LANE ADMINISTRATION ---" + RESET);
        System.out.println("  1. List All Toll Plazas & Lanes");
        System.out.println("  2. Create New Toll Plaza (Admin)");
        System.out.println("  3. Add Lane to Plaza (Admin)");
        System.out.println("  4. Configure Toll Rate with Effective Date (Admin)");
        System.out.println("  5. Open Plaza for Traffic (Validates >=2 Lanes & Rates)");
        System.out.println("  6. Close Lane (Validates Vehicle Processing Lock)");
        System.out.println("  7. Open Lane");

        String choice = prompt("Select option (1-7): ");
        IPlazaService svc = context.getPlazaService();
        Admin admin = context.getDefaultAdmin();

        try {
            switch (choice.trim()) {
                case "1" -> {
                    System.out.println("\n--- CONFIGURED TOLL PLAZAS ---");
                    for (TollPlaza p : svc.getAllPlazas()) {
                        System.out.printf("[%d] %s (%s, %s) | Status: %s%n",
                                p.getPlazaId(), p.getName(), p.getHighwayName(), p.getLocation(),
                                p.isOpen() ? (GREEN + "ACTIVE" + RESET) : (RED + "INACTIVE" + RESET));
                        System.out.println("    Lanes: (" + p.getLanes().size() + " total)");
                        for (Lane l : p.getLanes()) {
                            System.out.printf("      - Lane #%d: %s | Status: %s | ActiveProcessing: %s%n",
                                    l.getLaneNumber(), l.getLaneType(), l.getStatus(), l.isProcessingVehicle());
                        }
                    }
                }
                case "2" -> {
                    String name = prompt("Enter Plaza Name (e.g. Khedshivapur Plaza): ");
                    String highway = prompt("Enter Highway Name (e.g. NH-48): ");
                    String location = prompt("Enter Location (e.g. Pune): ");
                    TollPlaza plaza = svc.createPlaza(admin, name, highway, location);
                    System.out.println(GREEN + "? Created Toll Plaza #" + plaza.getPlazaId() + ": " + plaza.getName() + RESET);
                }
                case "3" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID: "));
                    int laneNum = Integer.parseInt(prompt("Enter Lane Number: "));
                    System.out.println("Lane Types: 1. FASTAG | 2. CASH | 3. EMERGENCY");
                    String tChoice = prompt("Select Type: ");
                    Lane lane = switch (tChoice.trim()) {
                        case "2" -> new CashLane(laneNum, "Duty Operator");
                        case "3" -> new EmergencyLane(laneNum);
                        default -> new FASTagLane(laneNum);
                    };
                    svc.addLane(admin, plazaId, lane);
                    System.out.println(GREEN + "? Added Lane #" + laneNum + " (" + lane.getLaneType() + ") to Plaza #" + plazaId + RESET);
                }
                case "4" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID: "));
                    System.out.println("Vehicle Types: CAR, VAN, BUS, TRUCK, MULTI_AXLE");
                    VehicleType vt = VehicleType.valueOf(prompt("Enter Vehicle Type: ").trim().toUpperCase());
                    System.out.println("Trip Types: SINGLE, RETURN");
                    TripType tt = TripType.valueOf(prompt("Enter Trip Type: ").trim().toUpperCase());
                    double amount = Double.parseDouble(prompt("Enter Toll Rate (Rs.): "));
                    TollRate rate = svc.setTollRate(admin, plazaId, vt, tt, amount, LocalDate.now());
                    System.out.println(GREEN + "? Set Rate: " + vt + " " + tt + " = Rs. " + rate.getAmount() + " (Effective: " + rate.getEffectiveDate() + ")" + RESET);
                }
                case "5" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID to Open: "));
                    svc.openPlaza(admin, plazaId);
                    System.out.println(GREEN + "? Plaza #" + plazaId + " opened successfully!" + RESET);
                }
                case "6" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID: "));
                    int laneNum = Integer.parseInt(prompt("Enter Lane Number to Close: "));
                    svc.closeLane(admin, plazaId, laneNum);
                    System.out.println(GREEN + "? Lane #" + laneNum + " at Plaza #" + plazaId + " closed successfully." + RESET);
                }
                case "7" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID: "));
                    int laneNum = Integer.parseInt(prompt("Enter Lane Number to Open: "));
                    svc.openLane(admin, plazaId, laneNum);
                    System.out.println(GREEN + "? Lane #" + laneNum + " at Plaza #" + plazaId + " opened successfully." + RESET);
                }
                default -> System.out.println(RED + "Invalid choice." + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "? Error: " + e.getMessage() + RESET);
        }
    }

    // ============================================================================================
    // MENU 2: VEHICLE & FASTAG REGISTRY
    // ============================================================================================
    private void handleVehicleAccountMenu() {
        System.out.println(BOLD + "\n--- [MODULE 2] VEHICLE & FASTAG ACCOUNT REGISTRY ---" + RESET);
        System.out.println("  1. List All Registered Vehicles & Accounts");
        System.out.println("  2. Register New Vehicle");
        System.out.println("  3. Issue New FASTag (Strict 1:1 Tag Association)");
        System.out.println("  4. Recharge FASTag Wallet (UPI, Card, NetBanking)");
        System.out.println("  5. Blacklist FASTag (Stolen / Fraud Hold - Admin)");
        System.out.println("  6. Un-blacklist FASTag (Admin)");

        String choice = prompt("Select option (1-6): ");
        IAccountService svc = context.getAccountService();
        Admin admin = context.getDefaultAdmin();

        try {
            switch (choice.trim()) {
                case "1" -> {
                    System.out.println("\n----------------------------------------------------------------------------------");
                    System.out.printf("| %-15s | %-12s | %-20s | %-12s | %-10s | %-8s |%n",
                            "Registration", "Type", "Owner", "Tag ID", "Balance", "Status");
                    System.out.println("----------------------------------------------------------------------------------");
                    for (Vehicle v : svc.getAllVehicles()) {
                        FASTagAccount acc = svc.getAccountByVehicleReg(v.getRegistrationNumber());
                        String tagId = acc != null ? acc.getTagId() : "NONE";
                        String bal = acc != null ? String.format("Rs. %.2f", acc.getBalance()) : "N/A";
                        String st = acc != null ? (acc.isBlacklisted() ? (RED + "BLACKLIST" + RESET) : (acc.getStatus() == AccountStatus.ACTIVE ? (GREEN + "ACTIVE" + RESET) : (YELLOW + "BLOCKED" + RESET))) : "N/A";
                        System.out.printf("| %-15s | %-12s | %-20s | %-12s | %-10s | %-8s |%n",
                                v.getRegistrationNumber(), v.getVehicleType(), v.getOwnerName(), tagId, bal, st);
                    }
                    System.out.println("----------------------------------------------------------------------------------");
                }
                case "2" -> {
                    String reg = prompt("Enter Indian Registration No (e.g. MH-12-AB-1234): ").trim().toUpperCase();
                    System.out.println("Categories: 1. CAR | 2. VAN | 3. BUS | 4. TRUCK | 5. MULTI_AXLE | 6. EMERGENCY");
                    String cat = prompt("Choose Category (1-6): ");
                    String owner = prompt("Enter Owner Name: ");
                    String phone = prompt("Enter Contact Phone: ");

                    Vehicle v = switch (cat.trim()) {
                        case "2" -> new Van(reg, owner, phone);
                        case "3" -> new Bus(reg, owner, phone);
                        case "4" -> new Truck(reg, owner, phone);
                        case "5" -> new MultiAxleVehicle(reg, owner, phone, 4);
                        case "6" -> new EmergencyVehicle(reg, owner, phone, "Ambulance");
                        default -> new Car(reg, owner, phone);
                    };
                    svc.registerVehicle(v);
                    System.out.println(GREEN + "? Vehicle registered successfully: " + v + RESET);
                }
                case "3" -> {
                    String tagId = prompt("Enter New Tag ID (e.g. IND-TAG-2001): ").trim().toUpperCase();
                    String reg = prompt("Enter Registered Vehicle No: ").trim().toUpperCase();
                    double initialBal = Double.parseDouble(prompt("Enter Initial Balance (Min Rs. 100, Max Rs. 50,000): "));

                    FASTagAccount acc = svc.issueFASTag(tagId, reg, initialBal);
                    System.out.println(GREEN + "? FASTag Issued! Linked 1:1 to " + reg + " | Balance: Rs. " + acc.getBalance() + " | Status: " + acc.getStatus() + RESET);
                }
                case "4" -> {
                    String tagId = prompt("Enter Tag ID to Recharge: ").trim().toUpperCase();
                    double amount = Double.parseDouble(prompt("Enter Recharge Amount (Rs. 100 to Rs. 10,000): "));
                    System.out.println("Recharge Instrument: 1. UPI | 2. CARD | 3. NET_BANKING");
                    String mChoice = prompt("Select Method (1-3): ");
                    RechargeMethod method = switch (mChoice.trim()) {
                        case "2" -> RechargeMethod.CARD;
                        case "3" -> RechargeMethod.NET_BANKING;
                        default -> RechargeMethod.UPI;
                    };

                    FASTagAccount acc = svc.rechargeAccount(tagId, amount, method);
                    context.getBillingService().recordRecharge(tagId, amount, method, RechargeStatus.PAID);
                    System.out.println(GREEN + "? Recharge Successful via " + method + "!" + RESET);
                    System.out.printf("   New Wallet Balance: Rs. %.2f | Account Status: %s%n", acc.getBalance(), acc.getStatus());
                }
                case "5" -> {
                    String tagId = prompt("Enter Tag ID to Blacklist: ").trim().toUpperCase();
                    String reason = prompt("Enter Blacklist Reason (e.g. Stolen Vehicle): ");
                    svc.blacklistTag(admin, tagId, reason);
                    System.out.println(RED + "? Tag " + tagId + " BLACKLISTED and refused at all toll plazas." + RESET);
                }
                case "6" -> {
                    String tagId = prompt("Enter Tag ID to Un-blacklist: ").trim().toUpperCase();
                    svc.unblacklistTag(admin, tagId);
                    System.out.println(GREEN + "? Tag " + tagId + " un-blacklisted and operational." + RESET);
                }
                default -> System.out.println(RED + "Invalid choice." + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "? Error: " + e.getMessage() + RESET);
        }
    }

    // ============================================================================================
    // MENU 3: TOLL PLAZA CROSSING SIMULATOR
    // ============================================================================================
    private void handleCrossingSimulatorMenu() {
        System.out.println(BOLD + "\n--- [MODULE 3 & 4] REAL-TIME TOLL PLAZA CROSSING SIMULATOR ---" + RESET);
        System.out.println("Available Toll Plazas:");
        for (TollPlaza p : context.getPlazaService().getAllPlazas()) {
            System.out.printf("  [%d] %s (%s)%n", p.getPlazaId(), p.getName(), p.getHighwayName());
        }

        try {
            int plazaId = Integer.parseInt(prompt("Select Plaza ID: "));
            TollPlaza plaza = context.getPlazaService().getPlaza(plazaId);

            System.out.println("Available Lanes at " + plaza.getName() + ":");
            for (Lane l : plaza.getLanes()) {
                System.out.printf("  Lane #%d: %s (Status: %s)%n", l.getLaneNumber(), l.getLaneType(), l.getStatus());
            }

            int laneNum = Integer.parseInt(prompt("Select Lane Number: "));
            Lane lane = plaza.getLane(laneNum);

            System.out.println("\nCrossing Simulation Scenarios:");
            System.out.println("  1. Normal Registered Vehicle with Active FASTag (e.g. MH-12-DE-1433 / IND-TAG-1001)");
            System.out.println("  2. Vehicle with Low Balance / Blocked Tag (< Rs. 100) -> Test Cash Fallback (e.g. IND-TAG-1003)");
            System.out.println("  3. Vehicle with NO Tag in FASTag Lane -> Test 2x Double Toll Cash Penalty (e.g. KA-04-NO-TAG)");
            System.out.println("  4. Vehicle with Optical AVC Mismatch -> AVC TRUCK sensor on CAR Tag -> Test Higher Toll + Flag");
            System.out.println("  5. Stolen / Blacklisted Tag -> Test Security Alarm & Barrier Rejection (e.g. IND-TAG-9999)");
            System.out.println("  6. Emergency Vehicle (Ambulance) in Emergency Lane -> Test Rs. 0 Toll Bypass");
            System.out.println("  7. Immediate Re-Crossing within 2 Minutes -> Test Anti-Double-Billing Cooldown");
            System.out.println("  8. Custom Vehicle Crossing (Enter Custom Reg / Tag / Observed Type)");

            String scenario = prompt("Select Scenario (1-8): ");
            CrossingRequest.Builder reqBuilder = CrossingRequest.builder().plazaId(plazaId).laneNumber(laneNum);

            switch (scenario.trim()) {
                case "1" -> reqBuilder.tagId("IND-TAG-1001").vehicleRegNo("MH-12-DE-1433").observedVehicleType(VehicleType.CAR);
                case "2" -> reqBuilder.tagId("IND-TAG-1003").vehicleRegNo("KA-03-MG-5678").cashOperatorName("Suresh Yadav (POS Operator)");
                case "3" -> reqBuilder.vehicleRegNo("KA-04-NO-TAG").observedVehicleType(VehicleType.CAR).cashOperatorName("Suresh Yadav (POS Operator)");
                case "4" -> reqBuilder.tagId("IND-TAG-1001").vehicleRegNo("MH-12-DE-1433").observedVehicleType(VehicleType.TRUCK);
                case "5" -> reqBuilder.tagId("IND-TAG-9999").vehicleRegNo("DL-03-XX-0000");
                case "6" -> reqBuilder.vehicleRegNo("MH-12-EM-108");
                case "7" -> {
                    System.out.println(CYAN + "Simulating initial crossing..." + RESET);
                    tollEngineCrossing(reqBuilder.tagId("IND-TAG-1001").vehicleRegNo("MH-12-DE-1433").build());
                    System.out.println(YELLOW + "Simulating duplicate attempt 15 seconds later..." + RESET);
                    reqBuilder.crossingTime(LocalDateTime.now().plusSeconds(15));
                }
                case "8" -> {
                    String reg = prompt("Enter Vehicle Registration: ");
                    String tag = prompt("Enter Tag ID (or leave blank for pure cash): ");
                    System.out.println("Optical AVC Sensor Types: CAR, VAN, BUS, TRUCK, MULTI_AXLE");
                    String avcStr = prompt("Enter Observed AVC Type (or blank for same as registered): ");
                    VehicleType avc = (!avcStr.trim().isEmpty()) ? VehicleType.valueOf(avcStr.trim().toUpperCase()) : null;

                    reqBuilder.vehicleRegNo(reg).tagId(tag.trim().isEmpty() ? null : tag).observedVehicleType(avc);
                }
                default -> throw new IllegalArgumentException("Unknown scenario.");
            }

            CrossingRequest req = reqBuilder.build();
            tollEngineCrossing(req);

        } catch (Exception e) {
            System.err.println(RED + "? Simulation Error: " + e.getMessage() + RESET);
        }
    }

    private void tollEngineCrossing(CrossingRequest request) {
        System.out.println("\n" + CYAN + "[LANE SENSOR ACTIVE] Vehicle detected approaching boom barrier..." + RESET);
        CrossingResponse resp = context.getTollProcessingService().processCrossing(request);

        System.out.println("----------------------------------------------------------------------------------");
        if (resp.isBarrierOpened()) {
            System.out.println(GREEN + BOLD + ">>> [BOOM BARRIER OPENED - GREEN SIGNAL] <<<" + RESET);
        } else {
            System.out.println(RED + BOLD + ">>> [BOOM BARRIER CLOSED - RED SIGNAL - REJECTED] <<<" + RESET);
        }
        System.out.println("----------------------------------------------------------------------------------");
        System.out.println("Status: " + resp.getStatus());
        System.out.println("Message: " + resp.getMessage());

        TollTransaction txn = resp.getTransaction();
        if (txn != null) {
            System.out.printf("Transaction ID : %s%n", txn.getTransactionId());
            System.out.printf("Vehicle Reg No : %s%n", txn.getVehicleRegNo());
            System.out.printf("Plaza & Lane   : %s (Lane #%d)%n", txn.getPlazaName(), txn.getLaneNumber());
            System.out.printf("Toll Deducted  : Rs. %.2f (Payment: %s)%n", txn.getTollAmount(), txn.getPaymentType());
            System.out.printf("Trip Category  : %s%n", txn.getTripType());
            System.out.printf("Registered Type: %s | Observed: %s | Billed: %s%n",
                    txn.getRegisteredType(), txn.getObservedType(), txn.getBilledType());
            if (txn.isFlaggedForReview()) {
                System.out.println(YELLOW + BOLD + "FLAGGED FOR REVIEW: YES (Audit Flagged by Automated Classifier)" + RESET);
            }
            if (txn.getOperatorName() != null) {
                System.out.println("Lane Operator  : " + txn.getOperatorName());
            }
            if (txn.getFailureReason() != null) {
                System.out.println("Reason / Audit : " + txn.getFailureReason());
            }
        }
    }

    // ============================================================================================
    // MENU 4: MONTHLY PASSES & PRICING
    // ============================================================================================
    private void handleMonthlyPassMenu() {
        System.out.println(BOLD + "\n--- [MODULE 4] MONTHLY PASSES & PRICING ---" + RESET);
        System.out.println("  1. View All Issued Monthly Passes");
        System.out.println("  2. Issue New Monthly Pass (Admin)");
        System.out.println("  3. View Configured Pass Fees per Vehicle Type");

        String choice = prompt("Select option (1-3): ");
        IPricingService svc = context.getPricingService();
        Admin admin = context.getDefaultAdmin();

        try {
            switch (choice.trim()) {
                case "1" -> {
                    System.out.println("\n--- ISSUED MONTHLY PASSES ---");
                    for (MonthlyPass p : svc.getAllPasses()) {
                        System.out.println(p);
                    }
                }
                case "2" -> {
                    String reg = prompt("Enter Vehicle Registration: ").trim().toUpperCase();
                    int plazaId = Integer.parseInt(prompt("Enter Toll Plaza ID: "));
                    double fee = Double.parseDouble(prompt("Enter Monthly Pass Fee (Rs.): "));
                    MonthlyPass p = svc.issueMonthlyPass(admin, reg, plazaId, LocalDate.now(), 30, fee);
                    System.out.println(GREEN + "? Monthly Pass Issued: " + p.getPassId() + " valid for 30 days at Plaza #" + plazaId + RESET);
                }
                case "3" -> {
                    System.out.println("\n--- MONTHLY PASS FEES TABLE ---");
                    for (VehicleType vt : VehicleType.values()) {
                        System.out.printf("  - %-15s: Rs. %.2f / month%n", vt.name(), svc.getPassFee(101, vt));
                    }
                }
                default -> System.out.println(RED + "Invalid choice." + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "? Error: " + e.getMessage() + RESET);
        }
    }

    // ============================================================================================
    // MENU 5: DISPUTES, BILLING & REPORTS
    // ============================================================================================
    private void handleDisputeBillingMenu() {
        System.out.println(BOLD + "\n--- [MODULE 5] DISPUTES, BILLING & AUDIT REPORTS ---" + RESET);
        System.out.println("  1. View Recent Crossing Transactions (Audit Log)");
        System.out.println("  2. Raise Dispute on Wrong Deduction (Strict 7-Day Window)");
        System.out.println("  3. Review & Resolve Customer Dispute (Admin - Auto Balance Refund)");
        System.out.println("  4. View All Raised Disputes");
        System.out.println("  5. View Low Balance Alerts (< Rs. 200)");
        System.out.println("  6. View Plaza Daily Revenue & Lane Vehicle Counts");

        String choice = prompt("Select option (1-6): ");
        IBillingReportingService svc = context.getBillingService();
        Admin admin = context.getDefaultAdmin();

        try {
            switch (choice.trim()) {
                case "1" -> {
                    System.out.println("\n--- RECENT TRANSACTIONS (IMMUTABLE LOG) ---");
                    List<TollTransaction> txns = svc.getAllTransactions();
                    System.out.printf("| %-10s | %-12s | %-20s | %-10s | %-8s | %-8s | %-8s |%n",
                            "Txn ID", "Vehicle", "Plaza", "Amount", "Trip", "Payment", "Status");
                    System.out.println("------------------------------------------------------------------------------------------");
                    for (TollTransaction t : txns) {
                        System.out.printf("| %-10s | %-12s | %-20s | Rs. %-6.2f | %-8s | %-8s | %-8s |%n",
                                t.getTransactionId().substring(0, Math.min(8, t.getTransactionId().length())),
                                t.getVehicleRegNo(), t.getPlazaName(), t.getTollAmount(), t.getTripType(), t.getPaymentType(), t.getStatus());
                    }
                }
                case "2" -> {
                    String reg = prompt("Enter Vehicle Registration: ");
                    String txnId = prompt("Enter Transaction ID to Dispute: ");
                    String reason = prompt("Enter Reason for Dispute: ");
                    Dispute d = svc.raiseDispute(reg, txnId, reason, LocalDateTime.now());
                    System.out.println(GREEN + "? Dispute filed successfully! Dispute ID: " + d.getDisputeId() + " | Disputed Amount: Rs. " + d.getDisputedAmount() + RESET);
                }
                case "3" -> {
                    String dispId = prompt("Enter Dispute ID to Review: ");
                    System.out.println("Action: 1. APPROVE & REFUND TO TAG BALANCE | 2. REJECT");
                    String dAction = prompt("Choose Action (1-2): ");
                    boolean approve = "1".equals(dAction.trim());
                    String remarks = prompt("Enter Admin Remarks: ");

                    svc.reviewDispute(admin, dispId, approve, remarks);
                    if (approve) {
                        System.out.println(GREEN + "? Dispute APPROVED! Refund automatically credited to customer's FASTag account." + RESET);
                    } else {
                        System.out.println(YELLOW + "? Dispute REJECTED. Remarks saved." + RESET);
                    }
                }
                case "4" -> {
                    System.out.println("\n--- CUSTOMER DISPUTES REGISTRY ---");
                    for (Dispute d : svc.getAllDisputes()) {
                        System.out.println(d + " | Remarks: " + d.getAdminRemarks());
                    }
                }
                case "5" -> {
                    System.out.println("\n--- LOW BALANCE ALERTS (< Rs. 200) ---");
                    for (LowBalanceAlert a : svc.getAllAlerts()) {
                        System.out.println(a);
                    }
                }
                case "6" -> {
                    int plazaId = Integer.parseInt(prompt("Enter Plaza ID: "));
                    double rev = svc.getDailyRevenue(plazaId, LocalDate.now());
                    Map<Integer, Long> counts = svc.getVehicleCountPerLane(plazaId);
                    System.out.printf("%nPlaza #%d Today's Total Revenue: Rs. %.2f%n", plazaId, rev);
                    System.out.println("Vehicle Traffic per Lane:");
                    for (Map.Entry<Integer, Long> e : counts.entrySet()) {
                        System.out.printf("  - Lane #%d: %d vehicle(s)%n", e.getKey(), e.getValue());
                    }
                }
                default -> System.out.println(RED + "Invalid choice." + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "? Error: " + e.getMessage() + RESET);
        }
    }

    // ============================================================================================
    // MENU 6: SQL ANALYTICAL QUERIES DASHBOARD
    // ============================================================================================
    private void handleSqlAnalyticsMenu() {
        System.out.println(BOLD + "\n--- [SQL EXERCISE DASHBOARD] IN-MEMORY AGGREGATION ENGINE ---" + RESET);
        System.out.println("  1. Execute Query 1: Vehicle Type Revenue Summary (> 2 Successful Txns, Sorted by Total Toll DESC)");
        System.out.println("  2. Execute Query 2: Plaza Crossing & Failure Audit (>= 3 Crossings)");
        System.out.println("  3. Execute Query 3: Multi-Plaza Accounts (> 2 Distinct Plazas Used, Sorted by Toll DESC)");
        System.out.println("  4. Execute ALL 3 SQL Queries Concurrently");

        String choice = prompt("Select option (1-4): ");
        IBillingReportingService svc = context.getBillingService();

        switch (choice.trim()) {
            case "1" -> runQuery1(svc);
            case "2" -> runQuery2(svc);
            case "3" -> runQuery3(svc);
            case "4" -> {
                runQuery1(svc);
                runQuery2(svc);
                runQuery3(svc);
            }
            default -> System.out.println(RED + "Invalid choice." + RESET);
        }
    }

    private void runQuery1(IBillingReportingService svc) {
        System.out.println(CYAN + BOLD + "\n>>> [SQL QUERY 1] Vehicle Type Revenue Summary (>2 Successful Txns, Sorted by Total Toll DESC):" + RESET);
        System.out.println(CYAN + "SQL: SELECT billed_type, COUNT(*), SUM(toll_amount), AVG(toll_amount) FROM toll_transactions WHERE status='SUCCESS' GROUP BY billed_type HAVING COUNT(*) > 2 ORDER BY SUM(toll_amount) DESC;" + RESET);
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
        System.out.println("| Vehicle Type    | Total Txns   | Total Toll (Rs.)   | Avg Toll (Rs.)     |");
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
        List<VehicleTypeRevenueReport> reports = svc.executeQuery1VehicleTypeSummary();
        for (VehicleTypeRevenueReport r : reports) {
            System.out.println(r);
        }
        System.out.println("+-----------------+--------------+--------------------+--------------------+");
    }

    private void runQuery2(IBillingReportingService svc) {
        System.out.println(CYAN + BOLD + "\n>>> [SQL QUERY 2] Plaza Crossing & Failure Audit (>=3 Crossings):" + RESET);
        System.out.println(CYAN + "SQL: SELECT plaza_id, plaza_name, COUNT(*), SUM(SUCCESS toll), COUNT(FAILED) FROM toll_transactions GROUP BY plaza_id, plaza_name HAVING COUNT(*) >= 3;" + RESET);
        System.out.println("+------------------------------------------------------------------------------------------+");
        List<PlazaAuditReport> reports = svc.executeQuery2PlazaCrossingsAudit();
        for (PlazaAuditReport r : reports) {
            System.out.println(r);
        }
        System.out.println("+------------------------------------------------------------------------------------------+");
    }

    private void runQuery3(IBillingReportingService svc) {
        System.out.println(CYAN + BOLD + "\n>>> [SQL QUERY 3] Multi-Plaza Accounts (>2 Distinct Plazas Used, Sorted by Toll DESC):" + RESET);
        System.out.println(CYAN + "SQL: SELECT tag_id, vehicle_reg_no, COUNT(*), SUM(toll_amount), COUNT(DISTINCT plaza_id) FROM toll_transactions WHERE status='SUCCESS' GROUP BY tag_id, vehicle_reg_no HAVING COUNT(DISTINCT plaza_id) > 2 ORDER BY SUM(toll_amount) DESC;" + RESET);
        System.out.println("+------------------------------------------------------------------------------------------+");
        List<MultiPlazaAccountReport> reports = svc.executeQuery3MultiPlazaAccountSummary();
        for (MultiPlazaAccountReport r : reports) {
            System.out.println(r);
        }
        System.out.println("+------------------------------------------------------------------------------------------+");
    }

    // ============================================================================================
    // MENU 7: AUTOMATED SYSTEM VERIFICATION TEST SUITE
    // ============================================================================================
    private void handleRunAutomatedTestSuite() {
        System.out.println(BOLD + "\n>>> LAUNCHING AUTOMATED SYSTEM VERIFICATION SUITE..." + RESET);
        SystemVerificationSuite.runAllVerifications();
    }

    private String prompt(String message) {
        System.out.print(BOLD + message + RESET);
        return scanner.nextLine();
    }
}
