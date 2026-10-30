package com.toll.service.billing;

import com.toll.exception.DisputeException;
import com.toll.exception.UnauthorizedException;
import com.toll.model.account.FASTagAccount;
import com.toll.model.admin.Admin;
import com.toll.model.billing.*;
import com.toll.model.dispute.Dispute;
import com.toll.model.enums.*;
import com.toll.model.transaction.TollTransaction;
import com.toll.service.account.IAccountService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Concrete implementation of IBillingReportingService.
 * Manages Payment Audits, 7-Day Disputes, Low Balance Notifications (< Rs. 200),
 * and In-Memory Execution of SQL Analytical Queries 1, 2, and 3.
 */
public class BillingReportingServiceImpl implements IBillingReportingService {

    private final IAccountService accountService;
    private final List<TollTransaction> transactionStore = new CopyOnWriteArrayList<>();
    private final List<RechargeRecord> rechargeStore = new CopyOnWriteArrayList<>();
    private final Map<String, Dispute> disputeStore = new ConcurrentHashMap<>();
    private final List<LowBalanceAlert> alertStore = new CopyOnWriteArrayList<>();

    public BillingReportingServiceImpl(IAccountService accountService) {
        this.accountService = accountService;
    }

    private void validateAdmin(Admin admin, String action) {
        if (admin == null || !admin.isActive()) {
            throw new UnauthorizedException("Unauthorized: Only an authorized Admin can " + action + ".");
        }
    }

    @Override
    public RechargeRecord recordRecharge(String tagId, double amount, RechargeMethod method, RechargeStatus status) {
        RechargeRecord record = new RechargeRecord(tagId, amount, method, status);
        rechargeStore.add(record);
        return record;
    }

    @Override
    public Dispute raiseDispute(String vehicleRegNo, String transactionId, String reason, LocalDateTime disputeTime) {
        if (disputeTime == null) {
            disputeTime = LocalDateTime.now();
        }

        TollTransaction txn = getTransaction(transactionId);
        if (!txn.getVehicleRegNo().equalsIgnoreCase(vehicleRegNo.trim())) {
            throw new DisputeException("Dispute Mismatch: Transaction " + transactionId +
                    " does not belong to vehicle " + vehicleRegNo);
        }

        // Business Rule: A vehicle owner can dispute a wrong deduction within 7 days.
        long daysElapsed = ChronoUnit.DAYS.between(txn.getTimestamp(), disputeTime);
        if (daysElapsed > 7) {
            throw new DisputeException(String.format(
                    "Dispute Rejected: Dispute was raised %d days after crossing. Window is strictly within 7 days.",
                    daysElapsed));
        }

        // Prevent duplicate disputes on the same transaction
        for (Dispute existing : disputeStore.values()) {
            if (existing.getTransactionId().equalsIgnoreCase(transactionId)) {
                throw new DisputeException("A dispute has already been filed for Transaction: " + transactionId);
            }
        }

        Dispute dispute = new Dispute(transactionId, vehicleRegNo, txn.getTagId(), txn.getTollAmount(), reason, disputeTime);
        disputeStore.put(dispute.getDisputeId(), dispute);
        return dispute;
    }

    @Override
    public void reviewDispute(Admin admin, String disputeId, boolean approve, String remarks) {
        validateAdmin(admin, "review and resolve disputes");

        Dispute dispute = disputeStore.get(disputeId);
        if (dispute == null) {
            throw new DisputeException("Dispute not found: " + disputeId);
        }
        if (dispute.getStatus() != DisputeStatus.RAISED) {
            throw new DisputeException("Dispute " + disputeId + " is already resolved (" + dispute.getStatus() + ").");
        }

        if (approve) {
            dispute.resolve(DisputeStatus.APPROVED, remarks);

            // Business Rule: If Admin approves, the amount is refunded to the tag balance.
            if (dispute.getTagId() != null && accountService != null) {
                FASTagAccount account = accountService.getAccountByTagId(dispute.getTagId());
                if (account != null) {
                    account.refund(dispute.getDisputedAmount());
                }
            }
        } else {
            dispute.resolve(DisputeStatus.REJECTED, remarks);
        }
    }

    @Override
    public LowBalanceAlert checkAndTriggerLowBalanceAlert(String tagId, String vehicleRegNo, double balanceAfterDeduction) {
        // Business Rule: When balance falls below Rs. 200 after a deduction, a low balance alert is generated.
        if (balanceAfterDeduction < 200.0) {
            LowBalanceAlert alert = new LowBalanceAlert(tagId, vehicleRegNo, balanceAfterDeduction);
            alertStore.add(alert);
            return alert;
        }
        return null;
    }

    @Override
    public void recordTransaction(TollTransaction transaction) {
        if (transaction != null) {
            transactionStore.add(transaction);
        }
    }

    @Override
    public List<TollTransaction> getAllTransactions() {
        return Collections.unmodifiableList(transactionStore);
    }

    @Override
    public TollTransaction getTransaction(String transactionId) {
        return transactionStore.stream()
                .filter(t -> t.getTransactionId().equalsIgnoreCase(transactionId.trim()))
                .findFirst()
                .orElseThrow(() -> new DisputeException("Transaction not found for ID: " + transactionId));
    }

