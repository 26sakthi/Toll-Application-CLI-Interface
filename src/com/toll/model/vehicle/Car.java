package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents a passenger car, jeep, or sedan.
 */
public class Car extends Vehicle {

    public Car(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.CAR, ownerName, phoneNumber, false);
    }

    public Car(String registrationNumber, String ownerName, String phoneNumber, boolean isEmergency) {
        super(registrationNumber, VehicleType.CAR, ownerName, phoneNumber, isEmergency);
    }

    @Override
    public String getClassificationDetails() {
        return "Passenger Car / Jeep / Sedan (Class 4 Light Motor Vehicle)";
    }
}
