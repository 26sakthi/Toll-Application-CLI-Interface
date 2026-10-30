package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents an authorized Emergency Vehicle (e.g. Ambulance, Fire Engine).
 * Toll exempt under MoRTH / NHAI guidelines.
 */
public class EmergencyVehicle extends Vehicle {
    private final String emergencyServiceType; // e.g. "Ambulance", "Fire Engine", "Disaster Response"

    public EmergencyVehicle(String registrationNumber, String ownerName, String phoneNumber, String emergencyServiceType) {
        super(registrationNumber, VehicleType.VAN, ownerName, phoneNumber, true);
        this.emergencyServiceType = emergencyServiceType != null ? emergencyServiceType : "Emergency Service";
    }

    public String getEmergencyServiceType() {
        return emergencyServiceType;
    }

    @Override
    public String getClassificationDetails() {
        return "Priority Emergency Service Vehicle [" + emergencyServiceType + "] - 100% Toll Exempt";
    }
}
