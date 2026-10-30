package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents a Light Commercial Vehicle (LCV), van, or mini-bus.
 */
public class Van extends Vehicle {

    public Van(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.VAN, ownerName, phoneNumber, false);
    }

    public Van(String registrationNumber, String ownerName, String phoneNumber, boolean isEmergency) {
        super(registrationNumber, VehicleType.VAN, ownerName, phoneNumber, isEmergency);
    }

    @Override
    public String getClassificationDetails() {
        return "Light Commercial Vehicle (LCV) / Mini-Bus";
    }
}
