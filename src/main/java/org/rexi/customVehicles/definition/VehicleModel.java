package org.rexi.customVehicles.definition;

import org.bukkit.util.Vector;

import java.nio.file.Path;

public record VehicleModel(
        Path modelFile,
        float scale,
        Vector offset,
        float yawOffset,
        float pitchOffset,
        float rollOffset
) {
}