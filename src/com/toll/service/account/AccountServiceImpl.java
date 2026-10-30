package com.toll.service.account;

import com.toll.exception.AccountException;
import com.toll.exception.UnauthorizedException;
import com.toll.model.account.FASTagAccount;
import com.toll.model.admin.Admin;
import com.toll.model.enums.AccountStatus;
import com.toll.model.enums.RechargeMethod;
import com.toll.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of IAccountService managing Vehicles and FASTag Accounts.
 * Enforces 1:1 Tag Association, Balance Invariants, and Admin Authorizations.
 */
public class AccountServiceImpl implements IAccountService {

    private final Map<String, Vehicle> vehicleRegistry = new ConcurrentHashMap<>();
    private final Map<String, FASTagAccount> tagAccountRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> vehicleToTagMap = new ConcurrentHashMap<>();

    private void validateAdmin(Admin admin, String action) {
        if (admin == null || !admin.isActive()) {
            throw new UnauthorizedException("Unauthorized: Only an authorized Admin can " + action + ".");
        }
    }

    @Override
    public Vehicle registerVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null.");
        }
        String regNo = vehicle.getRegistrationNumber();
        if (vehicleRegistry.containsKey(regNo)) {
            throw new AccountException("Vehicle with registration " + regNo + " is already registered.");
        }
        vehicleRegistry.put(regNo, vehicle);
        return vehicle;
    }

    @Override
    public FASTagAccount issueFASTag(String tagId, String vehicleRegNo, double initialBalance) {
        if (tagId == null || tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be empty.");
        }
        if (vehicleRegNo == null || vehicleRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }

        String normalizedTagId = tagId.trim().toUpperCase();
        String normalizedRegNo = vehicleRegNo.trim().toUpperCase();

        if (!vehicleRegistry.containsKey(normalizedRegNo)) {
            throw new AccountException("Cannot issue FASTag: Vehicle " + normalizedRegNo + " is not registered in the system.");
        }
        if (tagAccountRegistry.containsKey(normalizedTagId)) {
            throw new AccountException("FASTag ID " + normalizedTagId + " is already issued.");
        }

        // Strict 1:1 rule: A vehicle can have only one active tag
        String existingTag = vehicleToTagMap.get(normalizedRegNo);
        if (existingTag != null) {
            FASTagAccount existingAccount = tagAccountRegistry.get(existingTag);
            if (existingAccount != null && existingAccount.getStatus() != AccountStatus.CLOSED) {
                throw new AccountException("Violation: Vehicle " + normalizedRegNo +
                        " already has an active FASTag (" + existingTag + "). Strict 1:1 rule violated.");
            }
        }

        FASTagAccount newAccount = new FASTagAccount(normalizedTagId, normalizedRegNo, initialBalance);
        tagAccountRegistry.put(normalizedTagId, newAccount);
        vehicleToTagMap.put(normalizedRegNo, normalizedTagId);
        return newAccount;
    }

    @Override
    public FASTagAccount rechargeAccount(String tagId, double amount, RechargeMethod method) {
        FASTagAccount account = getAccountByTagId(tagId);
        account.recharge(amount);
        return account;
    }

    @Override
    public void blacklistTag(Admin admin, String tagId, String reason) {
        validateAdmin(admin, "blacklist a FASTag account");
        FASTagAccount account = getAccountByTagId(tagId);
        account.blacklist(reason);
    }

    @Override
    public void unblacklistTag(Admin admin, String tagId) {
        validateAdmin(admin, "un-blacklist a FASTag account");
        FASTagAccount account = getAccountByTagId(tagId);
        account.unblacklist();
    }

    @Override
    public void closeAccount(Admin admin, String tagId) {
        validateAdmin(admin, "close a FASTag account");
        FASTagAccount account = getAccountByTagId(tagId);
        account.closeAccount();
    }

    @Override
    public FASTagAccount getAccountByTagId(String tagId) {
        if (tagId == null) {
            throw new IllegalArgumentException("Tag ID cannot be null.");
        }
        FASTagAccount account = tagAccountRegistry.get(tagId.trim().toUpperCase());
        if (account == null) {
            throw new AccountException("FASTag account not found for Tag ID: " + tagId);
        }
        return account;
    }

    @Override
    public FASTagAccount getAccountByVehicleReg(String vehicleRegNo) {
        if (vehicleRegNo == null) {
            throw new IllegalArgumentException("Vehicle registration cannot be null.");
        }
        String tagId = vehicleToTagMap.get(vehicleRegNo.trim().toUpperCase());
        if (tagId == null) {
            return null;
        }
        return tagAccountRegistry.get(tagId);
    }

    @Override
    public Vehicle getVehicle(String registrationNumber) {
        if (registrationNumber == null) {
            throw new IllegalArgumentException("Registration number cannot be null.");
        }
        Vehicle vehicle = vehicleRegistry.get(registrationNumber.trim().toUpperCase());
        if (vehicle == null) {
            throw new AccountException("Vehicle not found with registration: " + registrationNumber);
        }
        return vehicle;
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return Collections.unmodifiableList(new ArrayList<>(vehicleRegistry.values()));
    }

    @Override
    public List<FASTagAccount> getAllAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(tagAccountRegistry.values()));
    }
}
