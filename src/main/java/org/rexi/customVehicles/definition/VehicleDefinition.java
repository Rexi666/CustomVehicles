package org.rexi.customVehicles.definition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record VehicleDefinition(
        String id,
        String displayName,
        VehicleCategory category,
        Path directory,
        VehiclePhysics physics,
        VehicleDimensions dimensions,
        VehicleModel model,
        SeatDefinition driverSeat,
        List<SeatDefinition> passengerSeats,
        Map<String, Path> textures
) {

    public int getTotalSeats() {
        return 1 + passengerSeats.size();
    }
}