package org.rexi.customVehicles.vehicle;

public enum VehicleType {

    COMPACT(2),
    SEDAN(4),
    VAN(8);

    private final int seats;

    VehicleType(int seats) {
        this.seats = seats;
    }

    public int getSeats() {
        return seats;
    }
}
