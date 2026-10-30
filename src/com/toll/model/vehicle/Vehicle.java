package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Abstract base class representing a registered vehicle on Indian Highways.
 * Demonstrates Object-Oriented Inheritance and Encapsulation.
 */
public abstract class Vehicle {
    private final String registrationNumber;
    private final VehicleType vehicleType;
    private String ownerName;
    private String phoneNumber;
    private final boolean emergencyVehicle;

    protected Vehicle(String registrationNumber, VehicleType vehicleType, String ownerName, String phoneNumber) {
        this(registrationNumber, vehicleType, ownerName, phoneNumber, false);
    }

    protected Vehicle(String registrationNumber, VehicleType vehicleType, String ownerName, String phoneNumber, boolean emergencyVehicle) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number cannot be empty.");
        }
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null.");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty.");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.vehicleType = vehicleType;
        this.ownerName = ownerName.trim();
        this.phoneNumber = phoneNumber.trim();
        this.emergencyVehicle = emergencyVehicle;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public synchronized String getOwnerName() {
        return ownerName;
    }

    public synchronized void setOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty.");
        }
        this.ownerName = ownerName.trim();
    }

    public synchronized String getPhoneNumber() {
        return phoneNumber;
    }

    public synchronized void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        this.phoneNumber = phoneNumber.trim();
    }

    public boolean isEmergencyVehicle() {
        return emergencyVehicle;
    }

    /**
     * Polymorphic method to return vehicle-specific classification details.
     */
    public abstract String getClassificationDetails();

    @Override
    public String toString() {
        return String.format("%s [Reg: %s | Type: %s | Owner: %s | Contact: %s%s]",
                getClass().getSimpleName(),
                registrationNumber,
                vehicleType,
                ownerName,
                phoneNumber,
                emergencyVehicle ? " | EMERGENCY" : "");
    }
}
