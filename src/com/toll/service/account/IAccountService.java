package com.toll.service.account;

import com.toll.model.account.FASTagAccount;
import com.toll.model.admin.Admin;
import com.toll.model.enums.RechargeMethod;
import com.toll.model.vehicle.Vehicle;

import java.util.List;

/**
 * Service contract for Vehicle and FASTag Account lifecycle (Module 2).
 * Demonstrates Abstraction and Encapsulation.
 */
public interface IAccountService {

    /**
     * Registers a new vehicle into the national vehicle registry.
     */
    Vehicle registerVehicle(Vehicle vehicle);

    /**
     * Issues and links a FASTag account to a registered vehicle.
     * Business Rules:
     * - Tag ID must be unique.
     * - Exactly one active tag linked per vehicle (strict 1:1 association).
     */
    FASTagAccount issueFASTag(String tagId, String vehicleRegNo, double initialBalance);

    /**
     * Recharges a FASTag account.
     * Business Rules:
     * - Min Rs. 100, Max Rs. 10,000 per recharge.
     * - Balance capped at Rs. 50,000.
     * - Automatically transitions from BLOCKED to ACTIVE if balance reaches >= Rs. 100.
     */
    FASTagAccount rechargeAccount(String tagId, double amount, RechargeMethod method);

    /**
     * Blacklists a tag (stolen / fraudulent).
     * Business Rule: Only an Admin can blacklist a tag.
     */
    void blacklistTag(Admin admin, String tagId, String reason);

    /**
     * Removes blacklist status from a tag.
     */
    void unblacklistTag(Admin admin, String tagId);

    /**
     * Closes a FASTag account permanently.
     */
    void closeAccount(Admin admin, String tagId);

    /**
     * Looks up an account by its unique Tag ID.
     */
    FASTagAccount getAccountByTagId(String tagId);

    /**
     * Looks up an account by the linked vehicle registration number.
     */
    FASTagAccount getAccountByVehicleReg(String vehicleRegNo);

    /**
     * Looks up a registered vehicle by its registration number.
     */
    Vehicle getVehicle(String registrationNumber);

    /**
     * Returns an unmodifiable list of all registered vehicles.
     */
    List<Vehicle> getAllVehicles();

    /**
     * Returns an unmodifiable list of all FASTag accounts.
     */
    List<FASTagAccount> getAllAccounts();
}
