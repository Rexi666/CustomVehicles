package org.rexi.customVehicles.definition;

public record VehiclePhysics(
        double maximumForwardSpeed,
        double maximumReverseSpeed,
        double acceleration,
        double reverseAcceleration,
        double friction,
        float handling,
        double minimumTurnSpeed
) {
}