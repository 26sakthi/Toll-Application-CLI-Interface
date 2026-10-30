package com.toll.model.vehicle;

import com.toll.model.enums.VehicleType;

/**
 * Represents a Multi-Axle Commercial Vehicle (3 to 6 axles, Heavy Construction Machinery, etc.).
 */
public class MultiAxleVehicle extends Vehicle {
    private final int numberOfAxles;

    public MultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber) {
        this(registrationNumber, ownerName, phoneNumber, 3);
    }

    public MultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber, int numberOfAxles) {
        super(registrationNumber, VehicleType.MULTI_AXLE, ownerName, phoneNumber, false);
        if (numberOfAxles < 3) {
            throw new IllegalArgumentException("Multi-axle vehicle must have at least 3 axles.");
        }
        this.numberOfAxles = numberOfAxles;
    }

    public int getNumberOfAxles() {
        return numberOfAxles;
    }

    @Override
    public String getClassificationDetails() {
        return "Multi-Axle Commercial Vehicle (" + numberOfAxles + " Axles, Over-Dimensional Cargo)";
    }
}