    @Override
    public List<Dispute> getAllDisputes() {
        return Collections.unmodifiableList(new ArrayList<>(disputeStore.values()));
    }

    @Override
    public List<RechargeRecord> getAllRecharges() {
        return Collections.unmodifiableList(rechargeStore);
    }

    @Override
    public List<LowBalanceAlert> getAllAlerts() {
        return Collections.unmodifiableList(alertStore);
    }

    @Override
    public double getDailyRevenue(int plazaId, LocalDate date) {
        return transactionStore.stream()
                .filter(t -> t.getPlazaId() == plazaId)
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getTimestamp().toLocalDate().equals(date))
                .mapToDouble(TollTransaction::getTollAmount)
                .sum();
    }

    @Override
    public Map<Integer, Long> getVehicleCountPerLane(int plazaId) {
        return transactionStore.stream()
                .filter(t -> t.getPlazaId() == plazaId)
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(TollTransaction::getLaneNumber, Collectors.counting()));
    }

    // ============================================================================================
    // SQL ANALYTICAL EXERCISES
    // ============================================================================================

    /**
     * Query 1:
     * For each vehicle type, find total number of toll transactions, total toll collected, and
     * average toll per crossing. Show only vehicle types that have more than 2 successful transactions.
     * Sort by total toll collected in descending order.
     */
    @Override
    public List<VehicleTypeRevenueReport> executeQuery1VehicleTypeSummary() {
        Map<VehicleType, List<TollTransaction>> grouped = transactionStore.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(TollTransaction::getBilledType));

        List<VehicleTypeRevenueReport> reports = new ArrayList<>();
        for (Map.Entry<VehicleType, List<TollTransaction>> entry : grouped.entrySet()) {
            List<TollTransaction> txns = entry.getValue();
            if (txns.size() > 2) { // HAVING COUNT(*) > 2
                long totalCount = txns.size();
                double totalToll = txns.stream().mapToDouble(TollTransaction::getTollAmount).sum();
                double avgToll = totalCount > 0 ? (totalToll / totalCount) : 0.0;
                reports.add(new VehicleTypeRevenueReport(entry.getKey(), totalCount, totalToll, avgToll));
            }
        }

        // ORDER BY total toll collected DESC
        reports.sort(Comparator.comparingDouble(VehicleTypeRevenueReport::getTotalTollCollected).reversed());
        return reports;
    }

    /**
     * Query 2:
     * For each toll plaza, list total crossings, total revenue collected, and failed transactions.
     * Group by plaza and include only plazas with at least 3 crossings.
     */
    @Override
    public List<PlazaAuditReport> executeQuery2PlazaCrossingsAudit() {
        Map<Integer, List<TollTransaction>> groupedByPlaza = transactionStore.stream()
                .collect(Collectors.groupingBy(TollTransaction::getPlazaId));

        List<PlazaAuditReport> reports = new ArrayList<>();
        for (Map.Entry<Integer, List<TollTransaction>> entry : groupedByPlaza.entrySet()) {
            List<TollTransaction> txns = entry.getValue();
            if (txns.size() >= 3) { // HAVING COUNT(*) >= 3
                int plazaId = entry.getKey();
                String plazaName = txns.get(0).getPlazaName();
                long totalCrossings = txns.size();
                double totalRevenue = txns.stream()
                        .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                        .mapToDouble(TollTransaction::getTollAmount)
                        .sum();
                long failedTxns = txns.stream()
                        .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                        .count();

                reports.add(new PlazaAuditReport(plazaId, plazaName, totalCrossings, totalRevenue, failedTxns));
            }
        }

        reports.sort(Comparator.comparingInt(PlazaAuditReport::getPlazaId));
        return reports;
    }

    /**
     * Query 3:
     * For each FASTag account, find total crossings, total amount deducted, and distinct plazas used.
     * Include only accounts that crossed at more than 2 distinct plazas, ordered by total amount deducted DESC.
     */
    @Override
    public List<MultiPlazaAccountReport> executeQuery3MultiPlazaAccountSummary() {
        Map<String, List<TollTransaction>> groupedByTag = transactionStore.stream()
                .filter(t -> t.getTagId() != null && !t.getTagId().trim().isEmpty())
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(TollTransaction::getTagId));

        List<MultiPlazaAccountReport> reports = new ArrayList<>();
        for (Map.Entry<String, List<TollTransaction>> entry : groupedByTag.entrySet()) {
            List<TollTransaction> txns = entry.getValue();
            long distinctPlazas = txns.stream().map(TollTransaction::getPlazaId).distinct().count();

            if (distinctPlazas > 2) { // HAVING COUNT(DISTINCT plaza_id) > 2
                String tagId = entry.getKey();
                String vehicleRegNo = txns.get(0).getVehicleRegNo();
                long totalCrossings = txns.size();
                double totalDeducted = txns.stream().mapToDouble(TollTransaction::getTollAmount).sum();

                reports.add(new MultiPlazaAccountReport(tagId, vehicleRegNo, totalCrossings, totalDeducted, distinctPlazas));
            }
        }

        // ORDER BY total amount deducted DESC
        reports.sort(Comparator.comparingDouble(MultiPlazaAccountReport::getTotalAmountDeducted).reversed());
        return reports;
    }
}
