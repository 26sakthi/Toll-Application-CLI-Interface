package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents a standard 2-axle commercial goods truck.
 */
public class Truck extends Vehicle {

    public Truck(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.TRUCK, ownerName, phoneNumber, false);
    }

    @Override
    public String getClassificationDetails() {
        return "Heavy Goods Vehicle - 2-Axle Commercial Freight Truck";
    }
}
