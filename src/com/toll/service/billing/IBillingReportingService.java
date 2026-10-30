package com.toll.service.billing;

import com.toll.model.admin.Admin;
import com.toll.model.billing.*;
import com.toll.model.dispute.Dispute;
import com.toll.model.enums.RechargeMethod;
import com.toll.model.enums.RechargeStatus;
import com.toll.model.transaction.TollTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service contract for Payments, Disputes and Reporting Analytics (Module 5).
 * Demonstrates Abstraction and Centralized Audit Logging.
 */
public interface IBillingReportingService {

    /**
     * Records a wallet recharge transaction.
     */
    RechargeRecord recordRecharge(String tagId, double amount, RechargeMethod method, RechargeStatus status);

    /**
     * Raises a customer dispute on a toll deduction.
     * Business Rule: Must be filed within 7 days of crossing.
     */
    Dispute raiseDispute(String vehicleRegNo, String transactionId, String reason, LocalDateTime disputeTime);

    /**
     * Admin reviews and approves/rejects a dispute.
     * Business Rule: If approved, the disputed amount is automatically refunded to the tag balance.
     */
    void reviewDispute(Admin admin, String disputeId, boolean approve, String remarks);

    /**
     * Evaluates and logs a low balance alert if remaining balance is below Rs. 200.
     */
    LowBalanceAlert checkAndTriggerLowBalanceAlert(String tagId, String vehicleRegNo, double balanceAfterDeduction);

    /**
     * Logs an immutable crossing transaction.
     */
    void recordTransaction(TollTransaction transaction);

    /**
     * Retrieves all recorded transactions.
     */
    List<TollTransaction> getAllTransactions();

    /**
     * Retrieves a single transaction by its ID.
     */
    TollTransaction getTransaction(String transactionId);

    /**
     * Retrieves all recorded customer disputes.
     */
    List<Dispute> getAllDisputes();

    /**
     * Retrieves all recorded recharges.
     */
    List<RechargeRecord> getAllRecharges();

    /**
     * Retrieves all generated low balance alerts.
     */
    List<LowBalanceAlert> getAllAlerts();

    /**
     * Calculates total daily revenue collected for a designated plaza on a date.
     */
    double getDailyRevenue(int plazaId, LocalDate date);

    /**
     * Returns a breakdown of total vehicles processed per lane number for a plaza.
     */
    Map<Integer, Long> getVehicleCountPerLane(int plazaId);

    // ============================================================================================
    // SQL ANALYTICAL EXERCISES
    // ============================================================================================

    /**
     * SQL Exercise Query 1:
     * For each vehicle type, find total transactions, total toll collected, and average toll.
     * Show only vehicle types with > 2 successful transactions. Sort by total toll collected DESC.
     */
    List<VehicleTypeRevenueReport> executeQuery1VehicleTypeSummary();

    /**
     * SQL Exercise Query 2:
     * For each toll plaza, list total crossings, total revenue collected, and failed transactions.
     * Group by plaza and include only plazas with at least 3 crossings.
     */
    List<PlazaAuditReport> executeQuery2PlazaCrossingsAudit();

    /**
     * SQL Exercise Query 3:
     * For each FASTag account, find total crossings, total amount deducted, and distinct plazas used.
     * Include only accounts that crossed at more than 2 distinct plazas, ordered by total amount deducted DESC.
     */
    List<MultiPlazaAccountReport> executeQuery3MultiPlazaAccountSummary();
}
