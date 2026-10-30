package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents a standard 2-axle heavy passenger bus.
 */
public class Bus extends Vehicle {

    public Bus(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.BUS, ownerName, phoneNumber, false);
    }

    @Override
    public String getClassificationDetails() {
        return "Heavy Passenger Motor Vehicle - 2-Axle Commercial Bus";
    }
}
